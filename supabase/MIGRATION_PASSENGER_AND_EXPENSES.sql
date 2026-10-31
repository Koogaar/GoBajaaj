-- ==========================================================
-- GoBajaaj: PASSENGER DIGITAL ADDRESS & DRIVER EXPENSES MIGRATION
-- Adds:
-- 1) saved_addresses: Guri Number Digital ah (e.g. MG-HODAN-125),
--    Tilmaanta Soomaalida, iyo GPS Coordinates.
-- 2) driver_expenses: Dakhliga & Kharashaadka Darawalka (Maalinle,
--    Isbuucle, Bille, Sanadle).
-- 3) get_nearby_drivers_count: Tirada bajaajyada diyaar ah ee 1 KM u jira.
-- 4) RLS updates si rakaabku u arkaan bajaajyada online-ka ah.
-- ==========================================================

-- ----------------------------------------------------------
-- 1) SAVED ADDRESSES (GURI NUMBER DIGITAL AH)
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
-- 2) DRIVER EXPENSES & INCOME (BUUGGA DHAQAALAHA)
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
-- 3) GET NEARBY ONLINE DRIVERS COUNT (1 KM RADAR)
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
      AND public.haversine_m(user_lat, user_lng, dp.latitude, dp.longitude) <= radius_m
      AND NOT EXISTS (
          SELECT 1 FROM public.bajaj_rides r
          WHERE r.driver_id = dp.driver_id
            AND r.status IN ('searching','assigned','accepted','arrived','in_progress')
      );
$$;

GRANT EXECUTE ON FUNCTION public.get_nearby_drivers_count(DOUBLE PRECISION, DOUBLE PRECISION, INTEGER) TO authenticated, anon;

-- Also allow passengers to view active driver presence positions
DROP POLICY IF EXISTS "Anyone see online driver presence" ON public.driver_presence;
CREATE POLICY "Anyone see online driver presence" ON public.driver_presence
    FOR SELECT USING (is_online = true);

