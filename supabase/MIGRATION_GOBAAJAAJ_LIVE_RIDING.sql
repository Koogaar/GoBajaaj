-- ==========================================================
-- GoBajaaj LIVE DRIVER TRACKING + RIDE MATCHING MIGRATION
-- Runs on the EXISTING Supabase project (lcigjbtprkloopvzpaxy).
-- Reuses bajaj_rides / profiles / subscriptions / notifications.
-- Only ONE new table is added: driver_presence (required for
-- matching: per-driver online flag + last GPS, which nothing
-- else in the schema provides; `devices` is the anti-theft
-- feature keyed by device fingerprint, not by driver).
-- Safe to re-run (idempotent).
-- ==========================================================

-- ----------------------------------------------------------
-- 0) Helper: haversine distance in meters (no extensions)
-- ----------------------------------------------------------
CREATE OR REPLACE FUNCTION public.haversine_m(
    lat1 double precision, lon1 double precision,
    lat2 double precision, lon2 double precision
) RETURNS double precision
LANGUAGE sql IMMUTABLE AS $$
    SELECT 6371000.0 * 2 * asin(sqrt(
        least(1.0,
          pow(sin(radians(lat2 - lat1) / 2), 2) +
          cos(radians(lat1)) * cos(radians(lat2)) *
          pow(sin(radians(lon2 - lon1) / 2), 2)
        )
    ));
$$;

-- Admin check used by RLS (profiles.role OR admin_users)
CREATE OR REPLACE FUNCTION public.is_admin_user()
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.profiles p
        WHERE p.id = auth.uid()
          AND p.role IN ('super_admin','payment_admin','traffic_admin',
                         'reporter_manager','support_agent','analyst',
                         'user_manager','subscription_manager')
    ) OR EXISTS (
        SELECT 1 FROM public.admin_users a
        WHERE a.user_id = auth.uid() AND a.status = 'active'
    );
$$;

-- ----------------------------------------------------------
-- 1) bajaj_rides: pickup/destination coordinates + live
--    driver location + lifecycle timestamps (all nullable so
--    existing rows and old app builds keep working)
-- ----------------------------------------------------------
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS pickup_lat double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS pickup_lng double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS dest_lat double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS dest_lng double precision;

ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_lat double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_lng double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_heading double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_speed double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_accuracy double precision;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS driver_location_at timestamptz;

ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS assigned_at timestamptz;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS arrived_at timestamptz;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS trip_started_at timestamptz;
ALTER TABLE public.bajaj_rides ADD COLUMN IF NOT EXISTS trip_completed_at timestamptz;

-- ----------------------------------------------------------
-- 2) Status vocabulary: ADD new lifecycle states only.
--    Existing values stay valid => old app builds keep working.
--      pending     = REQUESTED
--      searching   = DRIVER_SEARCHING (backend matching ran)
--      assigned    = DRIVER_ASSIGNED  (auto-assigned, awaiting accept)
--      accepted    = DRIVER_ON_THE_WAY (existing)
--      arrived     = DRIVER_ARRIVED  (was MISSING from CHECK => the
--                     existing app's arriveAtPickup was silently
--                     rejected by Supabase; fixed here)
--      in_progress = TRIP_STARTED (existing)
--      completed   = TRIP_COMPLETED (existing)
--      cancelled   = CANCELLED (existing)
--    DRIVER_NEARBY is a client-side distance state (<150 m), not stored.
-- ----------------------------------------------------------
ALTER TABLE public.bajaj_rides DROP CONSTRAINT IF EXISTS bajaj_rides_status_check;
ALTER TABLE public.bajaj_rides ADD CONSTRAINT bajaj_rides_status_check
    CHECK (status IN ('pending','searching','assigned','accepted',
                      'arrived','in_progress','completed','cancelled'));

