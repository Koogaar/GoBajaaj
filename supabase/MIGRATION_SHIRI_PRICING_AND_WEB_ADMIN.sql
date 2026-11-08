-- ============================================================================
-- SHIRI DELIVERY & SUPER APP — COMPREHENSIVE WEB ADMIN & DYNAMIC PRICING MIGRATION
-- Run this script in the Supabase SQL Editor.
-- This allows you to manage ALL pricing, commissions, categories, merchants, 
-- and couriers from the Web Admin WITHOUT releasing Play Store updates!
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. DYNAMIC PLATFORM PRICING & COMMISSION SETTINGS (SINGLETON ROW)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS platform_pricing_settings (
  id VARCHAR(50) PRIMARY KEY DEFAULT 'shiri_global_pricing',
  -- Darawal Bajaaj Pass Pricing
  driver_daily_pass_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.25,
  driver_weekly_pass_usd NUMERIC(10, 2) NOT NULL DEFAULT 1.50,
  driver_monthly_pass_usd NUMERIC(10, 2) NOT NULL DEFAULT 5.00,
  driver_grace_period_hours INT NOT NULL DEFAULT 2,
  
  -- Mooto Delivery Pricing
  courier_base_delivery_fee_usd NUMERIC(10, 2) NOT NULL DEFAULT 2.00,
  courier_per_km_fee_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.30,
  courier_share_percentage NUMERIC(5, 2) NOT NULL DEFAULT 80.00, -- 80% to driver, 20% to SHIRI
  service_fee_usd NUMERIC(10, 2) NOT NULL DEFAULT 0.50,
  night_surge_multiplier NUMERIC(3, 2) NOT NULL DEFAULT 1.25, -- 25% extra during night hours
  
  -- Ganacsatada (Merchant) Pricing
  merchant_default_commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 15.00, -- 15%
  merchant_min_order_usd NUMERIC(10, 2) NOT NULL DEFAULT 3.00,
  
  -- Metadata
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  updated_by VARCHAR(100) DEFAULT 'Super Admin'
);

