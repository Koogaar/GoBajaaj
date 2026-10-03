const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function checkCurrentCounts() {
  console.log("=== CHECKING CURRENT TEST DATA COUNTS ===");
  const tables = ['payment_transactions', 'subscriptions', 'traffic_reports', 'bajaj_rides', 'devices', 'profiles'];
  for (const t of tables) {
    const { count, error } = await supabase.from(t).select('*', { count: 'exact', head: true });
    console.log(`Table ${t} current count:`, count, "Error:", error?.message);
  }
}

checkCurrentCounts();