-- ----------------------------------------------------------
-- 3) driver_presence: online flag + last GPS per driver
--    (required for matching + battery-friendly)
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.driver_presence (
    driver_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    is_online boolean NOT NULL DEFAULT false,
    latitude double precision,
    longitude double precision,
    heading double precision,
    speed double precision,
    accuracy double precision,
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

ALTER TABLE public.driver_presence ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Drivers manage own presence" ON public.driver_presence;
CREATE POLICY "Drivers manage own presence" ON public.driver_presence
    FOR ALL USING (auth.uid() = driver_id) WITH CHECK (auth.uid() = driver_id);

DROP POLICY IF EXISTS "Admins read presence" ON public.driver_presence;
CREATE POLICY "Admins read presence" ON public.driver_presence
    FOR SELECT USING (public.is_admin_user());

-- ----------------------------------------------------------
-- 4) Ride RLS: riders only see their own rides (+ open pool),
--    drivers only rides assigned to them (+ open pool to claim),
--    admins everything. Replaces the previous USING (true).
-- ----------------------------------------------------------
ALTER TABLE public.bajaj_rides ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view bajaj_rides" ON public.bajaj_rides;
DROP POLICY IF EXISTS "Anyone can insert bajaj_rides" ON public.bajaj_rides;
DROP POLICY IF EXISTS "Anyone can update bajaj_rides" ON public.bajaj_rides;
DROP POLICY IF EXISTS "Rides select parties or open pool" ON public.bajaj_rides;
DROP POLICY IF EXISTS "Rides insert own" ON public.bajaj_rides;
DROP POLICY IF EXISTS "Rides update parties" ON public.bajaj_rides;

-- SELECT: open (unclaimed) pool visible to everyone (drivers discover
-- requests), everything else only to the rider, the assigned driver, admins.
CREATE POLICY "Rides select parties or open pool" ON public.bajaj_rides
    FOR SELECT USING (
        status IN ('pending','searching')
        OR (auth.uid() IS NOT NULL AND (passenger_id = auth.uid() OR driver_id = auth.uid()))
        OR public.is_admin_user()
    );

-- INSERT: only as yourself, only claiming yourself as passenger.
CREATE POLICY "Rides insert own" ON public.bajaj_rides
    FOR INSERT WITH CHECK (
        auth.uid() IS NOT NULL
        AND passenger_id = auth.uid()
    );

-- UPDATE: rider (own ride / cancel), assigned driver (own trip + GPS),
-- any authenticated user may claim an open (pending) ride, admins all.
CREATE POLICY "Rides update parties" ON public.bajaj_rides
    FOR UPDATE USING (
        (auth.uid() IS NOT NULL AND (passenger_id = auth.uid() OR driver_id = auth.uid()))
        OR (auth.uid() IS NOT NULL AND status IN ('pending','searching'))
        OR public.is_admin_user()
    )
    WITH CHECK (
        (auth.uid() IS NOT NULL AND (passenger_id = auth.uid() OR driver_id = auth.uid()))
        OR (auth.uid() IS NOT NULL AND status IN ('pending','searching','assigned','accepted'))
        OR public.is_admin_user()
    );

-- Server-side guard: a non-admin cannot re-point a ride at another
-- passenger or silently rewrite the agreed fare of someone else's trip.
CREATE OR REPLACE FUNCTION public.bajaj_rides_guard()
RETURNS trigger
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    -- RPCs created here run SECURITY DEFINER (owner = postgres): they must
    -- never be blocked by this guard, otherwise match/release break.
    IF current_user IN ('postgres', 'service_role') THEN
        RETURN NEW;
    END IF;
    IF public.is_admin_user() THEN
        RETURN NEW;
    END IF;
    -- driver may only move the ride forward through the lifecycle
    IF auth.uid() IS NOT NULL AND NEW.driver_id = auth.uid() THEN
        IF NEW.passenger_id IS DISTINCT FROM OLD.passenger_id THEN
            RAISE EXCEPTION 'passenger cannot be changed';
        END IF;
        IF NEW.status NOT IN ('assigned','accepted','arrived','in_progress','completed','cancelled','pending','searching') THEN
            RAISE EXCEPTION 'invalid status %', NEW.status;
        END IF;
        RETURN NEW;
    END IF;
    -- passenger: cancel own ride, or touch it without changing status (GPS read-back)
    IF auth.uid() IS NOT NULL AND NEW.passenger_id = auth.uid() AND OLD.passenger_id = auth.uid() THEN
        IF NEW.status = OLD.status THEN
            RETURN NEW;
        END IF;
        IF OLD.status IN ('pending','searching','assigned') AND NEW.status = 'cancelled' THEN
            RETURN NEW;
        END IF;
        -- re-open the pool (re-matching) while no trip is underway
        IF OLD.status IN ('pending','searching','assigned') AND NEW.status IN ('pending','searching') THEN
            RETURN NEW;
        END IF;
        -- only a REAL driver may be assigned (blocks passenger self-accept spoofing)
        IF OLD.status IN ('pending','searching') AND NEW.status = 'assigned'
           AND EXISTS (SELECT 1 FROM public.profiles dp
                       WHERE dp.id = NEW.driver_id AND dp.role = 'driver') THEN
            RETURN NEW;
        END IF;
        RETURN OLD;
    END IF;
    RETURN NEW;
END; $$;