-- Seed default pricing row if not exists
INSERT INTO platform_pricing_settings (
  id,
  driver_daily_pass_usd,
  driver_weekly_pass_usd,
  driver_monthly_pass_usd,
  driver_grace_period_hours,
  courier_base_delivery_fee_usd,
  courier_per_km_fee_usd,
  courier_share_percentage,
  service_fee_usd,
  night_surge_multiplier,
  merchant_default_commission_rate,
  merchant_min_order_usd,
  updated_at,
  updated_by
) VALUES (
  'shiri_global_pricing',
  0.25,
  1.50,
  5.00,
  2,
  2.00,
  0.30,
  80.00,
  0.50,
  1.25,
  15.00,
  3.00,
  NOW(),
  'System Init'
) ON CONFLICT (id) DO UPDATE SET
  updated_at = NOW();

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. DYNAMIC BUSINESS CATEGORIES & SUBCATEGORIES TABLE
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS dynamic_business_categories (
  id VARCHAR(60) PRIMARY KEY,
  name_so VARCHAR(120) NOT NULL,
  name_en VARCHAR(120) NOT NULL,
  emoji VARCHAR(20) NOT NULL,
  banner_image_url TEXT,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  subcategories JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed Default Somali Categories
INSERT INTO dynamic_business_categories (id, name_so, name_en, emoji, banner_image_url, is_active, sort_order, subcategories)
VALUES
  (
    'cunto',
    'Cunto & Maqaayado',
    'Food & Restaurants',
    '🍔',
    'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80',
    true,
    1,
    '[
      {"id": "pizza", "nameSo": "Pizza & Italian", "nameEn": "Pizza & Italian", "emoji": "🍕"},
      {"id": "fast_food", "nameSo": "Fast Food & Burgers", "nameEn": "Fast Food & Burgers", "emoji": "🍔"},
      {"id": "traditional", "nameSo": "Cunto Dhaqameed", "nameEn": "Traditional Somali", "emoji": "🍛"},
      {"id": "seafood", "nameSo": "Kalluun & Badda", "nameEn": "Seafood", "emoji": "🐟"},
      {"id": "bakery", "nameSo": "Rooti & Macmacaan", "nameEn": "Bakeries & Sweets", "emoji": "🥐"},
      {"id": "cafe", "nameSo": "Bun & Shaah", "nameEn": "Cafes & Coffee", "emoji": "☕"}
    ]'::jsonb
  ),
  (
    'supermarket',
    'Supermarket & Dukaamo',
    'Supermarket & Grocery',
    '🛒',
    'https://images.unsplash.com/photo-1542838132-92c53300491e?w=500&auto=format&fit=crop&q=80',
    true,
    2,
    '[
      {"id": "supermarkets", "nameSo": "Supermarkets Waaweyn", "nameEn": "Hypermarkets", "emoji": "🏪"},
      {"id": "groceries", "nameSo": "Raashinka Guud", "nameEn": "Pantry & Groceries", "emoji": "🍚"},
      {"id": "mini_market", "nameSo": "Mini Markets", "nameEn": "Convenience Stores", "emoji": "🏬"},
      {"id": "wholesale", "nameSo": "Jumlo & Bakaaro", "nameEn": "Wholesale Markets", "emoji": "📦"}
    ]'::jsonb
  ),
  (
    'daawo',
    'Daawo & Farmashiyo',
    'Pharmacy & Healthcare',
    '💊',
    'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=80',
    true,
    3,
    '[
      {"id": "medicine", "nameSo": "Daawooyinka Guud", "nameEn": "Prescription & OTC", "emoji": "🩹"},
      {"id": "skincare", "nameSo": "Daryeelka Maqaarka", "nameEn": "Skincare & Body", "emoji": "🧴"},
      {"id": "medical_devices", "nameSo": "Qalabka Caafimaadka", "nameEn": "Medical Devices", "emoji": "🩺"}
    ]'::jsonb
  ),
  (
    'hilib_qudaar',
    'Hilib, Qudaar & Kalluun',
    'Meat, Veggies & Fish',
    '🥩',
    'https://images.unsplash.com/photo-1607623814075-e51df1bdc82f?w=500&auto=format&fit=crop&q=80',
    true,
    4,
    '[
      {"id": "meat", "nameSo": "Hilib Geel, Ari & Lo''", "nameEn": "Fresh Meat", "emoji": "🥩"},
      {"id": "poultry", "nameSo": "Digaag & Beed", "nameEn": "Chicken & Eggs", "emoji": "🍗"},
      {"id": "veggies", "nameSo": "Qudaarta Beerta", "nameEn": "Fresh Vegetables", "emoji": "🥦"},
      {"id": "milk_butter", "nameSo": "Caano Geel & Subag", "nameEn": "Milk & Ghee", "emoji": "🥛"}
    ]'::jsonb
  ),
  (
    'electronics',
    'Taleefanno & Electronics',
    'Phones & Tech',
    '📱',
    'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500&auto=format&fit=crop&q=80',
    true,
    5,
    '[
      {"id": "smartphones", "nameSo": "Taleefannada", "nameEn": "Smartphones", "emoji": "📲"},
      {"id": "accessories", "nameSo": "Chargers & Covers", "nameEn": "Accessories", "emoji": "🔌"},
      {"id": "computers", "nameSo": "Kombuyuutarro", "nameEn": "Computers", "emoji": "💻"}
    ]'::jsonb
  ),
  (
    'fashion',
    'Dharka, Fashion & Tolista',
    'Clothing & Tailoring',
    '👗',
    'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?w=500&auto=format&fit=crop&q=80',
    true,
    6,
    '[
      {"id": "tailoring", "nameSo": "Harqaan & Tolista", "nameEn": "Tailoring", "emoji": "🪡"},
      {"id": "women", "nameSo": "Dirac & Googarad", "nameEn": "Women''s Wear", "emoji": "💃"},
      {"id": "men", "nameSo": "Khamiisyo & Macawiis", "nameEn": "Men''s Wear", "emoji": "👔"},
      {"id": "abaya", "nameSo": "Cabayado & Xijaab", "nameEn": "Abaya & Hijab", "emoji": "🧕"}
    ]'::jsonb
  ),
  (
    'stationery',
    'Stationery & Buugaag',
    'Stationery & Books',
    '📚',
    'https://images.unsplash.com/photo-1507842229451-79b1be886a20?w=500&auto=format&fit=crop&q=80',
    true,
    7,
    '[
      {"id": "school", "nameSo": "Daftarro & Buugaag", "nameEn": "School Books", "emoji": "✏️"},
      {"id": "printing", "nameSo": "Daabacaad & Koobi", "nameEn": "Printing", "emoji": "🖨️"}
    ]'::jsonb
  ),
  (
    'home_services',
    'Adeegyada Guriga & Farsamada',
    'Home Services',
    '🔧',
    'https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=500&auto=format&fit=crop&q=80',
    true,
    8,
    '[
      {"id": "electrician", "nameSo": "Koronto Yaqaan", "nameEn": "Electricians", "emoji": "💡"},
      {"id": "plumbing", "nameSo": "Tuubiste & Biyo", "nameEn": "Plumbers", "emoji": "🚰"},
      {"id": "ac_repair", "nameSo": "AC & Qaboojiye", "nameEn": "AC Repair", "emoji": "❄️"}
    ]'::jsonb
  ),
  (
    'perfume',
    'Barafuun, Uunsi & Qurxin',
    'Perfume & Cosmetics',
    '✨',
    'https://images.unsplash.com/photo-1592945403244-b3fbafd7f539?w=500&auto=format&fit=crop&q=80',
    true,
    9,
    '[
      {"id": "oud", "nameSo": "Uunsi & Cuud", "nameEn": "Bukhoor & Oud", "emoji": "🪵"},
      {"id": "fragrance", "nameSo": "Barafuunno", "nameEn": "Fragrances", "emoji": "🧴"}
    ]'::jsonb
  ),
  (
    'hardware',
    'Qalabka Guryaha & Dhismaha',
    'Building & Hardware',
    '🏠',
    'https://images.unsplash.com/photo-1504307651254-35680f356dfd?w=500&auto=format&fit=crop&q=80',
    true,
    10,
    '[
      {"id": "building", "nameSo": "Qalabka Dhismaha", "nameEn": "Building Materials", "emoji": "🧱"},
      {"id": "tools", "nameSo": "Qalabka Farsamada", "nameEn": "Tools", "emoji": "🔨"}
    ]'::jsonb
  )
