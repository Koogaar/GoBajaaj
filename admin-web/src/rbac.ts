import { AdminRole, PermissionKey } from './types';

export interface RoleInfo {
  key: AdminRole;
  name: string;
  nameSomali: string;
  description: string;
  badgeBg: string;
  badgeText: string;
  permissions: PermissionKey[];
}

export const ADMIN_ROLES: Record<AdminRole, RoleInfo> = {
  super_admin: {
    key: 'super_admin',
    name: 'SUPER ADMIN',
    nameSomali: 'Maamulaha Guud',
    description: 'Fasax buuxa oo habka oo dhan ah. Wuxuu maamuli karaa admin-yada kale.',
    badgeBg: 'bg-red-100',
    badgeText: 'text-red-800',
    permissions: [
      'users.view', 'users.create', 'users.update', 'users.suspend',
      'payments.view', 'payments.approve', 'payments.reject', 'payments.update',
      'subscriptions.view', 'subscriptions.update',
      'traffic.view', 'traffic.create', 'traffic.update', 'traffic.delete',
      'intersections.view', 'intersections.create', 'intersections.update', 'intersections.delete',
      'reporters.view', 'reporters.create', 'reporters.update', 'reporters.assign', 'reporters.deactivate',
      'protection.view', 'protection.commands',
      'notifications.view', 'notifications.create', 'notifications.send',
      'support.view', 'support.reply', 'support.update',
      'analytics.view', 'analytics.export',
      'admins.view', 'admins.create', 'admins.update', 'admins.deactivate',
      'roles.view', 'roles.create', 'roles.update',
      'audit_logs.view',
      'settings.view', 'settings.update'
    ]
  },
  payment_admin: {
    key: 'payment_admin',
    name: 'PAYMENT ADMIN',
    nameSomali: 'Maamulaha Lacagaha',
    description: 'Ansixinta iyo diidmada bixinta Driver Pass ($0.25/Shift).',
    badgeBg: 'bg-emerald-100',
    badgeText: 'text-emerald-800',
    permissions: [
      'users.view',
      'payments.view', 'payments.approve', 'payments.reject',
      'subscriptions.view',
      'notifications.view'
    ]
  },
  traffic_admin: {
    key: 'traffic_admin',
    name: 'TRAFFIC ADMIN',
    nameSomali: 'Maamulaha Saxmadda',
    description: 'Maamulka isgoysyada, xaaladaha saxmadda, iyo warbixinnada gaadiidka.',
    badgeBg: 'bg-blue-100',
    badgeText: 'text-blue-800',
    permissions: [
      'traffic.view', 'traffic.create', 'traffic.update', 'traffic.delete',
      'intersections.view', 'intersections.create', 'intersections.update', 'intersections.delete'
    ]
  },
  reporter_manager: {
    key: 'reporter_manager',
    name: 'REPORTER MANAGER',
    nameSomali: 'Maamulaha Wariyeyaasha',
    description: 'Qorista, maamulida, iyo u xilsaarista wariyeyaasha saxmadda isgoysyada.',
    badgeBg: 'bg-purple-100',
    badgeText: 'text-purple-800',
    permissions: [
      'reporters.view', 'reporters.create', 'reporters.update', 'reporters.assign', 'reporters.deactivate',
      'traffic.view',
      'intersections.view'
    ]
  },
  support_agent: {
    key: 'support_agent',
    name: 'SUPPORT AGENT',
    nameSomali: 'Shaqaalaha Taageerada',
    description: 'Wadahadalka macaamiisha, cabashooyinka, iyo caawinta darawallada.',
    badgeBg: 'bg-amber-100',
    badgeText: 'text-amber-800',
    permissions: [
      'users.view',
      'support.view', 'support.reply', 'support.update',
      'payments.view',
      'subscriptions.view',
      'protection.view'
    ]
  },
  analyst: {
    key: 'analyst',
    name: 'ANALYST',
    nameSomali: 'Aqoonyahan Xogta (Read-Only)',
    description: 'Eegida shaxanka, tirakoobka, iyo warbixinnada habka oo kaliya (Akhris keliya).',
    badgeBg: 'bg-indigo-100',
    badgeText: 'text-indigo-800',
    permissions: [
      'analytics.view', 'analytics.export',
      'users.view',
      'payments.view',
      'subscriptions.view',
      'traffic.view',
      'reporters.view',
      'intersections.view'
    ]
  },
  ride_admin: {
    key: 'ride_admin',
    name: 'RIDE & SUPPORT ADMIN',
    nameSomali: 'Maamulaha GoBajaaj & Cabashooyinka',
    description: 'Maamulida dalabaadka Bajajyada (GoBajaaj Ride) iyo ka jawaabista cabashooyinka/taageerada macaamiisha.',
    badgeBg: 'bg-rose-100',
    badgeText: 'text-rose-800',
    permissions: [
      'ride.view', 'ride.manage',
      'support.view', 'support.reply', 'support.update',
      'users.view',
      'notifications.view', 'notifications.create', 'notifications.send'
    ]
  }
};

export function hasPermission(role: AdminRole | null | undefined, permission: PermissionKey): boolean {
  if (!role) return false;
  if (role === 'super_admin') return true;
  const roleConfig = ADMIN_ROLES[role];
  if (!roleConfig) return false;
  return roleConfig.permissions.includes(permission);
}

export function getRoleDisplay(role: string): { name: string; nameSomali: string; bg: string; text: string } {
  const adminRole = role as AdminRole;
  if (ADMIN_ROLES[adminRole]) {
    const r = ADMIN_ROLES[adminRole];
    return { name: r.name, nameSomali: r.nameSomali, bg: r.badgeBg, text: r.badgeText };
  }
  if (role === 'reporter') {
    return { name: 'REPORTER', nameSomali: 'Wariye Gaadiid', bg: 'bg-sky-100', text: 'text-sky-800' };
  }
  return { name: 'DRIVER', nameSomali: 'Wade Gaadiid', bg: 'bg-neutral-100', text: 'text-neutral-800' };
}