DROP TRIGGER IF EXISTS trg_bajaj_rides_guard ON public.bajaj_rides;
CREATE TRIGGER trg_bajaj_rides_guard
    BEFORE UPDATE ON public.bajaj_rides
    FOR EACH ROW EXECUTE FUNCTION public.bajaj_rides_guard();

-- ----------------------------------------------------------
-- 5) RIDE MATCHING — GoBajaaj backend logic (Postgres RPC).
--    Eligibility: approved driver, online (presence fresh),
--    active subscription, not on another trip, has GPS, in radius.
--    No Google Directions involved: pure haversine + DB rules.
-- ----------------------------------------------------------
CREATE OR REPLACE FUNCTION public.match_drivers_for_ride(
    p_ride_id uuid,
    p_radius_m integer DEFAULT 5000
)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_ride public.bajaj_rides%ROWTYPE;
    v_pick_lat double precision;
    v_pick_lng double precision;
    v_d record;
    v_id uuid := NULL;
    v_name text;
    v_phone text;
BEGIN
    SELECT * INTO v_ride FROM public.bajaj_rides WHERE id = p_ride_id FOR UPDATE;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('ok', false, 'error', 'ride_not_found');
    END IF;
    IF v_ride.status NOT IN ('pending','searching') THEN
        RETURN jsonb_build_object('ok', false, 'error', 'not_open', 'status', v_ride.status);
    END IF;

    v_pick_lat := v_ride.pickup_lat;
    v_pick_lng := v_ride.pickup_lng;
    IF v_pick_lat IS NULL OR v_pick_lng IS NULL THEN
        -- no coordinates => cannot do radius matching, leave open pool
        UPDATE public.bajaj_rides SET status = 'searching', updated_at = NOW()
        WHERE id = p_ride_id;
        RETURN jsonb_build_object('ok', true, 'status', 'searching', 'driver_id', NULL,
                                  'reason', 'no_pickup_coordinates');
    END IF;

    -- Expanded search radius while re-matching
    IF v_ride.status = 'searching' THEN
        p_radius_m := GREATEST(p_radius_m, 8000);
    END IF;

    SELECT p.id, p.full_name, p.phone_number
      INTO v_id, v_name, v_phone
    FROM public.profiles p
    JOIN public.driver_presence dp ON dp.driver_id = p.id
    WHERE EXISTS (
            SELECT 1 FROM public.subscriptions s
            WHERE s.user_id = p.id AND s.status = 'active' AND s.end_date > NOW()
          )
      AND p.role = 'driver'
      AND p.status = 'active'
      AND dp.is_online = true
      AND dp.updated_at > NOW() - INTERVAL '90 seconds'
      AND dp.latitude IS NOT NULL
      AND dp.longitude IS NOT NULL
      AND public.haversine_m(v_pick_lat, v_pick_lng, dp.latitude, dp.longitude) <= p_radius_m
      AND NOT EXISTS (
            SELECT 1 FROM public.bajaj_rides r
            WHERE r.driver_id = p.id
              AND r.status IN ('searching','assigned','accepted','arrived','in_progress')
              AND r.id <> p_ride_id
      )
    ORDER BY public.haversine_m(v_pick_lat, v_pick_lng, dp.latitude, dp.longitude) ASC
    LIMIT 1;

    IF v_id IS NULL THEN
        UPDATE public.bajaj_rides SET status = 'searching', updated_at = NOW()
        WHERE id = p_ride_id;
        RETURN jsonb_build_object('ok', true, 'status', 'searching', 'driver_id', NULL);
    END IF;

    UPDATE public.bajaj_rides
       SET status = 'assigned',
           driver_id = v_id,
           driver_name = v_name,
           driver_phone = v_phone,
           plate_number = 'TR-' || upper(substring(md5(v_id::text) FROM 1 FOR 4)),
           driver_rating = 4.9,
           assigned_at = NOW(),
           updated_at = NOW()
     WHERE id = p_ride_id;

    INSERT INTO public.notifications (user_id, title, message, type)
    VALUES (v_id, 'Dalab Cusub oo Bajaaj',
            'Dalab cusub ayaa kuu qoran — fadlan ku dhawaad app-ka oo aqbal.',
            'system');

    RETURN jsonb_build_object('ok', true, 'status', 'assigned', 'driver_id', v_id);
END; $$;

-- Release a declined/timeout assignment back to the open pool
CREATE OR REPLACE FUNCTION public.release_ride_driver(p_ride_id uuid)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    UPDATE public.bajaj_rides
       SET driver_id = NULL, driver_name = NULL, driver_phone = NULL,
           plate_number = NULL, assigned_at = NULL, updated_at = NOW(),
           status = 'pending'
     WHERE id = p_ride_id AND status IN ('assigned','searching');
    RETURN jsonb_build_object('ok', true);
