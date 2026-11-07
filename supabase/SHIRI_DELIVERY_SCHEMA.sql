-- ============================================================================
-- SHIRI DELIVERY — PRODUCTION POSTGRESQL / SUPABASE DATABASE SCHEMA (IDEMPOTENT & SAFE)
-- Market: Mogadishu, Somalia & International Expansion
-- Includes Safe Type Checks, RLS, Triggers, Spatial Geometry & Telecom Payments
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- 1. ENUMS (Safe creation - will not fail if already exists)
DO $$ BEGIN
  CREATE TYPE user_role_type AS ENUM ('customer', 'merchant_owner', 'merchant_staff', 'courier', 'admin', 'dispatcher');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
  CREATE TYPE order_status_type AS ENUM (
    'pending', 'accepted', 'preparing', 'ready_for_pickup',
    'courier_assigned', 'picked_up', 'in_transit', 'delivered',
    'cancelled', 'failed_delivery'
  );
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
  CREATE TYPE payment_method_type AS ENUM ('evc_plus', 'zaad', 'edahab', 'sahal', 'cash_on_delivery');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
  CREATE TYPE payment_status_type AS ENUM ('pending', 'authorized', 'paid', 'failed', 'refunded');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

DO $$ BEGIN
  CREATE TYPE vehicle_type_enum AS ENUM ('motorcycle', 'bajaj', 'car', 'bicycle');
EXCEPTION
  WHEN duplicate_object THEN null;
END $$;

