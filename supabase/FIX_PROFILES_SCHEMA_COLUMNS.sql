-- =====================================================================
-- SHIRI APP - FIX PROFILES TABLE MISSING COLUMNS
-- Khaladka: "Could not find the 'business_name' column of 'profiles'"
-- 
-- Ku dhufo 'RUN' gudaha Supabase SQL Editor:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- =====================================================================

-- 1. KU DAR DHAMMAAN COLUMN-YADA MAQAN EE PROFILES TABLE-KA
--    (ADD COLUMN IF NOT EXISTS = ma toosin hadii horay u jirtey)

-- Column-yada merchant/business owners u baahan
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS business_name TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS business_type TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS merchant_payment_number TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS district TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS verification_status TEXT DEFAULT 'approved';
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS monthly_fee NUMERIC(10,2) DEFAULT NULL;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS referred_by TEXT;

-- Column-yada driver/bajaajle u baahan
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS plate_number TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS bajaaj_model TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS vehicle_model TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS license_number TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS guarantor_name TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS guarantor_phone TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS cid_letter_number TEXT;

-- Column-yada guud
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'pending';
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMPTZ;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'active';
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS push_token TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT false;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS last_seen TIMESTAMPTZ;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS rating NUMERIC(3,2) DEFAULT 5.0;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS total_rides INT DEFAULT 0;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS city TEXT DEFAULT 'Muqdisho';
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- 2. DAMI RLS DHAMMAAN MIISASHA MUHIIMKA AH
ALTER TABLE IF EXISTS public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.subscriptions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.payment_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.bajaj_rides DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.admin_users DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.audit_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.notifications DISABLE ROW LEVEL SECURITY;

-- 3. FASAX BUUXA SII DOORKA ANON IYO AUTHENTICATED
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated, service_role;

-- 4. RELOAD SCHEMA CACHE - Supabase PostgREST uu schema cusub aqriyo
NOTIFY pgrst, 'reload schema';

-- 5. HUBI INAY COLUMN-YADU JIRAAN
SELECT 
    column_name,
    data_type,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public' 
  AND table_name = 'profiles'
ORDER BY ordinal_position;

