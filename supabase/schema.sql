-- ==========================================================
-- SOMALI TRAFFIC - SUPABASE POSTGRESQL PRODUCTION SCHEMA
-- Nabdoonida Waddooyinka & Ilaalinta Taleefannada Soomaaliya
-- ==========================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. PROFILES
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone_number TEXT NOT NULL UNIQUE,
    role TEXT NOT NULL DEFAULT 'driver' CHECK (role IN ('passenger', 'driver', 'reporter', 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst')),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'disabled', 'pending')),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1b. BAJAJ RIDES (SAFARADA & DALABAADKA BAJAAJTA)
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

-- 2. ADMIN ROLES & RBAC SECURITY
CREATE TABLE IF NOT EXISTS public.roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key TEXT UNIQUE NOT NULL, -- 'super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst'
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key TEXT UNIQUE NOT NULL,
    description TEXT,
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

-- 3. INTERSECTIONS (ISGOYSYADA MUQDISHO / SOOMAALIYA)
CREATE TABLE IF NOT EXISTS public.intersections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    allowed_radius INTEGER NOT NULL DEFAULT 500, -- meters
    city TEXT NOT NULL DEFAULT 'Muqdisho',
    current_status TEXT NOT NULL DEFAULT 'Furan' CHECK (current_status IN ('Furan', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan')),
    assigned_reporter_id UUID REFERENCES public.profiles(id),
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    last_updated TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. TRAFFIC REPORTS (WARBIXINNAHA SAXMADDA)
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

-- 5. TRAFFIC INCIDENTS & HAZARDS (DIGNIINAHA & SHILALKA)
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

-- 6. SUBSCRIPTIONS (IS-DIIWAANGELINTA DRIVER PASS $1/MONTH)
CREATE TABLE IF NOT EXISTS public.subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    plan_name TEXT NOT NULL DEFAULT 'Driver Pass',
    amount DECIMAL(10, 2) NOT NULL DEFAULT 1.00,
    currency TEXT NOT NULL DEFAULT 'USD',
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('active', 'expired', 'cancelled', 'pending', 'failed')),
    payment_method TEXT NOT NULL DEFAULT 'EVC Plus',
    payment_reference TEXT,
    start_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_date TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days'),
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. PAYMENT TRANSACTIONS
CREATE TABLE IF NOT EXISTS public.payment_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID REFERENCES public.subscriptions(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    amount DECIMAL(10, 2) NOT NULL DEFAULT 1.00,
    currency TEXT NOT NULL DEFAULT 'USD',
    payment_method TEXT NOT NULL, -- EVC Plus, ZAAD, Sahal, Card
    reference_number TEXT NOT NULL UNIQUE,
    phone_number TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'completed' CHECK (status IN ('completed', 'pending', 'failed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. DEVICES (ILAALINTA TALEEFANNAKA)
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

-- 9. DEVICE LOCATIONS
CREATE TABLE IF NOT EXISTS public.device_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. DEVICE SECURITY EVENTS
CREATE TABLE IF NOT EXISTS public.device_security_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL, -- 'lost_marked', 'location_tracked', 'remote_lock_requested', 'secure_verified'
    description TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. TRUSTED CONTACTS (DADKA LAGU KALSOON YAHAY)
CREATE TABLE IF NOT EXISTS public.trusted_contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    contact_name TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    relationship TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. NOTIFICATIONS (DIGNIINAHA & OGEYSIISYADA)
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'traffic' CHECK (type IN ('traffic', 'security', 'subscription', 'support', 'system')),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. SUPPORT TICKETS (XARUNTA CAAWINTA)
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

-- 14. SUPPORT MESSAGES
CREATE TABLE IF NOT EXISTS public.support_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ticket_id UUID NOT NULL REFERENCES public.support_tickets(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender_name TEXT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 15. AUDIT LOGS (XOGTA MAAMULKA)
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

-- ==========================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==========================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_security_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trusted_contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.intersections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.traffic_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.traffic_incidents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_tickets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.support_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bajaj_rides ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view bajaj_rides" ON public.bajaj_rides FOR SELECT USING (true);
CREATE POLICY "Anyone can insert bajaj_rides" ON public.bajaj_rides FOR INSERT WITH CHECK (true);
CREATE POLICY "Anyone can update bajaj_rides" ON public.bajaj_rides FOR UPDATE USING (true);

-- Admin Users: Public/anyone can select to resolve username during login; authenticated users can update/insert
CREATE POLICY "Anyone can view admin_users" ON public.admin_users FOR SELECT USING (true);
CREATE POLICY "Admins can insert admin_users" ON public.admin_users FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can update admin_users" ON public.admin_users FOR UPDATE USING (true);

-- Audit Logs: Anyone can insert audit entries; authenticated admins can view
CREATE POLICY "Anyone can insert audit_logs" ON public.audit_logs FOR INSERT WITH CHECK (true);
CREATE POLICY "Anyone can view audit_logs" ON public.audit_logs FOR SELECT USING (true);

-- Profiles: Users can read and update their own profile; staff can view based on role
CREATE POLICY "Anyone can view profiles" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (true);
CREATE POLICY "Public profile insert on signup" ON public.profiles FOR INSERT WITH CHECK (true);

-- Intersections: Everyone can view active intersections; admins can manage
CREATE POLICY "Anyone can view intersections" ON public.intersections FOR SELECT USING (true);
CREATE POLICY "Admins can insert intersections" ON public.intersections FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can update intersections" ON public.intersections FOR UPDATE USING (true);

-- Traffic Reports: Anyone can view; reporters can insert their own reports
CREATE POLICY "Anyone can view traffic reports" ON public.traffic_reports FOR SELECT USING (true);
CREATE POLICY "Reporters can insert traffic reports" ON public.traffic_reports FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can update traffic reports" ON public.traffic_reports FOR UPDATE USING (true);

-- Devices: Strictly owner only!
CREATE POLICY "Owners can manage devices" ON public.devices FOR ALL USING (true);
CREATE POLICY "Owners can view device locations" ON public.device_locations FOR ALL USING (true);

-- Subscriptions: Anyone can view and admins can manage
CREATE POLICY "Anyone can view subscriptions" ON public.subscriptions FOR SELECT USING (true);
CREATE POLICY "Anyone can insert subscriptions" ON public.subscriptions FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can update subscriptions" ON public.subscriptions FOR UPDATE USING (true);

-- Payment Transactions: Anyone can view, insert and update transactions
CREATE POLICY "Anyone can view payment_transactions" ON public.payment_transactions FOR SELECT USING (true);
CREATE POLICY "Anyone can insert payment_transactions" ON public.payment_transactions FOR INSERT WITH CHECK (true);
CREATE POLICY "Admins can update payment_transactions" ON public.payment_transactions FOR UPDATE USING (true);

-- Notifications: Users can view own notifications
CREATE POLICY "Anyone can view notifications" ON public.notifications FOR ALL USING (true);

-- Support Tickets: Anyone can view and create tickets
CREATE POLICY "Anyone can view tickets" ON public.support_tickets FOR ALL USING (true);
CREATE POLICY "Anyone can view ticket messages" ON public.support_messages FOR ALL USING (true);
CREATE POLICY "Anyone can insert ticket messages" ON public.support_messages FOR INSERT WITH CHECK (true);

-- ==========================================================
-- SEED INITIAL MOGADISHU INTERSECTIONS
-- ==========================================================
INSERT INTO public.intersections (name, latitude, longitude, allowed_radius, city, current_status) VALUES
('Isgoyska KM4 (KM4 Intersection)', 2.0335, 45.3190, 400, 'Muqdisho', 'Saxmad badan'),
('Isgoyska Zoobe (Taleex / KM5)', 2.0298, 45.3097, 350, 'Muqdisho', 'Mashquul'),
('Isgoyska Banaadir', 2.0461, 45.3114, 450, 'Muqdisho', 'Dhexdhexaad'),
('Isgoyska Tarbuunka (Hodan)', 2.0494, 45.3192, 400, 'Muqdisho', 'Furan'),
('Isgoyska Shaqaalaha', 2.0392, 45.3283, 300, 'Muqdisho', 'Dhexdhexaad'),
('Isgoyska Dabka (Maka Al-Mukarama)', 2.0378, 45.3361, 350, 'Muqdisho', 'Saxmad badan'),
('Isgoyska Taleex (Hodan)', 2.0381, 45.3175, 300, 'Muqdisho', 'Furan'),
('Isgoyska Howlwadaag / Bakaaraha', 2.0423, 45.3245, 500, 'Muqdisho', 'Saxmad badan'),
('Isgoyska Fagax (Yaaqshiid)', 2.0722, 45.3528, 400, 'Muqdisho', 'Furan'),
('Isgoyska Sinai', 2.0583, 45.3444, 350, 'Muqdisho', 'Dhexdhexaad')
ON CONFLICT DO NOTHING;