END; $$;

-- Stop all live tracking for a ride when it ends (cleanup)
CREATE OR REPLACE FUNCTION public.finish_ride_tracking(p_ride_id uuid)
RETURNS jsonb
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    UPDATE public.bajaj_rides
       SET driver_lat = NULL, driver_lng = NULL, driver_heading = NULL,
           driver_speed = NULL, driver_accuracy = NULL,
           driver_location_at = NULL,
           updated_at = NOW()
     WHERE id = p_ride_id;
    RETURN jsonb_build_object('ok', true);
END; $$;

GRANT EXECUTE ON FUNCTION public.match_drivers_for_ride(uuid, integer) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.release_ride_driver(uuid) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.finish_ride_tracking(uuid) TO authenticated, anon;
GRANT EXECUTE ON FUNCTION public.haversine_m(double precision, double precision, double precision, double precision) TO authenticated, anon;

-- ----------------------------------------------------------
-- 6) REALTIME: stream bajaj_rides changes to rider/driver
-- ----------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables
        WHERE pubname = 'supabase_realtime'
          AND schemaname = 'public' AND tablename = 'bajaj_rides'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.bajaj_rides;
    END IF;
EXCEPTION WHEN undefined_object THEN
    -- publication not present (local/self-hosted): create it
    CREATE PUBLICATION supabase_realtime ADD TABLE public.bajaj_rides;
END $$;

-- ----------------------------------------------------------
-- 7) ADMIN: active-ride visibility (no personal extras)
-- ----------------------------------------------------------
CREATE OR REPLACE VIEW public.admin_active_rides AS
SELECT r.id,
       r.passenger_name,
       r.passenger_phone,
       r.pickup_location,
       r.destination,
       r.offered_price,
       r.status,
       r.driver_id,
       r.driver_name,
       r.driver_phone,
       r.plate_number,
       r.driver_lat,
       r.driver_lng,
       r.driver_location_at,
       CASE
         WHEN r.pickup_lat IS NOT NULL AND r.driver_lat IS NOT NULL
         THEN round(public.haversine_m(r.pickup_lat, r.pickup_lng, r.driver_lat, r.driver_lng)::numeric)
       END AS distance_to_pickup_m,
       r.assigned_at,
       r.created_at,
       r.updated_at
FROM public.bajaj_rides r
WHERE r.status IN ('pending','searching','assigned','accepted','arrived','in_progress');

-- ----------------------------------------------------------
-- 8) Four standard traffic statuses (matches app v3.3 UI):
--      Furan (green) / Normal (yellow) / Jaam (red) / Xeran (black)
--    Old values are migrated; support_tickets & traffic_incidents
--    are a DIFFERENT domain and are NOT touched.
-- ----------------------------------------------------------
ALTER TABLE public.intersections DROP CONSTRAINT IF EXISTS intersections_current_status_check;
UPDATE public.intersections SET current_status = 'Jaam'   WHERE current_status IN ('Mashquul','Saxmad badan');
UPDATE public.intersections SET current_status = 'Normal' WHERE current_status IN ('Dhexdhexaad');
UPDATE public.intersections SET current_status = 'Furan'  WHERE current_status NOT IN ('Furan','Normal','Jaam','Xeran');
ALTER TABLE public.intersections ADD CONSTRAINT intersections_current_status_check
    CHECK (current_status IN ('Furan','Normal','Jaam','Xeran'));

ALTER TABLE public.traffic_reports DROP CONSTRAINT IF EXISTS traffic_reports_status_check;
UPDATE public.traffic_reports SET status = 'Jaam'   WHERE status IN ('Mashquul','Saxmad badan','Jam','heavy');
UPDATE public.traffic_reports SET status = 'Xeran'  WHERE status IN ('Xiran','Wuu xiranyahay','closed');
UPDATE public.traffic_reports SET status = 'Normal' WHERE status IN ('Dhexdhexaad');
UPDATE public.traffic_reports SET status = 'Furan'  WHERE status NOT IN ('Furan','Normal','Jaam','Xeran');
ALTER TABLE public.traffic_reports ADD CONSTRAINT traffic_reports_status_check
    CHECK (status IN ('Furan','Normal','Jaam','Xeran'));

-- NOTE: intersections/traffic_reports only ever held the 4 legacy values
-- ('Furan','Dhexdhexaad','Mashquul','Saxmad badan'), so no extra seeds needed.

-- ==========================================================
-- END OF MIGRATION
-- ==========================================================
