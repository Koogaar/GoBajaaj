-- ====================================================================
-- GoBajaaj Phone Guard - Database Schema & Migration
-- Supports Android & Future iOS Mobility & Device Protection
-- ====================================================================

-- 1. Ensure extensions exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Enhance or create devices table
CREATE TABLE IF NOT EXISTS public.devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    device_model TEXT NOT NULL,
    platform TEXT NOT NULL DEFAULT 'Android' CHECK (platform IN ('Android', 'iOS', 'Web')),
    app_version TEXT NOT NULL DEFAULT '1.0.0',
    device_fingerprint TEXT NOT NULL DEFAULT '',
    app_installation_id TEXT,
    push_token TEXT,
    is_protected BOOLEAN NOT NULL DEFAULT true,
    is_lost BOOLEAN NOT NULL DEFAULT false,
    lost_at TIMESTAMPTZ,
    found_at TIMESTAMPTZ,
    battery_level INTEGER DEFAULT 85,
    network_status TEXT DEFAULT 'Online', -- 'WiFi', 'Cellular', 'Offline'
    location_permission_status TEXT DEFAULT 'Granted', -- 'Granted', 'Coarse', 'Denied'
    is_online BOOLEAN NOT NULL DEFAULT true,
    last_latitude DOUBLE PRECISION DEFAULT 2.0335,
    last_longitude DOUBLE PRECISION DEFAULT 45.3190,
    last_accuracy DOUBLE PRECISION DEFAULT 10.0,
    last_location_address TEXT DEFAULT 'KM4, Mogadishu',
    last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for quick user device lookups
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON public.devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_is_lost ON public.devices(is_lost);
CREATE INDEX IF NOT EXISTS idx_devices_platform ON public.devices(platform);

-- 3. Device Locations History (Breadcrumbs & Find Now traces)
CREATE TABLE IF NOT EXISTS public.device_locations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    source TEXT DEFAULT 'GPS', -- 'GPS', 'Network', 'Cell'
    address TEXT,
    battery_level INTEGER,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_device_locations_device_id ON public.device_locations(device_id);
CREATE INDEX IF NOT EXISTS idx_device_locations_recorded_at ON public.device_locations(recorded_at DESC);

-- 4. Device Recovery & Security Events Audit Trail
CREATE TABLE IF NOT EXISTS public.device_recovery_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id UUID NOT NULL REFERENCES public.devices(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    event_type TEXT NOT NULL, -- 'PROTECTION_ENABLED', 'PROTECTION_DISABLED', 'MARKED_LOST', 'MARKED_FOUND', 'FIND_NOW_REQUESTED', 'ALARM_TRIGGERED'
    description TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_device_events_device_id ON public.device_recovery_events(device_id);

-- 5. Row-Level Security (RLS)
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_locations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_recovery_events ENABLE ROW LEVEL SECURITY;

-- Devices RLS: Users can only manage their own devices; Admins can view/manage all
DROP POLICY IF EXISTS "Users can view and manage their own devices" ON public.devices;
CREATE POLICY "Users can view and manage their own devices"
    ON public.devices
    FOR ALL
    USING (
        auth.uid() = user_id 
        OR EXISTS (
            SELECT 1 FROM public.profiles 
            WHERE profiles.id = auth.uid() 
            AND profiles.role IN ('super_admin', 'support_agent', 'admin')
        )
    );

-- Device Locations RLS: Only device owners or admins can view location history
DROP POLICY IF EXISTS "Users can view their device location history" ON public.device_locations;
CREATE POLICY "Users can view their device location history"
    ON public.device_locations
    FOR ALL
    USING (
        EXISTS (
            SELECT 1 FROM public.devices 
            WHERE devices.id = device_locations.device_id 
            AND (devices.user_id = auth.uid() OR EXISTS (
                SELECT 1 FROM public.profiles 
                WHERE profiles.id = auth.uid() 
                AND profiles.role IN ('super_admin', 'support_agent', 'admin')
            ))
        )
    );

-- Device Recovery Events RLS
DROP POLICY IF EXISTS "Users can view their device events" ON public.device_recovery_events;
CREATE POLICY "Users can view their device events"
    ON public.device_recovery_events
    FOR ALL
    USING (
        user_id = auth.uid() 
        OR EXISTS (
            SELECT 1 FROM public.profiles 
            WHERE profiles.id = auth.uid() 
            AND profiles.role IN ('super_admin', 'support_agent', 'admin')
        )
    );
