const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function testFullFlow() {
  console.log("==========================================");
  console.log("   GOBAJAAJ REAL ROLE & SECURITY VERIFIER ");
  console.log("==========================================\n");

  const results = {};

  // 1. MACMIIL / PASSENGER
  try {
    const email = `testpassenger_${Date.now()}@gmail.com`;
    const password = "Password123!";
    const { data: authData, error: authErr } = await supabase.auth.signUp({
      email,
      password,
      options: { data: { full_name: "Macmiil Test", role: "passenger" } }
    });

    if (authErr) {
      results.macmiil_signup = `FAIL: ${authErr.message}`;
    } else {
      const uid = authData.user?.id;
      // create profile as passenger
      const { error: profErr } = await supabase.from('profiles').upsert([{
        id: uid,
        full_name: "Macmiil Test",
        phone_number: "252610000001",
        role: "passenger",
        status: "active"
      }]);

      if (profErr) {
        results.macmiil_profile = `FAIL: ${profErr.message}`;
      } else {
        // Verify RLS access for passenger
        const clientPassenger = createClient(url, key, { auth: { persistSession: false } });
        await clientPassenger.auth.signInWithPassword({ email, password });
        
        const { data: myProfile, error: getProfErr } = await clientPassenger.from('profiles').select('*').eq('id', uid).single();
        results.macmiil_flow = (myProfile && !getProfErr) ? "PASS" : `FAIL: ${getProfErr?.message}`;
      }
    }
  } catch (e) {
    results.macmiil_flow = `FAIL: ${e.message}`;
  }

  // 2. DARAWAL
  try {
    const email = `testdriver_${Date.now()}@gmail.com`;
    const password = "Password123!";
    const { data: authData, error: authErr } = await supabase.auth.signUp({
      email,
      password,
      options: { data: { full_name: "Darawal Test", role: "driver" } }
    });

    if (!authErr && authData.user) {
      const uid = authData.user.id;
      const { error: profErr } = await supabase.from('profiles').upsert([{
        id: uid,
        full_name: "Darawal Test",
        phone_number: "252610000002",
        role: "driver",
        subscription_status: "active",
        subscription_expiry: new Date(Date.now() + 86400000).toISOString(),
        status: "active"
      }]);
      results.darawal_flow = profErr ? `FAIL: ${profErr.message}` : "PASS";
    } else {
      results.darawal_flow = `FAIL: ${authErr?.message}`;
    }
  } catch (e) {
    results.darawal_flow = `FAIL: ${e.message}`;
  }

  // 3. REPORTER
  try {
    const { data: reporters, error: repErr } = await supabase.from('profiles').select('*').eq('role', 'reporter');
    if (repErr) {
      results.reporter_flow = `FAIL: ${repErr.message}`;
    } else {
      results.reporter_flow = reporters && reporters.length > 0 ? "PASS" : "MA JIRO / WELI LAMA DHISIN";
    }
  } catch (e) {
    results.reporter_flow = `FAIL: ${e.message}`;
  }

  // 4. ADMIN ROLES
  try {
    const { data: adminProfiles, error: admErr } = await supabase.from('profiles').select('*').in('role', ['super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin']);
    results.admin_roles = admErr ? `FAIL: ${admErr.message}` : (adminProfiles?.length ? "PASS" : "MA JIRO / WELI LAMA DHISIN");
  } catch (e) {
    results.admin_roles = `FAIL: ${e.message}`;
  }

  // 5. TRAFFIC / REALTIME / INTERSECTIONS
  try {
    const { data: intersections } = await supabase.from('intersections').select('*');
    results.intersections_count = intersections?.length || 0;
    results.intersections_list = intersections?.map(i => i.name).join(", ");
  } catch (e) {
    results.intersections_count = 0;
  }

  console.log("TEST RESULTS:", JSON.stringify(results, null, 2));
}

testFullFlow();
