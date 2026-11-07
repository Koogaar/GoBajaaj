-- =============================================================================
-- SHIRI: TIR-TIRIDDA ISGOYSYADA DEMO-DA & TIJAABOOYINKA (DATABASE CLEANUP)
-- Ku dhufo 'RUN' gudaha Supabase SQL Editor:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. TIR-TIR ISGOYSYADII DEMO-DA AHAA (10-kii isgoys ee hore loogu shubay)
-- -----------------------------------------------------------------------------
-- Waxay tirtiraysaa dhammaan isgoysyadii demo-da ahaa iyo warbixinada ku xirnaa
DELETE FROM public.traffic_reports;

DELETE FROM public.intersections 
WHERE id IN (
    '11111111-1111-4111-a111-111111111111',
    '22222222-2222-4222-a222-222222222222',
    '33333333-3333-4333-a333-333333333333',
    '44444444-4444-4444-a444-444444444444',
    '55555555-5555-4555-a555-555555555555',
    '66666666-6666-4666-a666-666666666666',
    '77777777-7777-4777-a777-777777777777',
    '88888888-8888-4888-a888-888888888888',
    '99999999-9999-4999-a999-999999999999',
    'aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa'
);

-- Hadda waxaad Isgoysyada dhabta ah ku dari kartaa Web Admin Dashboard-kaaga!

-- -----------------------------------------------------------------------------
-- 2. NADIIFI TIJAABOOYINKA DALABAADKA & LACAG-BIXINTA (Ikhtiyaari - haddii aad rabto inaad 0 ka bilowdo)
-- -----------------------------------------------------------------------------
-- TRUNCATE TABLE public.bajaj_rides CASCADE;
-- TRUNCATE TABLE public.payment_transactions CASCADE;
-- TRUNCATE TABLE public.subscriptions CASCADE;

COMMIT;

SELECT 'GUUL! Dhammaan isgoysyadii demo-da ahaa waa la tirtiray database-ka. Hadda waxaad ka abuuran kartaa Web Admin.' as natiijo;

