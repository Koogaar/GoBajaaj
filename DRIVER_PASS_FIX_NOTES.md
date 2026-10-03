# GoBajaaj Driver Pass Flow Fix

## What was fixed

The old source created a `pending` subscription and a `pending` payment during driver registration, before the driver had actually sent $0.25. Then the Driver Pass screen created another payment. This could create duplicates and could leave Admin Web looking at a different record.

The fixed flow is now:

1. Driver registers.
2. Registration creates/updates the driver profile only.
3. Driver opens Driver Pass.
4. Driver sends $0.25.
5. Driver enters the sender phone number.
6. `Xaqiiji oo U Dir Admin-ka` creates exactly one pending Driver Pass payment tied to one pending subscription.
7. Admin Web sees that pending payment.
8. Admin approves it.
9. Payment becomes `completed`, subscription becomes `active`, and the driver profile becomes `active`.
10. Driver app polling detects the active subscription.

## Important Supabase step

Run:

`supabase/FIX_DRIVER_PASS_SUBMISSION_FLOW.sql`

in the Supabase SQL Editor before testing.

This migration adds the sender/destination fields used by the Driver Pass flow and makes the `rejected` status accepted by the two payment tables.

## Error handling

The Android screen now displays the real Supabase/API failure under the sender-phone field instead of silently staying on the same screen.

Admin approval now throws and displays database errors instead of logging them and continuing as if approval succeeded.

## APK note

The existing APK inside `GoBajaaj_APK/` is not rebuilt by this source-only patch. Rebuild the Android project after applying the source changes and Supabase migration. Do not use the old APK to judge whether this fix is working.
