-- =============================================================================
-- GOBAJAAJ / SOMALI TRAFFIC - FRESH MASTER SUPABASE SQL SETUP (ONE-CLICK INSTALL)
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/lcigjbtprkloopvzpaxy/sql/new
-- =============================================================================

BEGIN;

-- 0. ENABLE REQUIRED EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- -----------------------------------------------------------------------------
-- 1. PROFILES (USERS, DRIVERS, REPORTERS, PASSENGERS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone_number TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'passenger' CHECK (role IN ('passenger', 'driver', 'reporter', 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'disabled', 'pending')),
    avatar_url TEXT,
    plate_number TEXT,
    bajaaj_model TEXT,
    subscription_status TEXT DEFAULT 'pending',
    subscription_expiry TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 2. RBAC LOOKUP TABLES (ROLES, PERMISSIONS, ROLE_PERMISSIONS)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.roles (
    key TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    name_somali TEXT NOT NULL,
    description TEXT,
    badge_bg TEXT DEFAULT 'bg-gray-100',
    badge_text TEXT DEFAULT 'text-gray-800',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.permissions (
    key TEXT PRIMARY KEY,
    description TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'general',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.role_permissions (
    role_key TEXT NOT NULL REFERENCES public.roles(key) ON DELETE CASCADE,
    permission_key TEXT NOT NULL REFERENCES public.permissions(key) ON DELETE CASCADE,
    PRIMARY KEY (role_key, permission_key)
);

CREATE TABLE IF NOT EXISTS public.admin_users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT UNIQUE,
    email TEXT NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT,
    role_key TEXT NOT NULL REFERENCES public.roles(key) DEFAULT 'analyst',
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'disabled')),
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 3. INTERSECTIONS & TRAFFIC DATA
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.intersections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    allowed_radius INTEGER NOT NULL DEFAULT 500,
    city TEXT NOT NULL DEFAULT 'Muqdisho',
    current_status TEXT NOT NULL DEFAULT 'Furan' CHECK (current_status IN ('Furan', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan')),
    assigned_reporter_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    congestion_level INTEGER DEFAULT 0,
    average_speed DOUBLE PRECISION DEFAULT 25.0,
    active_reporters INTEGER DEFAULT 1,
    last_report_time TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.traffic_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    intersection_id UUID NOT NULL REFERENCES public.intersections(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    status TEXT NOT NULL CHECK (status IN ('Furan', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan')),
    cause TEXT,
    notes TEXT,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    distance_from_intersection DOUBLE PRECISION,
    verification_status TEXT NOT NULL DEFAULT 'verified' CHECK (verification_status IN ('verified', 'outside_radius', 'unverified')),
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.traffic_incidents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('Saxmad', 'Shil', 'Waddo xiran', 'Caqabad', 'Khatar waddo')),
    location_name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    severity TEXT NOT NULL CHECK (severity IN ('Dhexdhexaad', 'Sare', 'Degdeg ah')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'resolved', 'investigating')),
    description TEXT,
    reported_by UUID REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 4. BAJAJ RIDES (BAJAJ BOOKING & RIDES)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bajaj_rides (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    passenger_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    passenger_name TEXT NOT NULL,
    passenger_phone TEXT NOT NULL,
    pickup_location TEXT NOT NULL,
    destination TEXT NOT NULL,
    vehicle_type TEXT NOT NULL DEFAULT 'Bajaj',
    offered_price DECIMAL(10,2) NOT NULL DEFAULT 1.50,
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'in_progress', 'completed', 'cancelled')),
    driver_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    driver_name TEXT,
    driver_phone TEXT,
    driver_rating DOUBLE PRECISION DEFAULT 4.9,
    plate_number TEXT DEFAULT 'M123',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 5. SUBSCRIPTIONS & PAYMENT TRANSACTIONS (DRIVER PASS $0.25)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_name TEXT NOT NULL DEFAULT 'Driver Pass',
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.25,
    currency TEXT NOT NULL DEFAULT 'USD',
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed')),
    payment_method TEXT NOT NULL DEFAULT 'EVC Plus',
    payment_reference TEXT,
    sender_phone TEXT,
    destination_phone TEXT,
    rejection_reason TEXT,
    start_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID REFERENCES public.subscriptions(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL DEFAULT 0.25,
    currency TEXT NOT NULL DEFAULT 'USD',
    payment_method TEXT NOT NULL DEFAULT 'EVC Plus',
    reference_number TEXT NOT NULL UNIQUE,
    phone_number TEXT NOT NULL,
    rejection_reason TEXT,
    status TEXT NOT NULL DEFAULT 'completed' CHECK (status IN ('completed', 'pending', 'failed', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 6. DEVICES & PHONE GUARD
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    device_model TEXT NOT NULL,
    platform TEXT NOT NULL DEFAULT 'Android',
    app_version TEXT NOT NULL DEFAULT '1.0.0',
    device_fingerprint TEXT NOT NULL,
    is_protected BOOLEAN NOT NULL DEFAULT true,
    is_lost BOOLEAN NOT NULL DEFAULT false,
    battery_level INTEGER,
    is_online BOOLEAN NOT NULL DEFAULT true,
    last_latitude DOUBLE PRECISION,
    last_longitude DOUBLE PRECISION,
    last_accuracy DOUBLE PRECISION,
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.device_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.device_security_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.trusted_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    relationship TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 7. NOTIFICATIONS, SUPPORT & AUDIT LOGS
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'traffic' CHECK (type IN ('traffic', 'security', 'subscription', 'support', 'system')),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.support_tickets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    ticket_number SERIAL,
    category TEXT NOT NULL CHECK (category IN ('Koontada', 'Lacag-bixinta', 'Saxmadda', 'Ilaalinta Taleefanka', 'Wariyaha', 'Kale')),
    priority TEXT NOT NULL DEFAULT 'Dhexdhexaad' CHECK (priority IN ('Hoose', 'Dhexdhexaad', 'Sare', 'Degdeg ah')),
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Furan' CHECK (status IN ('Furan', 'Socda', 'La xaliyay', 'Xiran')),
    assigned_agent UUID REFERENCES public.profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.support_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_id UUID NOT NULL REFERENCES public.support_tickets(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    actor_name TEXT NOT NULL,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- 8. SEED ROLES & PERMISSIONS DATA
-- -----------------------------------------------------------------------------
INSERT INTO public.roles (key, name, name_somali, description, badge_bg, badge_text) VALUES
('super_admin', 'SUPER ADMIN', 'Maamulaha Guud', 'Fasax buuxa oo habka oo dhan ah. Wuxuu maamuli karaa admin-yada kale.', 'bg-red-100', 'text-red-800'),
('payment_admin', 'PAYMENT ADMIN', 'Maamulaha Lacagaha', 'Ansixinta iyo diidmada bixinta Driver Pass ($0.25/Shift).', 'bg-emerald-100', 'text-emerald-800'),
('traffic_admin', 'TRAFFIC ADMIN', 'Maamulaha Saxmadda', 'Maamulka isgoysyada, xaaladaha saxmadda, iyo warbixinnada gaadiidka.', 'bg-blue-100', 'text-blue-800'),
('reporter_manager', 'REPORTER MANAGER', 'Maamulaha Wariyeyaasha', 'Qorista, maamulida, iyo u xilsaarista wariyeyaasha saxmadda isgoysyada.', 'bg-purple-100', 'text-purple-800'),
('support_agent', 'SUPPORT AGENT', 'Shaqaalaha Taageerada', 'Wadahadalka macaamiisha, cabashooyinka, iyo caawinta darawallada.', 'bg-amber-100', 'text-amber-800'),
('analyst', 'ANALYST', 'Aqoonyahan Xogta', 'Eegida shaxanka, tirakoobka, iyo warbixinnada habka oo kaliya (Read-Only).', 'bg-indigo-100', 'text-indigo-800')
ON CONFLICT (key) DO UPDATE SET
    name = EXCLUDED.name,
    name_somali = EXCLUDED.name_somali,
    description = EXCLUDED.description,
    badge_bg = EXCLUDED.badge_bg,
    badge_text = EXCLUDED.badge_text;

INSERT INTO public.permissions (key, description, category) VALUES
('users.view', 'Eegida akoonnada isticmaalayaasha', 'users'),
('users.create', 'Abuurista akoon cusub', 'users'),
('users.update', 'Wax ka beddelka akoonnada', 'users'),
('users.suspend', 'Hakinta ama xiridda akoonnada', 'users'),
('payments.view', 'Eegida risiidhada iyo lacagaha', 'payments'),
('payments.approve', 'Ansixinta lacag-bixinta Driver Pass', 'payments'),
('payments.reject', 'Diidmada lacag-bixinta', 'payments'),
('payments.update', 'Wax ka beddelka xogta lacagaha', 'payments'),
('subscriptions.view', 'Eegida rukunnada darawallada', 'payments'),
('subscriptions.update', 'Cusbooneysiinta rukunnada', 'payments'),
('traffic.view', 'Eegida warbixinnada saxmadda', 'traffic'),
('traffic.create', 'Gudbinta warbixinta saxmadda', 'traffic'),
('traffic.update', 'Cusbooneysiinta saxmadda isgoyska', 'traffic'),
('traffic.delete', 'Tirtiridda warbixinnada saxmadda', 'traffic'),
('intersections.view', 'Eegida isgoysyada', 'traffic'),
('intersections.create', 'Dhisidda isgoys cusub', 'traffic'),
('intersections.update', 'Beddelidda isgoyska ama wariyaha', 'traffic'),
('intersections.delete', 'Tirtiridda isgoyska', 'traffic'),
('reporters.view', 'Eegida liiska wariyeyaasha', 'reporters'),
('reporters.create', 'Qorista wariye cusub', 'reporters'),
('reporters.update', 'Beddelidda xogta wariyaha', 'reporters'),
('reporters.assign', 'U xilsaarista isgoys wariyaha', 'reporters'),
('reporters.deactivate', 'Awood-beelida wariyaha', 'reporters'),
('protection.view', 'Eegida qalabka iyo Phone Guard', 'protection'),
('protection.commands', 'Gudbinta awaamiirta ammaan', 'protection'),
('notifications.view', 'Eegida ogeysiisyada', 'support'),
('notifications.create', 'Abuurista ogeysiis', 'support'),
('notifications.send', 'Dirida ogeysiisyada', 'support'),
('support.view', 'Eegida cabashooyinka', 'support'),
('support.reply', 'Kabuuxinta jawaabaha taageerada', 'support'),
('support.update', 'Xiridda ama beddelidda cabashada', 'support'),
('analytics.view', 'Eegida shaxanka iyo tirakoobka', 'analytics'),
('analytics.export', 'Dhoofinta xogta tirakoobka', 'analytics'),
('audit_logs.view', 'Eegida audit logs', 'audit'),
('admins.view', 'Eegida maamulayaasha habka', 'admin'),
('admins.create', 'Dhisidda admin cusub', 'admin'),
('admins.update', 'Beddelidda maamulayaasha', 'admin'),
('admins.deactivate', 'Xiridda admin', 'admin'),
('roles.view', 'Eegida doorarka', 'admin'),
('roles.create', 'Abuurista door cusub', 'admin'),
('roles.update', 'Beddelidda doorarka', 'admin'),
('settings.view', 'Eegida hagaajinta habka', 'settings'),
('settings.update', 'Beddelidda hagaajinta', 'settings')
ON CONFLICT (key) DO UPDATE SET
    description = EXCLUDED.description,
    category = EXCLUDED.category;

-- -----------------------------------------------------------------------------
-- 9. SEED INITIAL SUPER ADMIN ACCOUNTS
-- -----------------------------------------------------------------------------
INSERT INTO public.admin_users (id, username, email, full_name, phone, role_key, status) VALUES
('5ae9788f-c6fa-4164-a846-5db62741b092', 'koogaaryare@gmail.com', 'koogaaryare@gmail.com', 'GoBajaaj Super Admin', NULL, 'super_admin', 'active'),
('4c49d2a4-0ec8-4add-9161-5e421559fca1', 'owner', 'kuluntamedia@gmail.com', 'SomTraffic Owner (Cabdifitax)', '618092060', 'super_admin', 'active')
ON CONFLICT (username) DO UPDATE SET
    role_key = 'super_admin',
    status = 'active';

-- -----------------------------------------------------------------------------
-- 10. SEED THE 10 OFFICIAL MOGADISHU INTERSECTIONS
-- -----------------------------------------------------------------------------
INSERT INTO public.intersections (id, name, latitude, longitude, allowed_radius, city, current_status, status) VALUES
('11111111-1111-4111-a111-111111111111', 'Isgoyska KM4', 2.0335, 45.3190, 400, 'Muqdisho', 'Saxmad badan', 'active'),
('22222222-2222-4222-a222-222222222222', 'Isgoyska Benaadir', 2.0420, 45.3120, 450, 'Muqdisho', 'Dhexdhexaad', 'active'),
('33333333-3333-4333-a333-333333333333', 'Isgoyska Tarabuunka', 2.0480, 45.3080, 400, 'Muqdisho', 'Furan', 'active'),
('44444444-4444-4444-a444-444444444444', 'Isgoyska Zoobe (KM5)', 2.0290, 45.3140, 350, 'Muqdisho', 'Mashquul', 'active'),
('55555555-5555-4555-a555-555555555555', 'Isgoyska Dabka', 2.0380, 45.3280, 350, 'Muqdisho', 'Saxmad badan', 'active'),
('66666666-6666-4666-a666-666666666666', 'Isgoyska Sayidka', 2.0410, 45.3340, 300, 'Muqdisho', 'Dhexdhexaad', 'active'),
('77777777-7777-4777-a777-777777777777', 'Isgoyska Jubba', 2.0450, 45.3400, 350, 'Muqdisho', 'Furan', 'active'),
('88888888-8888-4888-a888-888888888888', 'Isgoyska Taleex', 2.0370, 45.3170, 300, 'Muqdisho', 'Furan', 'active'),
('99999999-9999-4999-a999-999999999999', 'Isgoyska Sanaa', 2.0520, 45.3460, 400, 'Muqdisho', 'Dhexdhexaad', 'active'),
('aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa', 'Isgoyska Ex-Control Afgooye', 2.0590, 45.2850, 500, 'Muqdisho', 'Furan', 'active')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;

-- -----------------------------------------------------------------------------
-- 11. AUTOMATIC ADMIN ROLE TO PROFILE TRIGGER
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.sync_admin_role_to_profile()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NOT NULL AND NEW.role_key IS NOT NULL THEN
        UPDATE public.profiles
        SET role = NEW.role_key,
            updated_at = NOW()
        WHERE id = NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

DROP TRIGGER IF EXISTS trg_sync_admin_role ON public.admin_users;

CREATE TRIGGER trg_sync_admin_role
AFTER INSERT OR UPDATE OF role_key, user_id ON public.admin_users
FOR EACH ROW
EXECUTE FUNCTION public.sync_admin_role_to_profile();

-- -----------------------------------------------------------------------------
-- 12. HELPER FUNCTIONS & SECURE USERNAME LOOKUP
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.get_auth_user_role(p_user_id UUID)
RETURNS TEXT AS $$
DECLARE
    v_role TEXT;
BEGIN
    IF p_user_id IS NULL THEN RETURN NULL; END IF;

    SELECT role_key INTO v_role
    FROM public.admin_users
    WHERE (user_id = p_user_id OR id = p_user_id) AND status = 'active'
    LIMIT 1;

    IF v_role IS NOT NULL THEN RETURN v_role; END IF;

    SELECT role INTO v_role
    FROM public.profiles
    WHERE id = p_user_id AND status = 'active'
    LIMIT 1;

    RETURN v_role;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.check_user_permission(p_user_id UUID, p_perm TEXT)
RETURNS BOOLEAN AS $$
DECLARE
    v_role TEXT;
    v_exists BOOLEAN;
BEGIN
    IF p_user_id IS NULL THEN RETURN FALSE; END IF;

    v_role := public.get_auth_user_role(p_user_id);
    IF v_role IS NULL THEN RETURN FALSE; END IF;
    IF v_role = 'super_admin' THEN RETURN TRUE; END IF;

    SELECT EXISTS (
        SELECT 1 FROM public.role_permissions
        WHERE role_key = v_role AND permission_key = p_perm
    ) INTO v_exists;

    RETURN v_exists;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

CREATE OR REPLACE FUNCTION public.get_admin_email_by_username(p_username TEXT)
RETURNS TABLE (email TEXT, status TEXT) AS $$
BEGIN
    RETURN QUERY
    SELECT a.email, a.status
    FROM public.admin_users a
    WHERE LOWER(a.username) = LOWER(p_username) AND a.status = 'active'
    LIMIT 1;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- -----------------------------------------------------------------------------
-- 13. PERMISSIONS & SCHEMA GRANTS FOR SEAMLESS CONNECTIVITY
-- -----------------------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.get_admin_email_by_username(TEXT) TO anon, authenticated;

-- -----------------------------------------------------------------------------
-- 14. REALTIME PUBLICATION ENABLEMENT
-- -----------------------------------------------------------------------------
DO $$
BEGIN
  BEGIN
    ALTER PUBLICATION supabase_realtime ADD TABLE 
      public.profiles, 
      public.subscriptions, 
      public.payment_transactions, 
      public.intersections, 
      public.traffic_reports, 
      public.bajaj_rides;
  EXCEPTION
    WHEN duplicate_object THEN NULL;
    WHEN OTHERS THEN NULL;
  END;
END $$;

COMMIT;

SELECT 'GUUL! Fresh Master Supabase Setup & Realtime Configuration successfully created!' as status;
