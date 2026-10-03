-- ==============================================================================
-- SOMALI TRAFFIC - ADMIN & REPORTERS DATABASE REPAIR SCRIPT
-- ==============================================================================
-- Run this script in your Supabase SQL Editor (https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql)

-- 1. FURI / BALAARI ROLE CHECK CONSTRAINT EE PROFILES
-- Tani waxay suurtagelinaysaa in profiles la geliyo: passenger, driver, reporter, super_admin, payment_admin, traffic_admin, iwm.
ALTER TABLE IF EXISTS public.profiles DROP CONSTRAINT IF EXISTS profiles_role_check;
ALTER TABLE IF EXISTS public.profiles 
  ADD CONSTRAINT profiles_role_check 
  CHECK (role IN ('passenger', 'driver', 'reporter', 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin'));

-- 2. HUBI IN PROFILES ID UU SI TOOS AH U YEESHO UUID HADDIAN LA DIRIN
ALTER TABLE IF EXISTS public.profiles ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- 3. ABUUR MIISKA ADMIN_USERS (MAAMULAYAASHA SYSTEM-KA)
CREATE TABLE IF NOT EXISTS public.admin_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    username TEXT UNIQUE,
    email TEXT,
    full_name TEXT NOT NULL,
    phone TEXT,
    role_key TEXT NOT NULL DEFAULT 'analyst',
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'disabled')),
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. FASAX AMMAANKA RLS MIISKA ADMIN_USERS
ALTER TABLE public.admin_users DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.admin_users TO anon, authenticated, service_role;

-- 5. GELI KOONTADA SUPER ADMIN / OWNER-KA (KOOGAARYARE)
INSERT INTO public.admin_users (username, email, full_name, phone, role_key, status)
VALUES ('owner', 'koogaaryare@gmail.com', 'SomTraffic Owner (Cabdifitax)', '618092060', 'super_admin', 'active')
ON CONFLICT (username) DO UPDATE SET
  email = EXCLUDED.email,
  full_name = EXCLUDED.full_name,
  role_key = EXCLUDED.role_key,
  status = 'active';

-- 6. HUBI FASAXA PROFILES & INTERSECTIONS
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.profiles TO anon, authenticated, service_role;

ALTER TABLE public.intersections DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.intersections TO anon, authenticated, service_role;

-- 7. HUBI FASAXA AUDIT_LOGS
CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_name TEXT,
    action TEXT NOT NULL,
    target_type TEXT,
    target_id TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.audit_logs DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.audit_logs TO anon, authenticated, service_role;

-- 8. ABUUR MIISKA BAJAJ_RIDES (SAFARADA & DALABAADKA BAJAAJTA EE APP-KA IYO WEB ADMIN-KA)
CREATE TABLE IF NOT EXISTS public.bajaj_rides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    passenger_id UUID,
    passenger_name TEXT NOT NULL,
    passenger_phone TEXT NOT NULL,
    pickup_location TEXT NOT NULL,
    destination TEXT NOT NULL,
    vehicle_type TEXT NOT NULL DEFAULT 'Bajaj',
    offered_price DECIMAL(10,2) NOT NULL DEFAULT 1.50,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'in_progress', 'completed', 'cancelled')),
    driver_id UUID,
    driver_name TEXT,
    driver_phone TEXT,
    driver_rating DOUBLE PRECISION DEFAULT 4.9,
    plate_number TEXT DEFAULT 'M123',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.bajaj_rides DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.bajaj_rides TO anon, authenticated, service_role;

-- 9. HUBI FASAXA MIISASKA DEVICES, SUBSCRIPTIONS, SUPPORT TICKETS
ALTER TABLE IF EXISTS public.devices DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.devices TO anon, authenticated, service_role;

ALTER TABLE IF EXISTS public.subscriptions DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.subscriptions TO anon, authenticated, service_role;

ALTER TABLE IF EXISTS public.payment_transactions DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.payment_transactions TO anon, authenticated, service_role;

ALTER TABLE IF EXISTS public.support_tickets DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.support_tickets TO anon, authenticated, service_role;
