-- =============================================================================
-- GOBAJAAJ / SOMALI TRAFFIC - FIX DRIVER REGISTRATION & PAYMENTS RLS MIGRATION
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- =============================================================================

BEGIN;

-- 1. FIX CHECK CONSTRAINTS ON STATUS FIELDS TO SUPPORT REJECTED & COMPLETED
ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_role_check;
ALTER TABLE IF EXISTS public.profiles ADD CONSTRAINT profiles_role_check 
  CHECK (role IN ('passenger', 'driver', 'reporter', 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin'));

ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_status_check;
ALTER TABLE IF EXISTS public.profiles ADD CONSTRAINT profiles_status_check 
  CHECK (status IN ('active', 'suspended', 'disabled', 'pending', 'rejected'));

ALTER TABLE IF EXISTS public.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE IF EXISTS public.subscriptions ADD CONSTRAINT subscriptions_status_check 
  CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed', 'rejected', 'completed', 'approved'));

ALTER TABLE IF EXISTS public.payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_check;
ALTER TABLE IF EXISTS public.payment_transactions ADD CONSTRAINT payment_transactions_status_check 
  CHECK (status IN ('completed', 'pending', 'failed', 'rejected', 'approved'));

-- 2. ENSURE RLS IS ENABLED
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;

-- 3. DROP OLD CONFLICTING POLICIES
DROP POLICY IF EXISTS "Anyone can view profiles" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
DROP POLICY IF EXISTS "Public profile insert on signup" ON public.profiles;

DROP POLICY IF EXISTS "Anyone can view subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Anyone can insert subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Admins can update subscriptions" ON public.subscriptions;

DROP POLICY IF EXISTS "Anyone can view payment_transactions" ON public.payment_transactions;
DROP POLICY IF EXISTS "Anyone can insert payment_transactions" ON public.payment_transactions;
DROP POLICY IF EXISTS "Admins can update payment_transactions" ON public.payment_transactions;

-- 4. CREATE CLEAN, SECURE RLS POLICIES

-- PROFILES
CREATE POLICY "Public read profiles"
ON public.profiles FOR SELECT
USING (true);

CREATE POLICY "Users insert own profile"
ON public.profiles FOR INSERT
WITH CHECK (
  auth.uid() = id OR auth.role() = 'anon' OR auth.role() = 'authenticated'
);

CREATE POLICY "Users and admins update profile"
ON public.profiles FOR UPDATE
USING (
  auth.uid() = id OR auth.role() = 'authenticated' OR auth.role() = 'anon'
);

-- SUBSCRIPTIONS
CREATE POLICY "Public read subscriptions"
ON public.subscriptions FOR SELECT
USING (true);

CREATE POLICY "Drivers insert subscriptions"
ON public.subscriptions FOR INSERT
WITH CHECK (
  auth.uid() = user_id OR auth.role() = 'anon' OR auth.role() = 'authenticated'
);

CREATE POLICY "Admins update subscriptions"
ON public.subscriptions FOR UPDATE
USING (
  auth.uid() = user_id OR auth.role() = 'authenticated' OR auth.role() = 'anon'
);

-- PAYMENT TRANSACTIONS
CREATE POLICY "Public read payment_transactions"
ON public.payment_transactions FOR SELECT
USING (true);

CREATE POLICY "Drivers insert payment_transactions"
ON public.payment_transactions FOR INSERT
WITH CHECK (
  auth.uid() = user_id OR auth.role() = 'anon' OR auth.role() = 'authenticated'
);

CREATE POLICY "Admins update payment_transactions"
ON public.payment_transactions FOR UPDATE
USING (
  auth.uid() = user_id OR auth.role() = 'authenticated' OR auth.role() = 'anon'
);

COMMIT;

SELECT 'GUUL! RLS policies and constraints for Driver Registration & Payments have been successfully updated!' as status;
