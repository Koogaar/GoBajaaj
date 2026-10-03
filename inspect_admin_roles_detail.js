const { createClient } = require("./admin-web/node_modules/@supabase/supabase-js");

const url = "https://lcigjbtprkloopvzpaxy.supabase.co";
const key = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM";
const supabase = createClient(url, key);

async function inspectRolesTable() {
  console.log("=== INSPECTING ROLES TABLE ===");
  const { data: roles, error: rolesErr } = await supabase.from('roles').select('*');
  console.log("Roles table error:", rolesErr ? rolesErr.message : "None");
  console.log("Roles table data:", roles);

  console.log("\n=== INSPECTING PERMISSIONS TABLE ===");
  const { data: perms, error: permsErr } = await supabase.from('permissions').select('*');
  console.log("Permissions table error:", permsErr ? permsErr.message : "None");
  console.log("Permissions table data:", perms);

  console.log("\n=== INSPECTING ROLE_PERMISSIONS TABLE ===");
  const { data: rolePerms, error: rpErr } = await supabase.from('role_permissions').select('*');
  console.log("Role Permissions error:", rpErr ? rpErr.message : "None");
  console.log("Role Permissions data count:", rolePerms?.length);

  console.log("\n=== INSPECTING ADMIN USERS ===");
  const { data: admins, error: admErr } = await supabase.from('admin_users').select('*');
  console.log("Admins count:", admins?.length);
  console.log("Admins roles present:", admins?.map(a => a.role_key));
}

inspectRolesTable();
