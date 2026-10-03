-- ==========================================================
-- SOMALI TRAFFIC - SUPABASE PERMISSIONS & RLS FIX
-- Oggolow in App-ka Android iyo Web Admin-ka ay toos u wadaagaan xogta
-- ==========================================================

-- 1. KA SAAR XAYIRAADDA AUTH.USERS (Si darawallada App-ka iska diiwaangeliya ay toos ugu keydsamaan profiles)
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;

-- 2. DAREEMIYAHA RLS KA DAMI MIISASHA MUHIIMKA AH
-- Tani waxay keenaysaa in lacagaha, warbixinnada, iyo macaamiisha ay toos ugu soo baxaan Web Admin-ka
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.intersections DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.traffic_reports DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.traffic_incidents DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_tickets DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_messages DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications DISABLE ROW LEVEL SECURITY;

-- 3. SII FASAX BUUXA ANON IYO AUTHENTICATED ROLES
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated;