-- 2. USER PROFILES & ROLES
CREATE TABLE IF NOT EXISTS profiles (
  id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  phone_number VARCHAR(20) NOT NULL UNIQUE,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(255),
  role user_role_type NOT NULL DEFAULT 'customer',
  avatar_url TEXT,
  preferred_language VARCHAR(5) DEFAULT 'so', -- 'so' or 'en'
  is_verified BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. SHIRI ADDRESS (GURI NUMBER DIGITAL AH)
CREATE TABLE IF NOT EXISTS shiri_addresses (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  address_code VARCHAR(32) NOT NULL UNIQUE, -- e.g. "SHR-MG-48291"
  owner_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
  label VARCHAR(50) NOT NULL DEFAULT 'Home', -- 'Home', 'Work', 'Family', 'Store'
  city VARCHAR(80) NOT NULL DEFAULT 'Mogadishu',
  district VARCHAR(80) NOT NULL, -- 'Hodan', 'Waberi', 'Hamarweyne', 'Wadajir', etc.
  neighborhood VARCHAR(120) NOT NULL, -- 'Digfeer', 'KM4', 'Taleex'
  street VARCHAR(120),
  building_or_floor VARCHAR(120),
  landmark TEXT NOT NULL, -- 'Agta Masaajidka Digfeer'
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  geom GEOMETRY(Point, 4326),
  accuracy_meters DOUBLE PRECISION DEFAULT 8.0,
  is_verified BOOLEAN DEFAULT TRUE,
  delivery_notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shiri_addresses_code ON shiri_addresses(address_code);
CREATE INDEX IF NOT EXISTS idx_shiri_addresses_owner ON shiri_addresses(owner_id);
CREATE INDEX IF NOT EXISTS idx_shiri_addresses_geom ON shiri_addresses USING GIST(geom);

-- 4. MERCHANTS & STORES
CREATE TABLE IF NOT EXISTS merchants (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  owner_id UUID NOT NULL REFERENCES profiles(id),
  name VARCHAR(150) NOT NULL,
  slogan TEXT,
  category VARCHAR(80) NOT NULL,
  logo_url TEXT,
  banner_url TEXT,
  phone VARCHAR(20) NOT NULL,
  email VARCHAR(255),
  rating NUMERIC(3, 2) DEFAULT 5.0,
  review_count INT DEFAULT 0,
  delivery_time_minutes INT DEFAULT 25,
  min_order NUMERIC(10, 2) DEFAULT 5.00,
  delivery_fee NUMERIC(10, 2) DEFAULT 2.00,
  is_open BOOLEAN DEFAULT TRUE,
  shiri_address_code VARCHAR(32) REFERENCES shiri_addresses(address_code),
  district VARCHAR(80) NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  geom GEOMETRY(Point, 4326),
  verification_status VARCHAR(20) DEFAULT 'verified',
  commission_rate NUMERIC(4, 2) DEFAULT 0.15, -- 15%
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. PRODUCTS & CATALOG
CREATE TABLE IF NOT EXISTS products (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
  name VARCHAR(150) NOT NULL,
  name_somali VARCHAR(150) NOT NULL,
  description TEXT,
  description_somali TEXT,
  price NUMERIC(10, 2) NOT NULL,
  image_url TEXT,
  category VARCHAR(80) NOT NULL,
  in_stock BOOLEAN DEFAULT TRUE,
  stock_quantity INT DEFAULT 100,
  prep_time_minutes INT DEFAULT 15,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. COURIER PROFILES & FLEET
CREATE TABLE IF NOT EXISTS courier_profiles (
  id UUID PRIMARY KEY REFERENCES profiles(id) ON DELETE CASCADE,
  vehicle_type vehicle_type_enum NOT NULL DEFAULT 'motorcycle',
  plate_number VARCHAR(30) NOT NULL,
  driver_license_number VARCHAR(50),
  is_online BOOLEAN DEFAULT FALSE,
  current_latitude DOUBLE PRECISION,
  current_longitude DOUBLE PRECISION,
  current_geom GEOMETRY(Point, 4326),
  today_earnings NUMERIC(10, 2) DEFAULT 0.00,
  completed_trips INT DEFAULT 0,
  rating NUMERIC(3, 2) DEFAULT 5.0,
  verification_status VARCHAR(20) DEFAULT 'verified',
  cash_collected_today NUMERIC(10, 2) DEFAULT 0.00,
  last_location_update TIMESTAMPTZ DEFAULT NOW()
);

-- 7. ORDERS & DISPATCH
CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_number VARCHAR(30) NOT NULL UNIQUE, -- e.g. '#SHR-2026-000123'
  customer_id UUID NOT NULL REFERENCES profiles(id),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  courier_id UUID REFERENCES courier_profiles(id),
  delivery_address_id UUID NOT NULL REFERENCES shiri_addresses(id),
  subtotal NUMERIC(10, 2) NOT NULL,
  delivery_fee NUMERIC(10, 2) NOT NULL,
  service_fee NUMERIC(10, 2) NOT NULL DEFAULT 0.50,
  discount NUMERIC(10, 2) DEFAULT 0.00,
  total NUMERIC(10, 2) NOT NULL,
  status order_status_type NOT NULL DEFAULT 'pending',
  payment_method payment_method_type NOT NULL,
  payment_status payment_status_type NOT NULL DEFAULT 'pending',
  payment_reference VARCHAR(100),
  delivery_instructions TEXT,
  handover_pin VARCHAR(6) NOT NULL, -- 4-digit code e.g. '4792'
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 8. ORDER ITEMS
CREATE TABLE IF NOT EXISTS order_items (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id UUID NOT NULL REFERENCES products(id),
  product_name VARCHAR(150) NOT NULL,
  price NUMERIC(10, 2) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  selected_options JSONB,
  total_price NUMERIC(10, 2) NOT NULL
);

-- 9. ORDER STATUS AUDIT TRAIL
CREATE TABLE IF NOT EXISTS order_status_history (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  status order_status_type NOT NULL,
  actor VARCHAR(100) NOT NULL,
  note TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 10. TELECOM & MOBILE MONEY TRANSACTIONS
CREATE TABLE IF NOT EXISTS telecom_transactions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  order_id UUID NOT NULL REFERENCES orders(id),
  provider payment_method_type NOT NULL,
  payer_phone VARCHAR(20) NOT NULL,
  amount NUMERIC(10, 2) NOT NULL,
  currency VARCHAR(5) DEFAULT 'USD',
  status payment_status_type NOT NULL,
  provider_reference VARCHAR(100) NOT NULL,
  idempotency_key VARCHAR(150) NOT NULL UNIQUE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  verified_at TIMESTAMPTZ
);

-- 11. AUDIT LOGS
CREATE TABLE IF NOT EXISTS system_audit_logs (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  actor VARCHAR(100) NOT NULL,
  action VARCHAR(100) NOT NULL,
  details TEXT,
  ip_address INET,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- ROW LEVEL SECURITY (RLS) POLICIES (Safe drop & recreate)
-- ----------------------------------------------------------------------------
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE shiri_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;
ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE courier_profiles ENABLE ROW LEVEL SECURITY;

-- Profiles & Addresses
DROP POLICY IF EXISTS "Users read own profile" ON profiles;
CREATE POLICY "Users read own profile" ON profiles FOR SELECT USING (auth.uid() = id);

DROP POLICY IF EXISTS "Users manage own addresses" ON shiri_addresses;
CREATE POLICY "Users manage own addresses" ON shiri_addresses FOR ALL USING (auth.uid() = owner_id);

-- Merchants & Products
DROP POLICY IF EXISTS "Public read merchants" ON merchants;
CREATE POLICY "Public read merchants" ON merchants FOR SELECT USING (is_open = true OR verification_status = 'verified');

DROP POLICY IF EXISTS "Public read products" ON products;
CREATE POLICY "Public read products" ON products FOR SELECT USING (in_stock = true);

DROP POLICY IF EXISTS "Merchants manage products" ON products;
CREATE POLICY "Merchants manage products" ON products FOR ALL USING (
  EXISTS (SELECT 1 FROM merchants WHERE merchants.id = products.merchant_id AND merchants.owner_id = auth.uid())
);

-- Orders
DROP POLICY IF EXISTS "Customer reads own orders" ON orders;
CREATE POLICY "Customer reads own orders" ON orders FOR SELECT USING (auth.uid() = customer_id);

DROP POLICY IF EXISTS "Merchant reads assigned orders" ON orders;
CREATE POLICY "Merchant reads assigned orders" ON orders FOR SELECT USING (
  EXISTS (SELECT 1 FROM merchants WHERE merchants.id = orders.merchant_id AND merchants.owner_id = auth.uid())
);

DROP POLICY IF EXISTS "Courier reads assigned orders" ON orders;
CREATE POLICY "Courier reads assigned orders" ON orders FOR SELECT USING (courier_id = auth.uid());
