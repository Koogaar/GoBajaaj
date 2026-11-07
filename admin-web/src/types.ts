export type AdminRole = 
  | 'super_admin'
  | 'payment_admin'
  | 'traffic_admin'
  | 'reporter_manager'
  | 'support_agent'
  | 'analyst'
  | 'ride_admin'
  | 'marketer';

export type UserRole = 
  | AdminRole
  | 'driver' 
  | 'courier'
  | 'merchant'
  | 'passenger'
  | 'reporter';

export type PermissionKey =
  | 'users.view' | 'users.create' | 'users.update' | 'users.suspend' | 'users.delete'
  | 'payments.view' | 'payments.approve' | 'payments.reject' | 'payments.update'
  | 'subscriptions.view' | 'subscriptions.update'
  | 'traffic.view' | 'traffic.create' | 'traffic.update' | 'traffic.delete'
  | 'intersections.view' | 'intersections.create' | 'intersections.update' | 'intersections.delete'
  | 'reporters.view' | 'reporters.create' | 'reporters.update' | 'reporters.assign' | 'reporters.deactivate'
  | 'protection.view' | 'protection.commands'
  | 'notifications.view' | 'notifications.create' | 'notifications.send'
  | 'support.view' | 'support.reply' | 'support.update'
  | 'analytics.view' | 'analytics.export'
  | 'admins.view' | 'admins.create' | 'admins.update' | 'admins.deactivate'
  | 'roles.view' | 'roles.create' | 'roles.update'
  | 'audit_logs.view'
  | 'settings.view' | 'settings.update'
  | 'ride.view' | 'ride.manage'
  | 'marketing.view' | 'marketing.manage';

export interface Profile {
  id: string;
  full_name: string;
  phone_number: string;
  role: UserRole;
  status: 'active' | 'suspended' | 'disabled' | 'pending';
  plate_number?: string;
  bajaaj_model?: string;
  license_number?: string;
  guarantor_name?: string;
  guarantor_phone?: string;
  cid_letter_number?: string;
  business_name?: string;
  business_type?: string;
  merchant_payment_number?: string;
  district?: string;
  verification_status?: string;
  monthly_fee?: number;
  subscription_status?: string;
  subscription_expiry?: string;
  created_at: string;
}

export interface AdminUser {
  id: string;
  user_id: string;
  username?: string;
  email: string;
  full_name: string;
  phone?: string;
  role_key: AdminRole;
  status: 'active' | 'suspended' | 'disabled';
  last_login?: string;
  created_at: string;
  updated_at?: string;
}

export interface Intersection {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  allowed_radius: number;
  city: string;
  current_status: 'Furan' | 'Normal' | 'Jaam' | 'Xeran' | 'Dhexdhexaad' | 'Mashquul' | 'Saxmad badan' | 'Xiran' | 'Wuu xiranyahay';
  assigned_reporter_id?: string;
  assigned_reporter_name?: string;
  status?: 'active' | 'inactive';
  last_updated: string;
}

export interface TrafficReport {
  id: string;
  intersection_id: string;
  reporter_id: string;
  reporter_name?: string;
  status: string;
  cause: string;
  notes: string;
  latitude?: number;
  longitude?: number;
  distance_from_intersection?: number;
  verification_status: 'verified' | 'outside_radius' | 'unverified';
  created_at: string;
}

export interface Subscription {
  id: string;
  user_id: string;
  user_name?: string;
  plan_name: string;
  amount: number;
  currency: string;
  status: 'active' | 'expired' | 'cancelled' | 'pending' | 'failed' | 'rejected';
  payment_method: string;
  payment_reference?: string;
  sender_phone?: string;
  destination_phone?: string;
  rejection_reason?: string;
  start_date: string;
  end_date: string;
}

export interface PaymentTransaction {
  id: string;
  subscription_id?: string;
  user_id: string;
  user_name?: string;
  amount: number;
  currency: string;
  payment_method: string;
  reference_number: string;
  phone_number: string;
  account_phone?: string;
  sender_phone?: string;
  status: 'completed' | 'pending' | 'failed' | 'rejected';
  rejection_reason?: string;
  created_at: string;
}

export interface MarketingAgent {
  id: string;
  user_id?: string;
  username: string;
  full_name: string;
  phone?: string;
  code: string;
  password?: string;
  status: 'active' | 'suspended' | 'disabled';
  created_at: string;
  referred_count?: number;
  active_referred_count?: number;
}

export interface Device {
  id: string;
  user_id: string;
  user_name?: string;
  device_name: string;
  platform: string;
  is_protected: boolean;
  is_lost: boolean;
  battery_level: number;
  is_online: boolean;
  last_latitude?: number;
  last_longitude?: number;
  last_seen: string;
}

export interface SupportTicket {
  id: string;
  user_id: string;
  user_name?: string;
  ticket_number: number;
  category: string;
  priority: string;
  subject: string;
  message: string;
  status: 'Furan' | 'Socda' | 'La xaliyay' | 'Xiran';
  assigned_agent?: string;
  created_at: string;
}

export interface AuditLog {
  id: string;
  actor_id?: string;
  actor_name: string;
  action: string;
  target_type: string;
  target_id?: string;
  metadata?: any;
  created_at: string;
}

