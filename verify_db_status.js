const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function runVerification() {
  console.log("=== VERIFYING DATABASE & RLS ===");
  
  // 1. Intersections
  const { data: intersections, error: intErr } = await supabase.from('intersections').select('*');
  console.log("\n--- INTERSECTIONS ---");
  console.log("Count:", intersections?.length, "Error:", intErr?.message);
  if (intersections) {
    intersections.forEach(i => console.log(` - [${i.id}] ${i.name} (${i.latitude}, ${i.longitude}) radius:${i.radius_meters}m active:${i.is_active}`));
  }

  // 2. Traffic Reports
  const { data: reports, error: repErr } = await supabase.from('traffic_reports').select('*').limit(5);
  console.log("\n--- TRAFFIC REPORTS ---");
  console.log("Count:", reports?.length, "Error:", repErr?.message);

  // 3. Rides (Bajaj)
  const { data: rides, error: rideErr } = await supabase.from('bajaj_rides').select('*').limit(5);
  console.log("\n--- BAJAJ RIDES ---");
  console.log("Count:", rides?.length, "Error:", rideErr?.message);

  // 4. Lost Phones / Devices
  const { data: devices, error: devErr } = await supabase.from('devices').select('*').limit(5);
  console.log("\n--- DEVICES (LOST PHONES) ---");
  console.log("Count:", devices?.length, "Error:", devErr?.message);

  // 5. Profiles & Roles check
  const { data: profiles, error: profErr } = await supabase.from('profiles').select('id, full_name, role, status').limit(20);
  console.log("\n--- PROFILES & ROLES ---");
  console.log("Count:", profiles?.length, "Error:", profErr?.message);
  if (profiles) {
    const rolesFound = [...new Set(profiles.map(p => p.role))];
    console.log("Distinct roles in DB:", rolesFound);
  }
}

runVerification();
