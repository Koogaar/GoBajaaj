-- =============================================================================
-- GOBAJAAJ — Driver Pass enforcement (REVIEW BEFORE APPLYING TO PRODUCTION)
-- Project: lcigjbtprkloopvzpaxy
--
-- This migration is additive and backward-compatible:
--   * does not drop tables or truncate data
--   * does not reset auth users
--   * introduces RPCs that use NOW() (UTC) for the 24-hour window
--
-- DO NOT run this against production until the owner reviews and approves it.
-- After approval, run it once in the Supabase SQL Editor.
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- 1) Columns used by Driver Pass + audit (safe IF NOT EXISTS)
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS public.profiles
  ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'inactive';
ALTER TABLE IF EXISTS public.profiles
  ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMPTZ;
ALTER TABLE IF EXISTS public.profiles
  ADD COLUMN IF NOT EXISTS plate_number TEXT;
ALTER TABLE IF EXISTS public.profiles
  ADD COLUMN IF NOT EXISTS bajaaj_model TEXT;

ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS sender_phone TEXT;
ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS destination_phone TEXT;
ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS approved_by UUID;
ALTER TABLE IF EXISTS public.subscriptions
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS public.payment_transactions
  ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE IF EXISTS public.payment_transactions
  ADD COLUMN IF NOT EXISTS approved_by UUID;
ALTER TABLE IF EXISTS public.payment_transactions
  ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

ALTER TABLE IF EXISTS public.subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE IF EXISTS public.subscriptions
  ADD CONSTRAINT subscriptions_status_check
  CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed', 'rejected', 'completed', 'approved'));

ALTER TABLE IF EXISTS public.payment_transactions DROP CONSTRAINT IF EXISTS payment_transactions_status_check;
ALTER TABLE IF EXISTS public.payment_transactions
  ADD CONSTRAINT payment_transactions_status_check
  CHECK (status IN ('completed', 'pending', 'failed', 'rejected', 'approved'));

CREATE INDEX IF NOT EXISTS idx_subscriptions_driver_pending
  ON public.subscriptions (user_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_driver_pending
  ON public.payment_transactions (user_id, status, created_at DESC);
-- Unique pending-per-driver is enforced in submit_driver_pass_payment.
-- A unique index is NOT created here because existing duplicate pending
-- rows in production would abort the transaction.

-- ---------------------------------------------------------------------------
-- 2) Eligibility helper — trusted server time, never the phone clock
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.driver_has_active_pass(p_user_id UUID)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.subscriptions s
    WHERE s.user_id = p_user_id
      AND lower(s.status) = 'active'
      AND s.end_date IS NOT NULL
      AND s.end_date > NOW()
  );
$$;

REVOKE ALL ON FUNCTION public.driver_has_active_pass(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.driver_has_active_pass(UUID) TO authenticated;

-- ---------------------------------------------------------------------------
-- 3) Admin permission helper
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.is_admin_user()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.admin_users a
    WHERE a.user_id = auth.uid()
      AND a.status = 'active'
  );
$$;

CREATE OR REPLACE FUNCTION public.is_payment_admin()
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.admin_users a
    WHERE a.user_id = auth.uid()
      AND a.status = 'active'
      AND a.role_key IN ('super_admin', 'payment_admin', 'subscription_manager')
  );
$$;

