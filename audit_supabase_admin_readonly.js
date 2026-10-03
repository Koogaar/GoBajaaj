const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function inspectSchemaAndRoles() {
  console.log("=== SUPABASE READ-ONLY AUDIT ===");
  console.log("Configured Supabase URL:", url);

  // 1. Connection check
  const { data: intData, error: intErr } = await supabase.from('intersections').select('id, name').limit(1);
  if (intErr) {
    console.log("SUPABASE CONNECTION STATUS: FAIL -", intErr.message);
  } else {
    console.log("SUPABASE CONNECTION STATUS: PASS - Connected successfully to project lcigjbtprkloopvzpaxy");
  }

  // 2. Profiles table roles
  const { data: profilesData, error: profErr } = await supabase.from('profiles').select('id, full_name, role, status').limit(50);
  console.log("\n--- PROFILES TABLE ---");
  console.log("Error:", profErr ? profErr.message : "None");
  if (profilesData) {
    const roles = [...new Set(profilesData.map(p => p.role))];
    console.log("Existing distinct roles in profiles:", roles);
    console.log("Total profiles sampled:", profilesData.length);
  }

  // 3. Admin Users table
  const { data: adminUsersData, error: adminErr } = await supabase.from('admin_users').select('*').limit(50);
  console.log("\n--- ADMIN_USERS TABLE ---");
  console.log("Error:", adminErr ? adminErr.message : "None");
  if (adminUsersData) {
    const adminRoles = [...new Set(adminUsersData.map(a => a.role_key || a.role))];
    console.log("Existing distinct roles in admin_users:", adminRoles);
    console.log("Total admin_users rows:", adminUsersData.length);
    console.log("Admin Users records:", JSON.stringify(adminUsersData, null, 2));
  }

  // 4. Test Role Constraint / Schema Check (Read-Only metadata if possible, or error testing on profiles role constraint)
  // Let's check table structure by attempting an invalid select or examining profiles table definition files in the repo
}

inspectSchemaAndRoles();
