-- ==========================================================
-- GoBajaaj: PASSENGER DIGITAL ADDRESS & DRIVER EXPENSES MIGRATION
-- Fully self-contained & safe to re-run (idempotent).
-- ==========================================================

-- ----------------------------------------------------------
-- 0) PREREQUISITES & HELPER FUNCTIONS
-- ----------------------------------------------------------

-- Helper: Haversine distance in meters (pure SQL, no extensions required)
CREATE OR REPLACE FUNCTION public.haversine_m(
    lat1 DOUBLE PRECISION, lon1 DOUBLE PRECISION,
    lat2 DOUBLE PRECISION, lon2 DOUBLE PRECISION
) RETURNS DOUBLE PRECISION
LANGUAGE sql IMMUTABLE AS $$
    SELECT 6371000.0 * 2 * asin(sqrt(
        least(1.0,
          pow(sin(radians(lat2 - lat1) / 2), 2) +
          cos(radians(lat1)) * cos(radians(lat2)) *
          pow(sin(radians(lon2 - lon1) / 2), 2)
        )
    ));
$$;

-- Helper: Check if current user is admin
CREATE OR REPLACE FUNCTION public.is_admin_user()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles p
        WHERE p.id = auth.uid()
          AND p.role IN ('super_admin','payment_admin','traffic_admin',
                         'reporter_manager','support_agent','analyst',
                         'user_manager','subscription_manager')
    );
$$;

-- FIX PROFILES ROLE CHECK (ALLOW PASSENGER, DRIVER, ADMINS, ETC.)
ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_role_check;
ALTER TABLE IF EXISTS public.profiles ADD CONSTRAINT profiles_role_check 
  CHECK (role IN ('passenger', 'driver', 'reporter', 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin', 'user'));

ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_status_check;
ALTER TABLE IF EXISTS public.profiles ADD CONSTRAINT profiles_status_check 
  CHECK (status IN ('active', 'suspended', 'disabled', 'pending', 'rejected'));

-- FIX SUBSCRIPTIONS & PAYMENT TRANSACTIONS STATUS CHECKS
ALTER TABLE IF EXISTS public.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE IF EXISTS public.subscriptions ADD CONSTRAINT subscriptions_status_check 
  CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed', 'rejected', 'completed', 'approved'));

ALTER TABLE IF EXISTS public.payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_check;
ALTER TABLE IF EXISTS public.payment_transactions ADD CONSTRAINT payment_transactions_status_check 
  CHECK (status IN ('completed', 'pending', 'failed', 'rejected', 'approved'));

-- RLS POLICIES FOR PROFILES, SUBSCRIPTIONS & PAYMENTS
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read profiles" ON public.profiles;
CREATE POLICY "Public read profiles" ON public.profiles FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users insert own profile" ON public.profiles;
CREATE POLICY "Users insert own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id OR auth.role() = 'anon' OR auth.role() = 'authenticated');

DROP POLICY IF EXISTS "Users and admins update profile" ON public.profiles;
CREATE POLICY "Users and admins update profile" ON public.profiles FOR UPDATE USING (auth.uid() = id OR auth.role() = 'authenticated' OR auth.role() = 'anon');

DROP POLICY IF EXISTS "Public read subscriptions" ON public.subscriptions;
CREATE POLICY "Public read subscriptions" ON public.subscriptions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Drivers insert subscriptions" ON public.subscriptions;
CREATE POLICY "Drivers insert subscriptions" ON public.subscriptions FOR INSERT WITH CHECK (auth.uid() = user_id OR auth.role() = 'anon' OR auth.role() = 'authenticated');

DROP POLICY IF EXISTS "Admins update subscriptions" ON public.subscriptions;
CREATE POLICY "Admins update subscriptions" ON public.subscriptions FOR UPDATE USING (auth.uid() = user_id OR auth.role() = 'authenticated' OR auth.role() = 'anon');

DROP POLICY IF EXISTS "Public read payment_transactions" ON public.payment_transactions;
CREATE POLICY "Public read payment_transactions" ON public.payment_transactions FOR SELECT USING (true);

DROP POLICY IF EXISTS "Drivers insert payment_transactions" ON public.payment_transactions;
CREATE POLICY "Drivers insert payment_transactions" ON public.payment_transactions FOR INSERT WITH CHECK (auth.uid() = user_id OR auth.role() = 'anon' OR auth.role() = 'authenticated');

DROP POLICY IF EXISTS "Admins update payment_transactions" ON public.payment_transactions;
CREATE POLICY "Admins update payment_transactions" ON public.payment_transactions FOR UPDATE USING (auth.uid() = user_id OR auth.role() = 'authenticated' OR auth.role() = 'anon');

-- ----------------------------------------------------------
-- 1) DRIVER PRESENCE TABLE (REQUIRED FOR 1 KM RADAR & TRACKING)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.driver_presence (
    driver_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    is_online BOOLEAN NOT NULL DEFAULT false,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    speed DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.driver_presence ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Drivers manage own presence" ON public.driver_presence;
CREATE POLICY "Drivers manage own presence" ON public.driver_presence
    FOR ALL USING (auth.uid() = driver_id) WITH CHECK (auth.uid() = driver_id);

DROP POLICY IF EXISTS "Admins read presence" ON public.driver_presence;
CREATE POLICY "Admins read presence" ON public.driver_presence
    FOR SELECT USING (public.is_admin_user());

DROP POLICY IF EXISTS "Anyone see online driver presence" ON public.driver_presence;
CREATE POLICY "Anyone see online driver presence" ON public.driver_presence
    FOR SELECT USING (is_online = true);

-- ----------------------------------------------------------
-- 2) SAVED ADDRESSES (GURI NUMBER DIGITAL AH)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.saved_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    digital_code TEXT NOT NULL,
    address_label TEXT NOT NULL DEFAULT 'Gurigeyga',
    district TEXT NOT NULL DEFAULT 'Hodan',
    somali_directions TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saved_addresses_user ON public.saved_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_addresses_code ON public.saved_addresses(digital_code);

ALTER TABLE public.saved_addresses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users manage own saved addresses" ON public.saved_addresses;
CREATE POLICY "Users manage own saved addresses" ON public.saved_addresses
    FOR ALL USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Anyone read by digital code" ON public.saved_addresses;
CREATE POLICY "Anyone read by digital code" ON public.saved_addresses
    FOR SELECT USING (true);

-- ----------------------------------------------------------
-- 3) DRIVER EXPENSES & INCOME (BUUGGA DHAQAALAHA)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.driver_expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    type TEXT NOT NULL CHECK (type IN ('income', 'expense')),
    category TEXT NOT NULL, -- 'shidaal', 'pass', 'dayactir', 'kiro', 'cunto', 'safar_gobajaaj', 'safar_caadi'
    amount DOUBLE PRECISION NOT NULL CHECK (amount >= 0),
    notes TEXT,
    entry_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_driver_expenses_driver_date ON public.driver_expenses(driver_id, entry_date);