-- ---------------------------------------------------------------------------
-- 4) Driver submits $0.25 claim (pending until admin approval)
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.submit_driver_pass_payment(
    p_sender_phone TEXT,
    p_destination_phone TEXT,
    p_payment_method TEXT DEFAULT 'EVC Plus',
    p_reference TEXT DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_sub_id UUID;
    v_tx_id UUID;
    v_ref TEXT;
    v_existing_tx UUID;
BEGIN
    IF v_uid IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;

    IF public.driver_has_active_pass(v_uid) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'pass_still_active');
    END IF;

    SELECT id INTO v_existing_tx
    FROM public.payment_transactions
    WHERE user_id = v_uid AND status = 'pending'
    ORDER BY created_at DESC
    LIMIT 1;

    IF v_existing_tx IS NOT NULL THEN
        SELECT subscription_id INTO v_sub_id
        FROM public.payment_transactions WHERE id = v_existing_tx;
        RETURN jsonb_build_object(
            'ok', true,
            'duplicate', true,
            'status', 'pending',
            'payment_id', v_existing_tx,
            'subscription_id', v_sub_id
        );
    END IF;

    SELECT id INTO v_sub_id
    FROM public.subscriptions
    WHERE user_id = v_uid AND status = 'pending'
    ORDER BY created_at DESC
    LIMIT 1;

    v_ref := COALESCE(NULLIF(trim(p_reference), ''),
                      'PASS-' || right(regexp_replace(COALESCE(p_sender_phone, ''), '\D', '', 'g'), 4)
                      || '-' || floor(extract(epoch FROM now()))::bigint);

    IF v_sub_id IS NULL THEN
        INSERT INTO public.subscriptions (
            user_id, plan_name, amount, currency, status,
            payment_method, payment_reference, auto_renew,
            sender_phone, destination_phone, start_date, end_date
        ) VALUES (
            v_uid,
            'Driver Pass (24h)',
            0.25,
            'USD',
            'pending',
            COALESCE(NULLIF(trim(p_payment_method), ''), 'EVC Plus'),
            v_ref,
            false,
            p_sender_phone,
            p_destination_phone,
            NOW(),
            NOW()  -- overwritten on approval; pending never grants access
        )
        RETURNING id INTO v_sub_id;
    ELSE
        UPDATE public.subscriptions
           SET payment_method = COALESCE(NULLIF(trim(p_payment_method), ''), payment_method),
               sender_phone = p_sender_phone,
               destination_phone = p_destination_phone,
               payment_reference = v_ref,
               status = 'pending',
               updated_at = NOW()
         WHERE id = v_sub_id;
    END IF;

    INSERT INTO public.payment_transactions (
        subscription_id, user_id, amount, currency, payment_method,
        reference_number, phone_number, status
    ) VALUES (
        v_sub_id, v_uid, 0.25, 'USD',
        COALESCE(NULLIF(trim(p_payment_method), ''), 'EVC Plus'),
        v_ref, p_sender_phone, 'pending'
    )
    RETURNING id INTO v_tx_id;

    UPDATE public.profiles
       SET subscription_status = 'pending',
           updated_at = NOW()
     WHERE id = v_uid;

    RETURN jsonb_build_object(
        'ok', true,
        'duplicate', false,
        'status', 'pending',
        'payment_id', v_tx_id,
        'subscription_id', v_sub_id,
        'reference_number', v_ref
    );
END;
$$;

REVOKE ALL ON FUNCTION public.submit_driver_pass_payment(TEXT, TEXT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_driver_pass_payment(TEXT, TEXT, TEXT, TEXT) TO authenticated;

-- ---------------------------------------------------------------------------
-- 5) Admin approve — server-side NOW() + 24 hours. Never stacks extra days.
--    Accepts either a payment_transactions.id OR (for subscription-only pending
--    entries) a subscriptions.id.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.approve_driver_pass_payment(p_payment_id UUID)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_tx public.payment_transactions%ROWTYPE;
    v_sub public.subscriptions%ROWTYPE;
    v_start TIMESTAMPTZ;
    v_end TIMESTAMPTZ;
    v_sub_id UUID;
    v_user_id UUID;
