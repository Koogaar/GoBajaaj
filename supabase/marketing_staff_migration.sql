-- ============================================================
-- GoBajaaj Marketing Staff SQL Migration
-- Run this in Supabase SQL Editor
-- ============================================================

-- 1. Create marketing_staff table
CREATE TABLE IF NOT EXISTS marketing_staff (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  username TEXT UNIQUE NOT NULL,
  full_name TEXT NOT NULL,
  phone TEXT,
  code TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  status TEXT DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'disabled')),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Add referred_by column to profiles (if not exists)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS referred_by TEXT;

-- 3. Add subscription_status and subscription_expiry columns to profiles (if not exists)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'inactive';
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMPTZ;

-- 4. Enable RLS
ALTER TABLE marketing_staff ENABLE ROW LEVEL SECURITY;

-- 5. Create RLS policies (admin access only)
DROP POLICY IF EXISTS "Admin full access marketing_staff" ON marketing_staff;
CREATE POLICY "Admin full access marketing_staff" ON marketing_staff
  FOR ALL USING (true);

-- 6. Create index on referral code for fast lookups
CREATE INDEX IF NOT EXISTS idx_marketing_staff_code ON marketing_staff(code);
CREATE INDEX IF NOT EXISTS idx_profiles_referred_by ON profiles(referred_by);

-- 7. Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_marketing_staff_updated_at ON marketing_staff;
CREATE TRIGGER update_marketing_staff_updated_at
  BEFORE UPDATE ON marketing_staff
  FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();