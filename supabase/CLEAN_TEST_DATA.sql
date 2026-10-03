-- =============================================================================
-- GOBAJAAJ / SOMALI TRAFFIC - CLEAN ALL TEST DATA (WIPE TEST RECORDS)
-- Ku dhufo 'RUN' gudaha Supabase SQL Editor si aad xogta tijaabada ah u tirtirto:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- =============================================================================

BEGIN;

-- 1. TRUNCATE TEST TRANSACTIONS & SUBSCRIPTIONS
TRUNCATE TABLE public.payment_transactions CASCADE;
TRUNCATE TABLE public.subscriptions CASCADE;

-- 2. TRUNCATE TEST TRAFFIC REPORTS & INCIDENTS
TRUNCATE TABLE public.traffic_reports CASCADE;
TRUNCATE TABLE public.traffic_incidents CASCADE;

-- 3. TRUNCATE TEST BAJAJ RIDES
TRUNCATE TABLE public.bajaj_rides CASCADE;

-- 4. TRUNCATE TEST SUPPORT TICKETS & MESSAGES
TRUNCATE TABLE public.support_messages CASCADE;
TRUNCATE TABLE public.support_tickets CASCADE;

-- 5. TRUNCATE TEST DEVICES & LOCATIONS (PHONE GUARD)
TRUNCATE TABLE public.device_security_events CASCADE;
TRUNCATE TABLE public.device_locations CASCADE;
TRUNCATE TABLE public.devices CASCADE;
TRUNCATE TABLE public.trusted_contacts CASCADE;

-- 6. TRUNCATE AUDIT LOGS
TRUNCATE TABLE public.audit_logs CASCADE;

-- 7. UNASSIGN REPORTERS ON INTERSECTIONS BEFORE PROFILES DELETE (PREVENTS FK CONFLICT)
UPDATE public.intersections
SET assigned_reporter_id = NULL,
    current_status = 'Furan',
    congestion_level = 0,
    average_speed = 30.0,
    last_updated = NOW();

-- 8. DELETE TEST USER PROFILES (PRESERVES SUPER ADMIN ACCOUNTS ONLY)
DELETE FROM public.profiles 
WHERE role NOT IN ('super_admin', 'admin') 
  AND id NOT IN ('5ae9788f-c6fa-4164-a846-5db62741b092', '4c49d2a4-0ec8-4add-9161-5e421559fca1');

COMMIT;

SELECT 'GUUL! Dhammaan xogta tijaabada ah waa la tirtiray. Dashboard-ku waa 0 USD oo nadiif ah!' as status;