ON CONFLICT (id) DO UPDATE SET
  name_so = EXCLUDED.name_so,
  name_en = EXCLUDED.name_en,
  emoji = EXCLUDED.emoji,
  subcategories = EXCLUDED.subcategories,
  updated_at = NOW();

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. ENSURE MERCHANTS TABLE HAS COMMISSION & VERIFICATION FIELDS
-- ─────────────────────────────────────────────────────────────────────────────
DO $$ BEGIN
  ALTER TABLE merchants ADD COLUMN IF NOT EXISTS commission_rate NUMERIC(5, 2) DEFAULT 15.00;
  ALTER TABLE merchants ADD COLUMN IF NOT EXISTS verification_status VARCHAR(30) DEFAULT 'verified';
  ALTER TABLE merchants ADD COLUMN IF NOT EXISTS is_open BOOLEAN DEFAULT TRUE;
  ALTER TABLE merchants ADD COLUMN IF NOT EXISTS min_order NUMERIC(10, 2) DEFAULT 3.00;
  ALTER TABLE merchants ADD COLUMN IF NOT EXISTS delivery_fee NUMERIC(10, 2) DEFAULT 2.00;
EXCEPTION
  WHEN undefined_table THEN null;
END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. ROW LEVEL SECURITY (RLS) FOR PRICING & CATEGORIES
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE platform_pricing_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE dynamic_business_categories ENABLE ROW LEVEL SECURITY;

-- Anyone (public / mobile apps) can READ pricing & categories without authentication
DROP POLICY IF EXISTS "Public read pricing" ON platform_pricing_settings;
CREATE POLICY "Public read pricing" ON platform_pricing_settings FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read categories" ON dynamic_business_categories;
CREATE POLICY "Public read categories" ON dynamic_business_categories FOR SELECT USING (true);

-- Admins can UPDATE pricing
DROP POLICY IF EXISTS "Admin manage pricing" ON platform_pricing_settings;
CREATE POLICY "Admin manage pricing" ON platform_pricing_settings FOR ALL USING (true);

-- Admins can INSERT, UPDATE, DELETE categories
DROP POLICY IF EXISTS "Admin manage categories" ON dynamic_business_categories;
CREATE POLICY "Admin manage categories" ON dynamic_business_categories FOR ALL USING (true);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. FUNCTION TO GET CURRENT PRICING CONFIG (SAFE RPC)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION get_current_platform_pricing()
RETURNS JSONB
LANGUAGE sql
SECURITY DEFINER
AS $$
  SELECT to_jsonb(p) FROM platform_pricing_settings p WHERE id = 'shiri_global_pricing' LIMIT 1;
$$;

