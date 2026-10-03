-- GoBajaaj Driver Pass submission flow fix
-- Run this migration in Supabase SQL Editor before testing the fixed APK/source.
-- It prevents registration-time fake payments and gives the real payment
-- submission enough fields for Admin Web to trace and approve it.

BEGIN;

ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS sender_phone TEXT;

ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS destination_phone TEXT;

ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE IF EXISTS public.payment_transactions
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

-- The Admin UI can mark a payment/subscription as rejected.
ALTER TABLE IF EXISTS public.subscriptions
  DROP CONSTRAINT IF EXISTS subscriptions_status_check;

ALTER TABLE IF EXISTS public.subscriptions
  ADD CONSTRAINT subscriptions_status_check
  CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed', 'rejected'));

ALTER TABLE IF EXISTS public.payment_transactions
  DROP CONSTRAINT IF EXISTS payment_transactions_status_check;

ALTER TABLE IF EXISTS public.payment_transactions
  ADD CONSTRAINT payment_transactions_status_check
  CHECK (status IN ('completed', 'pending', 'failed', 'rejected'));

CREATE INDEX IF NOT EXISTS idx_subscriptions_driver_pending
  ON public.subscriptions (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_driver_pending
  ON public.payment_transactions (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_subscription
  ON public.payment_transactions (subscription_id, created_at DESC);

COMMIT;