BEGIN
    IF auth.uid() IS NULL OR NOT public.is_payment_admin() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'forbidden');
    END IF;

    SELECT * INTO v_tx
    FROM public.payment_transactions
    WHERE id = p_payment_id
    FOR UPDATE;

    IF FOUND THEN
        IF v_tx.status <> 'pending' THEN
            RETURN jsonb_build_object('ok', false, 'error', 'not_pending', 'status', v_tx.status);
        END IF;
        v_user_id := v_tx.user_id;
        v_sub_id := v_tx.subscription_id;
    ELSE
        -- p_payment_id is a subscriptions.id (pending entry without a transaction row).
        SELECT * INTO v_sub
        FROM public.subscriptions
        WHERE id = p_payment_id
        FOR UPDATE;

        IF NOT FOUND THEN
            RETURN jsonb_build_object('ok', false, 'error', 'payment_not_found');
        END IF;
        IF lower(v_sub.status) <> 'pending' THEN
            RETURN jsonb_build_object('ok', false, 'error', 'not_pending', 'status', v_sub.status);
        END IF;

        v_user_id := v_sub.user_id;
        v_sub_id := v_sub.id;

        INSERT INTO public.payment_transactions (
            subscription_id, user_id, amount, currency, payment_method,
            reference_number, phone_number, status, approved_by, approved_at
        ) VALUES (
            v_sub.id, v_sub.user_id, COALESCE(v_sub.amount, 0.25),
            COALESCE(v_sub.currency, 'USD'), COALESCE(v_sub.payment_method, 'EVC Plus'),
            v_sub.payment_reference, v_sub.sender_phone, 'completed', auth.uid(), NOW()
        );
    END IF;

    v_start := NOW();
    v_end := v_start + INTERVAL '24 hours';

    IF FOUND AND v_tx.id IS NOT NULL THEN
        UPDATE public.payment_transactions
           SET status = 'completed',
               approved_by = auth.uid(),
               approved_at = v_start
         WHERE id = v_tx.id
           AND status = 'pending';

        IF NOT FOUND THEN
            RETURN jsonb_build_object('ok', false, 'error', 'already_processed');
        END IF;
    END IF;

    -- Expire any other active passes for this driver (no overlapping extra access)
    UPDATE public.subscriptions
       SET status = 'expired', updated_at = v_start
     WHERE user_id = v_user_id
       AND status = 'active'
       AND (v_sub_id IS NULL OR id <> v_sub_id);

    IF v_sub_id IS NOT NULL THEN
        UPDATE public.subscriptions
           SET status = 'active',
               start_date = v_start,
               end_date = v_end,
               approved_by = auth.uid(),
               approved_at = v_start,
               rejection_reason = NULL,
               updated_at = v_start
         WHERE id = v_sub_id;
    END IF;

    UPDATE public.profiles
       SET status = 'active',
           subscription_status = 'active',
           subscription_expiry = v_end,
           updated_at = v_start
     WHERE id = v_user_id;

    INSERT INTO public.audit_logs (actor_name, action, target_type, target_id, metadata)
    VALUES (
        COALESCE((SELECT full_name FROM public.admin_users WHERE user_id = auth.uid() LIMIT 1), 'admin'),
        'PAYMENT_APPROVED',
        'payment_transaction',
        p_payment_id::text,
        jsonb_build_object(
            'user_id', v_user_id,
            'subscription_id', v_sub_id,
            'start', v_start,
            'end', v_end,
            'amount', 0.25
        )
    );

    RETURN jsonb_build_object(
        'ok', true,
        'status', 'active',
        'start_date', v_start,
        'end_date', v_end,
        'subscription_id', v_sub_id
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.reject_driver_pass_payment(
    p_payment_id UUID,
    p_reason TEXT DEFAULT 'Rejected by admin'
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_tx public.payment_transactions%ROWTYPE;
    v_sub public.subscriptions%ROWTYPE;
    v_reason TEXT := COALESCE(NULLIF(trim(p_reason), ''), 'Rejected by admin');
    v_user_id UUID;
    v_sub_id UUID;
BEGIN
    IF auth.uid() IS NULL OR NOT public.is_payment_admin() THEN
        RETURN jsonb_build_object('ok', false, 'error', 'forbidden');
    END IF;

    SELECT * INTO v_tx FROM public.payment_transactions WHERE id = p_payment_id FOR UPDATE;

    IF FOUND THEN
        IF v_tx.status <> 'pending' THEN
            RETURN jsonb_build_object('ok', false, 'error', 'not_pending', 'status', v_tx.status);
        END IF;
        v_user_id := v_tx.user_id;
        v_sub_id := v_tx.subscription_id;

        UPDATE public.payment_transactions
           SET status = 'rejected',
               rejection_reason = v_reason,
               approved_by = auth.uid(),
               approved_at = NOW()
         WHERE id = p_payment_id AND status = 'pending';
    ELSE
        SELECT * INTO v_sub FROM public.subscriptions WHERE id = p_payment_id FOR UPDATE;
        IF NOT FOUND THEN
            RETURN jsonb_build_object('ok', false, 'error', 'payment_not_found');
        END IF;
        IF lower(v_sub.status) <> 'pending' THEN
            RETURN jsonb_build_object('ok', false, 'error', 'not_pending', 'status', v_sub.status);
        END IF;
        v_user_id := v_sub.user_id;
        v_sub_id := v_sub.id;
    END IF;

    IF v_sub_id IS NOT NULL THEN
        UPDATE public.subscriptions
           SET status = 'rejected',
               rejection_reason = v_reason,
               updated_at = NOW()
         WHERE id = v_sub_id;
    END IF;

    UPDATE public.profiles
       SET subscription_status = 'rejected',
           updated_at = NOW()
     WHERE id = v_user_id
       AND COALESCE(subscription_status, '') <> 'active';

    INSERT INTO public.audit_logs (actor_name, action, target_type, target_id, metadata)
    VALUES (
        COALESCE((SELECT full_name FROM public.admin_users WHERE user_id = auth.uid() LIMIT 1), 'admin'),
        'PAYMENT_REJECTED',
        'payment_transaction',
        p_payment_id::text,
        jsonb_build_object('reason', v_reason, 'user_id', v_user_id)
    );

    RETURN jsonb_build_object('ok', true, 'status', 'rejected');
END;
$$;

REVOKE ALL ON FUNCTION public.approve_driver_pass_payment(UUID) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.reject_driver_pass_payment(UUID, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.approve_driver_pass_payment(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION public.reject_driver_pass_payment(UUID, TEXT) TO authenticated;

-- ---------------------------------------------------------------------------
-- 6) Ride accept — recheck eligibility on the server
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.accept_ride_as_driver(
    p_ride_id UUID,
    p_agreed_fare NUMERIC DEFAULT NULL
)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_uid UUID := auth.uid();
    v_ride public.bajaj_rides%ROWTYPE;
    v_name TEXT;
    v_phone TEXT;
    v_plate TEXT;
BEGIN
    IF v_uid IS NULL THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_authenticated');
    END IF;
    IF NOT public.driver_has_active_pass(v_uid) THEN
        RETURN jsonb_build_object('ok', false, 'error', 'pass_inactive');
    END IF;

    SELECT * INTO v_ride FROM public.bajaj_rides WHERE id = p_ride_id FOR UPDATE;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'ride_not_found');
    END IF;
    IF v_ride.status NOT IN ('pending', 'searching', 'assigned') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_open', 'status', v_ride.status);
    END IF;
    IF v_ride.driver_id IS NOT NULL AND v_ride.driver_id <> v_uid AND v_ride.status = 'assigned' THEN
        RETURN jsonb_build_object('ok', false, 'error', 'assigned_to_other');
    END IF;

    SELECT full_name, phone_number, plate_number
      INTO v_name, v_phone, v_plate
    FROM public.profiles WHERE id = v_uid;

    UPDATE public.bajaj_rides
       SET status = 'accepted',
           driver_id = v_uid,
           driver_name = COALESCE(v_name, driver_name),
           driver_phone = COALESCE(v_phone, driver_phone),
           plate_number = COALESCE(v_plate, plate_number),
           offered_price = COALESCE(p_agreed_fare, offered_price),
           updated_at = NOW()
     WHERE id = p_ride_id;

    RETURN jsonb_build_object('ok', true, 'status', 'accepted');
END;
$$;

REVOKE ALL ON FUNCTION public.accept_ride_as_driver(UUID, NUMERIC) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.accept_ride_as_driver(UUID, NUMERIC) TO authenticated;

-- Tighten matching RPC: already filters active unexpired subscriptions.
-- Also force-expire stale active rows using server time (non-destructive).
UPDATE public.subscriptions
   SET status = 'expired', updated_at = NOW()
 WHERE status = 'active'
   AND end_date IS NOT NULL
   AND end_date <= NOW();

UPDATE public.profiles
   SET subscription_status = 'expired', updated_at = NOW()
 WHERE subscription_status = 'active'
   AND subscription_expiry IS NOT NULL
   AND subscription_expiry <= NOW();

-- Presence: drivers cannot mark themselves online without an active pass
CREATE OR REPLACE FUNCTION public.driver_presence_guard()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NEW.is_online IS TRUE THEN
        IF auth.uid() IS NULL OR NEW.driver_id <> auth.uid() THEN
            IF current_user NOT IN ('postgres', 'service_role') THEN
                RAISE EXCEPTION 'presence_forbidden';
            END IF;
        END IF;
        IF NOT public.driver_has_active_pass(NEW.driver_id) THEN
            RAISE EXCEPTION 'driver_pass_required';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_driver_presence_guard ON public.driver_presence;
DO $$
BEGIN
    IF to_regclass('public.driver_presence') IS NOT NULL THEN
        CREATE TRIGGER trg_driver_presence_guard
            BEFORE INSERT OR UPDATE ON public.driver_presence
            FOR EACH ROW EXECUTE FUNCTION public.driver_presence_guard();
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 7) Least-privilege RLS (does not GRANT to anon beyond existing traffic reads)
-- ---------------------------------------------------------------------------
ALTER TABLE IF EXISTS public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.bajaj_rides ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users insert own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users and admins update profile" ON public.profiles;
DROP POLICY IF EXISTS "Public read profiles" ON public.profiles;
DROP POLICY IF EXISTS "profiles_select_own_or_admin" ON public.profiles;
DROP POLICY IF EXISTS "profiles_insert_own" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update_own_or_admin" ON public.profiles;

CREATE POLICY "profiles_select_own_or_admin" ON public.profiles
  FOR SELECT USING (auth.uid() = id OR public.is_payment_admin() OR public.is_admin_user());
CREATE POLICY "profiles_insert_own" ON public.profiles
  FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update_own_or_admin" ON public.profiles
  FOR UPDATE USING (auth.uid() = id OR public.is_payment_admin() OR public.is_admin_user());

DROP POLICY IF EXISTS "Public read subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Drivers insert subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "Admins update subscriptions" ON public.subscriptions;
DROP POLICY IF EXISTS "subs_select_own_or_admin" ON public.subscriptions;
DROP POLICY IF EXISTS "subs_insert_own" ON public.subscriptions;
DROP POLICY IF EXISTS "subs_update_admin" ON public.subscriptions;

CREATE POLICY "subs_select_own_or_admin" ON public.subscriptions
  FOR SELECT USING (auth.uid() = user_id OR public.is_payment_admin() OR public.is_admin_user());
CREATE POLICY "subs_insert_own" ON public.subscriptions
  FOR INSERT WITH CHECK (auth.uid() = user_id AND status = 'pending');
CREATE POLICY "subs_update_admin" ON public.subscriptions
  FOR UPDATE USING (public.is_payment_admin() OR public.is_admin_user());

DROP POLICY IF EXISTS "Public read payment_transactions" ON public.payment_transactions;
DROP POLICY IF EXISTS "Drivers insert payment_transactions" ON public.payment_transactions;
DROP POLICY IF EXISTS "Admins update payment_transactions" ON public.payment_transactions;
DROP POLICY IF EXISTS "pay_select_own_or_admin" ON public.payment_transactions;
DROP POLICY IF EXISTS "pay_insert_own" ON public.payment_transactions;
DROP POLICY IF EXISTS "pay_update_admin" ON public.payment_transactions;

CREATE POLICY "pay_select_own_or_admin" ON public.payment_transactions
  FOR SELECT USING (auth.uid() = user_id OR public.is_payment_admin() OR public.is_admin_user());
CREATE POLICY "pay_insert_own" ON public.payment_transactions
  FOR INSERT WITH CHECK (auth.uid() = user_id AND status = 'pending');
CREATE POLICY "pay_update_admin" ON public.payment_transactions
  FOR UPDATE USING (public.is_payment_admin() OR public.is_admin_user());

-- ---------------------------------------------------------------------------
-- 8) Defense-in-depth: a non-admin may never self-activate a pass or change
--    role/status/subscription_expiry through a direct profile PATCH/upsert.
--    Legitimate activation happens only inside the SECURITY DEFINER RPCs.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.profiles_guard_self_update()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF current_user IN ('postgres', 'service_role') THEN
        RETURN NEW;
    END IF;
    IF public.is_admin_user() THEN
        RETURN NEW;
    END IF;

    IF NEW.role IS DISTINCT FROM OLD.role OR NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'profile_privileged_field_forbidden';
    END IF;

    IF NEW.subscription_status IS DISTINCT FROM OLD.subscription_status
       AND lower(COALESCE(NEW.subscription_status, '')) NOT IN
           ('pending', 'inactive', 'expired', 'rejected', 'failed') THEN
        RAISE EXCEPTION 'profile_subscription_status_forbidden';
    END IF;

    IF NEW.subscription_expiry IS DISTINCT FROM OLD.subscription_expiry
       AND NEW.subscription_expiry IS NOT NULL THEN
        RAISE EXCEPTION 'profile_subscription_expiry_forbidden';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_guard_self_update ON public.profiles;
CREATE TRIGGER trg_profiles_guard_self_update
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.profiles_guard_self_update();

-- ---------------------------------------------------------------------------
-- 9) Defense-in-depth: a driver may only create subscriptions in 'pending',
--    never 'active', even if a policy is later loosened by mistake.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.subscriptions_guard_self_insert()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF current_user IN ('postgres', 'service_role') THEN
        RETURN NEW;
    END IF;
    IF public.is_payment_admin() OR public.is_admin_user() THEN
        RETURN NEW;
    END IF;
    IF lower(COALESCE(NEW.status, '')) <> 'pending' THEN
        RAISE EXCEPTION 'subscription_self_activation_forbidden';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_subscriptions_guard_self_insert ON public.subscriptions;
CREATE TRIGGER trg_subscriptions_guard_self_insert
    BEFORE INSERT ON public.subscriptions
    FOR EACH ROW EXECUTE FUNCTION public.subscriptions_guard_self_insert();

COMMIT;

SELECT 'Migration prepared. Review results, then verify RPCs with an admin JWT before shipping.' AS status;
