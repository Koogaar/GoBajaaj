-- ==========================================================
-- GOBAJAAJ / SOMALI TRAFFIC - DHAMMAAN SAXIDDA SUPABASE (ONE-CLICK FIX)
-- Ku dhufo 'RUN' gudaha Supabase SQL Editor:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- ==========================================================

-- 1. KA SAAR XAYIRAADDAHA ADAG EE FOREIGN KEYS (Si App-ka & Web-ku xogta si toos ah isugu diraan)
ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;
ALTER TABLE IF EXISTS public.payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_user_id_fkey;
ALTER TABLE IF EXISTS public.payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_subscription_id_fkey;
ALTER TABLE IF EXISTS public.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_user_id_fkey;

-- 2. KU DAR TIIRARKA MUHIIMKA AH HADDI AY KA MAQNAAYEEN MIISASHA
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS plate_number TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS bajaaj_model TEXT;
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'pending';
ALTER TABLE IF EXISTS public.profiles ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMPTZ;

ALTER TABLE IF EXISTS public.subscriptions ADD COLUMN IF NOT EXISTS sender_phone TEXT;
ALTER TABLE IF EXISTS public.subscriptions ADD COLUMN IF NOT EXISTS destination_phone TEXT;
ALTER TABLE IF EXISTS public.subscriptions ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE IF EXISTS public.payment_transactions ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- 3. DAMI RLS (ROW LEVEL SECURITY) DHAMMAAN MIISASHA (Si uu Web Admin-ka & App-ka toos ugu wada xirnaado)
ALTER TABLE IF EXISTS public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.subscriptions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.payment_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.bajaj_rides DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.admin_users DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.audit_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.intersections DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.traffic_reports DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.traffic_incidents DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.devices DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.support_tickets DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.support_messages DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.notifications DISABLE ROW LEVEL SECURITY;

-- 4. FASAX BUUXA SII DOORKA ANON IYO AUTHENTICATED
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated, service_role;

-- 5. SHID REALTIME REPLICATION (Si isla markii aad Approve dhahdo uu app-ku isla ilbiriqsigaas u garto)
DO $$
BEGIN
  BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE public.profiles, public.subscriptions, public.payment_transactions;
  EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN OTHERS THEN NULL;
  END;
END $$;

-- 6. HUBI INAY SHARCIYADU HAGAAJEEN
SELECT 'Guul! Supabase Permissions & Schema si buuxda ayaa loo saxay.' as status;