ALTER TABLE public.driver_expenses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Drivers manage own expenses" ON public.driver_expenses;
CREATE POLICY "Drivers manage own expenses" ON public.driver_expenses
    FOR ALL USING (auth.uid() = driver_id) WITH CHECK (auth.uid() = driver_id);

DROP POLICY IF EXISTS "Admins read all expenses" ON public.driver_expenses;
CREATE POLICY "Admins read all expenses" ON public.driver_expenses
    FOR SELECT USING (public.is_admin_user());

-- ----------------------------------------------------------
-- 4) GET NEARBY ONLINE DRIVERS COUNT (1 KM RADAR)
-- ----------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_nearby_drivers_count(
    user_lat DOUBLE PRECISION,
    user_lng DOUBLE PRECISION,
    radius_m INTEGER DEFAULT 1000
)
RETURNS INTEGER
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
    SELECT count(*)::integer
    FROM public.driver_presence dp
    JOIN public.profiles p ON p.id = dp.driver_id
    WHERE dp.is_online = true
      AND dp.updated_at > NOW() - INTERVAL '180 seconds'
      AND dp.latitude IS NOT NULL
      AND dp.longitude IS NOT NULL
      AND p.role = 'driver'
      AND p.status = 'active'
      AND public.haversine_m(user_lat, user_lng, dp.latitude, dp.longitude) <= radius_m;
$$;

GRANT EXECUTE ON FUNCTION public.get_nearby_drivers_count(DOUBLE PRECISION, DOUBLE PRECISION, INTEGER) TO authenticated, anon;

-- ----------------------------------------------------------
-- 5) RESET PASSWORD BY PHONE OR GMAIL (SECURITY DEFINER)
-- ----------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

CREATE OR REPLACE FUNCTION public.reset_user_password_by_phone(
    p_phone text,
    p_full_name text,
    p_new_password text
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, extensions
AS $$
DECLARE
    v_clean_phone text;
    v_e164 text;
    v_user_id uuid;
    v_profile_name text;
    v_encrypted_pw text;
BEGIN
    -- 1. Check if input is email or phone
    IF p_phone LIKE '%@%' THEN
        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE lower(phone_number) = lower(trim(p_phone))
        LIMIT 1;

        IF v_user_id IS NULL THEN
            SELECT id, email INTO v_user_id, v_profile_name
            FROM auth.users
            WHERE lower(email) = lower(trim(p_phone))
            LIMIT 1;
        END IF;
    ELSE
        v_clean_phone := regexp_replace(p_phone, '[^0-9]', '', 'g');
        IF length(v_clean_phone) >= 7 THEN
            IF length(v_clean_phone) >= 9 THEN
                v_e164 := '+252' || right(v_clean_phone, 9);
            ELSE
                v_e164 := '+252' || v_clean_phone;
            END IF;
        ELSE
            RETURN jsonb_build_object('success', false, 'message', 'Fadlan geli nambar taleefan ama Gmail sax ah.');
        END IF;

        SELECT id, full_name INTO v_user_id, v_profile_name
        FROM public.profiles
        WHERE phone_number = v_e164
           OR phone_number = right(v_clean_phone, 9)
           OR phone_number = '0' || right(v_clean_phone, 9)
           OR phone_number = '252' || right(v_clean_phone, 9)
           OR phone_number = v_clean_phone
           OR phone_number = p_phone
        LIMIT 1;
    END IF;

    -- 2. Validate password length
    IF length(p_new_password) < 4 THEN
        RETURN jsonb_build_object('success', false, 'message', 'Furaha sirta waa inuu ugu yaraan 4 xaraf yahay.');
    END IF;

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object('success', false, 'message', 'Ma jiro akoon ku diiwaangashan: ' || p_phone);
    END IF;

    -- 3. Hash password using bcrypt and update auth.users
    v_encrypted_pw := extensions.crypt(p_new_password, extensions.gen_salt('bf'));

    UPDATE auth.users
    SET encrypted_password = v_encrypted_pw,
        updated_at = now()
    WHERE id = v_user_id;

    RETURN jsonb_build_object(
        'success', true, 
        'message', 'Furaha sirta si guul leh ayaa loo beddelay! Hadda waad gali kartaa.'
    );
END;
$$;

REVOKE ALL ON FUNCTION public.reset_user_password_by_phone(text, text, text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.reset_user_password_by_phone(text, text, text) TO anon, authenticated;
