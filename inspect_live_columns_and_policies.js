const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function inspectColumns() {
  console.log("=== INSPECTING LIVE COLUMNS FOR ALL TABLES ===");

  const tables = [
    'admin_users', 'profiles', 'payment_transactions', 'subscriptions',
    'intersections', 'traffic_reports', 'support_tickets', 'support_messages',
    'devices', 'bajaj_rides', 'audit_logs'
  ];

  for (const t of tables) {
    const { data, error } = await supabase.from(t).select('*').limit(1);
    console.log(`\n--- TABLE: ${t} ---`);
    if (error) {
      console.log(`Error reading ${t}:`, error.message);
    } else if (data && data.length > 0) {
      console.log(`Sample row keys for ${t}:`, Object.keys(data[0]));
    } else {
      console.log(`Table ${t} is empty. Attempting to select columns via null query.`);
    }
  }
}

inspectColumns();
