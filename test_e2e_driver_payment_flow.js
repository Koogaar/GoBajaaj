const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function testDatabaseFlow() {
  console.log("=================================================");
  console.log("   TESTING END-TO-END DRIVER FLOW IN DATABASE    ");
  console.log("=================================================\n");

  const realUserId = "00000000-0000-4000-a000-000000000777";
  const testPhone = "252619888777";
  const fullName = "Darawal Test Real Flow";
  const plateNumber = "M-7777";

  // 1. Upsert Profile with REAL User ID
  console.log("1. Upserting Driver Profile into Supabase profiles table...");
  const { error: profErr } = await supabase.from('profiles').upsert([{
    id: realUserId,
    full_name: fullName,
    phone_number: testPhone,
    role: "driver",
    status: "active",
    plate_number: plateNumber,
    subscription_status: "pending"
  }]);

  if (profErr) {
    console.error("❌ Profile Upsert Failed:", profErr.message);
    return;
  }
  console.log("✅ Driver Profile Created/Updated in Supabase!");

  // 2. Submit $0.25 Driver Pass Pending Subscription & Payment
  console.log("\n2. Inserting Pending Subscription and Payment Transaction...");
  const subId = "11111111-2222-4333-a444-555555555555";
  const refNum = `EVC-${testPhone.slice(-4)}-${Date.now() % 100000}`;

  const { error: subErr } = await supabase.from('subscriptions').insert([{
    id: subId,
    user_id: realUserId,
    plan_name: "Driver Pass (Maalinle 05:00 AM)",
    amount: 0.25,
    currency: "USD",
    status: "pending",
    payment_method: "EVC Plus",
    payment_reference: refNum,
    auto_renew: false
  }]);

  if (subErr) {
    console.error("❌ Subscription Insert Failed:", subErr.message);
    return;
  }

  const { error: txErr } = await supabase.from('payment_transactions').insert([{
    user_id: realUserId,
    subscription_id: subId,
    amount: 0.25,
    currency: "USD",
    payment_method: "EVC Plus",
    reference_number: refNum,
    phone_number: testPhone,
    status: "pending"
  }]);

  if (txErr) {
    console.error("❌ Payment Transaction Insert Failed:", txErr.message);
    return;
  }
  console.log("✅ Pending Subscription & Payment Transaction Inserted!");

  // 3. Query as Admin Web (PaymentManagement.tsx query)
  console.log("\n3. Admin Web Fetching Pending Payments...");
  const { data: pendingPayments, error: adminErr } = await supabase
    .from('payment_transactions')
    .select('*')
    .eq('user_id', realUserId)
    .eq('status', 'pending');

  if (adminErr || !pendingPayments || pendingPayments.length === 0) {
    console.error("❌ Admin Web Query Failed:", adminErr?.message);
    return;
  }
  console.log("✅ Admin Web Successfully Found Pending Payment! Ref:", pendingPayments[0].reference_number);

  // 4. Admin Approves Payment
  console.log("\n4. Admin Approving Driver Payment...");
  const now = new Date();
  const expiry = new Date(now.getTime() + 24 * 60 * 60 * 1000);

  await supabase.from('payment_transactions').update({ status: 'completed' }).eq('id', pendingPayments[0].id);
  await supabase.from('subscriptions').update({ status: 'active', end_date: expiry.toISOString() }).eq('id', subId);
  await supabase.from('profiles').update({ subscription_status: 'active', subscription_expiry: expiry.toISOString() }).eq('id', realUserId);

  console.log("✅ Payment Approved by Admin!");

  // 5. Android App Syncs Approved Status
  console.log("\n5. Android Driver App Syncing Status...");
  const { data: syncedSub, error: syncErr } = await supabase
    .from('subscriptions')
    .select('*')
    .eq('user_id', realUserId)
    .single();

  if (syncErr || syncedSub.status !== 'active') {
    console.error("❌ Android Status Sync Failed:", syncErr?.message);
    return;
  }

  console.log("✅ Android App Synced Active Subscription! Status:", syncedSub.status, "Expiry:", syncedSub.end_date);

  // Clean up
  console.log("\n6. Cleaning up test data...");
  await supabase.from('payment_transactions').delete().eq('user_id', realUserId);
  await supabase.from('subscriptions').delete().eq('user_id', realUserId);
  await supabase.from('profiles').delete().eq('id', realUserId);
  console.log("✅ Cleaned up test data.");

  console.log("\n=================================================");
  console.log("🎉 ALL END-TO-END FLOW CHECKS PASSED PERFECTLY!");
  console.log("=================================================");
}

testDatabaseFlow();
