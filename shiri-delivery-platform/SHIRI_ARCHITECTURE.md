# SHIRI DELIVERY — Architecture, Deployment & Specifications

> **Brand**: SHIRI DELIVERY  
> **Somali Slogan**: *Ku dhowaa, Ku gaara*  
> **English Brand Message**: *Your City, Our Priority.*  
> **Primary Initial Market**: Mogadishu, Somalia (Expansion: Hargeisa, Garowe, Kismayo, and International)

---

## 1. System Overview & Implemented Applications

The platform delivers four distinct, interconnected application experiences powered by a unified state and database schema:

### 1. Customer Mobile App (`src/components/customer/`)
- **Splash & Onboarding**: Somali & English onboarding flows with delivery scooter illustrations.
- **Authentication**: Somali phone number formatting (`+252 61 XXXXXXX`) with OTP input and resend countdown.
- **Shiri Address (Guri Number Digital Ah)**:
  - Interactive Mogadishu map with draggable red pin down to entrance level.
  - GPS latitude/longitude recording with accuracy radius (±8m).
  - Reverse geocoded fields: District (Hodan, Waberi, Hamarweyne, Wadajir, Dayniile, Yaqshid), Neighborhood, Street, Floor, and Landmark.
  - Unique deterministic identifier generation (e.g. `SHR-MG-48291`).
  - Standalone SVG QR Code rendering for zero-network sharing with drivers and family.
- **Catalog & Orders**:
  - Configurable categories (Food, Groceries, Pharmacy, Fashion, Electronics, Parcels).
  - Merchant store pages with menu options, variants (Small, Medium, Large), and instructions.
  - Real-time shopping cart and authoritative server-side price recalculation.
  - Somalia Telecom checkout: Hormuud EVC Plus, Somtel eDahab, Telesom ZAAD, Golis Sahal, and Cash on Delivery.
- **Order Tracking & Handover PIN**:
  - Live interactive map displaying restaurant pickup, customer dropoff, and moving courier vehicle.
  - Status progression stepper (Placed → Accepted → Preparing → Ready → Dispatched → In Transit → Delivered).
  - Secure 4-digit Handover PIN (`handoverPin`) displayed to the customer to give to the courier upon delivery.

### 2. Merchant Mobile App (`src/components/merchant/`)
- Dedicated Android and iOS application for business owners.
- Store status toggle (Open / Closed) with instant update in customer app.
- Real-time order dispatch alerts with sound and badge counts.
- Order processing pipeline: Accept order, Reject with reason, Mark Preparing, Mark Ready for Pickup, Handover to assigned courier.
- Product & inventory management: Add new product with Somali and English titles, image, category, price, and instant Stock Availability toggle.
- Merchant finances: Gross sales, 15% platform commission calculation, net earnings, and daily automated settlement accounts.

### 3. Courier Mobile App (`src/components/courier/`)
- Dedicated driver mobile application with Online / Offline toggle.
- Incoming dispatch requests with pickup location, customer Shiri Address preview, distance (2.4 km), ETA (12 min), and trip pay ($6.50).
- Turn-by-turn route navigation on Mogadishu map with checkpoints: Arrived at Merchant → Confirm Pickup → Navigate to Customer.
- Delivery Handover PIN Keypad: Courier enters customer's 4-digit PIN for server validation before marking delivery complete.
- Daily earnings tracker and Cash on Delivery (COD) collection reconciliation.

### 4. Admin Web Dashboard (`src/components/admin/`)
- Enterprise operations console with key metrics: Total Customers (12,482), Active Couriers (842), Registered Merchants (1,203), Gross Merchandise Value ($34,520).
- Live Mogadishu Operations Map showing active drivers, pickups, and dropoff destinations.
- Order Dispatch Center with status filters, reassignment, and dispute resolution.
- Merchant and Courier approval workflows and document verification.
- Shiri Address Registry: Search and audit all registered digital addresses across Mogadishu.
- Telecom gateway monitor: EVC Plus, eDahab, and ZAAD transaction logs and idempotency keys.
- Immutable System Audit Logs.

---

## 2. PostgreSQL & Supabase Database Migration

The complete production SQL schema is located in `src/supabase/schema.sql`. It includes:
- **Tables**: `profiles`, `shiri_addresses`, `merchants`, `products`, `orders`, `order_items`, `order_status_history`, `courier_profiles`, `telecom_transactions`, `system_audit_logs`.
- **PostGIS Extension**: Spatial point geometry and GIST indexes on coordinates.
- **Row Level Security (RLS)**:
  - Customers can only access their own profile and orders.
  - Merchants can only edit their store's products and assigned orders.
  - Couriers can only view orders assigned to them.
  - Public can only view active, open merchants and in-stock products.

### Running Migrations in Supabase:
1. Open the Supabase Dashboard -> **SQL Editor**.
2. Copy and paste the contents of `src/supabase/schema.sql`.
3. Click **Run**.
4. Enable Realtime on the `orders` and `courier_profiles` tables under **Database -> Publications -> supabase_realtime**.

---

## 3. Environment Variables (`.env.example`)

```env
# Supabase Configuration
VITE_SUPABASE_URL="https://your-project.supabase.co"
VITE_SUPABASE_ANON_KEY="your-anon-public-key"

# Map Configuration
VITE_GOOGLE_MAPS_API_KEY="your-restricted-web-api-key"

# Somalia Telecom Gateway Credentials (Server-side Edge Functions only)
HORMUUD_EVC_PLUS_MERCHANT_UID="your-merchant-uid"
HORMUUD_EVC_PLUS_API_KEY="your-evc-api-key"
SOMTEL_EDAHAB_AGENT_CODE="your-edahab-agent-code"
SOMTEL_EDAHAB_SECRET_KEY="your-edahab-secret"
TELESOM_ZAAD_API_KEY="your-zaad-api-key"
```

*Note: Never include private service role keys or telecom merchant secrets in client application bundles.*

---

## 4. Mobile Build Instructions (React Native / Expo)

When exporting the mobile components (`apps/customer-mobile`, `apps/merchant-mobile`, `apps/courier-mobile`):

### Android (APK & AAB):
```bash
# 1. Install EAS CLI
npm install -g eas-cli

# 2. Configure project
eas build:configure

# 3. Build Android Development APK
eas build --platform android --profile development

# 4. Build Production Android App Bundle (.aab) for Google Play
eas build --platform android --profile production
```

### iOS (IPA):
```bash
# 1. Build iOS Simulator or Development build
eas build --platform ios --profile development

# 2. Build Production iOS IPA for Apple TestFlight / App Store
eas build --platform ios --profile production
```

---

## 5. Telecom API Integration Checklist

- [x] Abstraction layer implemented (`src/services/telecomProviders.ts`) with idempotency keys.
- [x] Hormuud EVC Plus USSD simulation (`*712*61XXXXXXX*Amount#`).
- [x] Somtel eDahab USSD simulation (`*789*62XXXXXXX*Amount#`).
- [x] Telesom ZAAD simulation (`*880*...#`).
- [x] Golis Sahal simulation (`*550*...#`).
- [x] Cash on Delivery reconciliation with driver physical cash tracking.
- [ ] Connect production merchant credentials with Hormuud/Somtel enterprise agreement upon approval.
