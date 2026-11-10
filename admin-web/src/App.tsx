import React, { useState, useEffect } from 'react';
import logoImg from './assets/logo.png';
import {
  Shield,
  TrafficCone,
  Users,
  CreditCard,
  MapPin,
  Smartphone,
  Headphones,
  UserCheck,
  FileText,
  AlertTriangle,
  LogOut,
  Search,
  CheckCircle2,
  RefreshCw,
  BarChart3,
  History,
  Menu,
  X,
  Lock,
  ChevronRight,
  ChevronLeft,
  PanelLeftClose,
  PanelLeftOpen,
  Zap,
  Sparkles,
  Clock,
  Bike,
  Volume2,
  VolumeX,
  Bell,
  Megaphone,
  Trash2,
  ShieldOff,
  ShieldCheck,
  Wallet,
  Store,
  Layers,
  ShoppingBag,
  Truck,
  Settings,
  DollarSign,
  Package
} from 'lucide-react';
import { supabase } from './supabase';
import { soundAlert } from './utils/soundAlert';
import { AdminUser, Profile, AdminRole, MarketingAgent } from './types';
import { ADMIN_ROLES, hasPermission, getRoleDisplay } from './rbac';
import { LoginScreen } from './components/LoginScreen';
import { AdminManagement } from './components/AdminManagement';
import { ReporterManagement } from './components/ReporterManagement';
import { PaymentManagement, isWithinMinutes, formatTimeAgoSomali, formatExactDateTime } from './components/PaymentManagement';
import { TrafficManagement } from './components/TrafficManagement';
import { AnalyticsDashboard } from './components/AnalyticsDashboard';
import { AuditLogsView } from './components/AuditLogsView';
import { DeviceSupportManagement } from './components/DeviceSupportManagement';
import { RideSupportManagement } from './components/RideSupportManagement';
import { MarketingManagement } from './components/MarketingManagement';
import { MarketerPortal } from './components/MarketerPortal';
import { DriversManagement } from './components/DriversManagement';
import { DailyRechargedManagement } from './components/DailyRechargedManagement';
import { PricingControlCenter } from './components/PricingControlCenter';
import { MerchantsManagement } from './components/MerchantsManagement';
import { CategoriesManagement } from './components/CategoriesManagement';
import { LiveOrdersManagement } from './components/LiveOrdersManagement';
import { CouriersManagement } from './components/CouriersManagement';
import { ShiriDeliveryManagement } from './components/ShiriDeliveryManagement';
import { ShiriBusinessEats } from './components/ShiriBusinessEats';

export default function App() {
  const [session, setSession] = useState<any>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);
  const [currentAdmin, setCurrentAdmin] = useState<AdminUser | null>(null);
  const [marketingAgent, setMarketingAgent] = useState<MarketingAgent | null>(() => {
    try {
      const saved = localStorage.getItem('gobajaaj_marketing_agent');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const checkMarketingSession = () => {
    try {
      const saved = localStorage.getItem('gobajaaj_marketing_agent');
      if (saved) {
        setMarketingAgent(JSON.parse(saved));
      }
    } catch {
      setMarketingAgent(null);
    }
  };

  const handleMarketerLogout = () => {
    localStorage.removeItem('gobajaaj_marketing_agent');
    setMarketingAgent(null);
  };

  // Tab State
  const [activeTab, setActiveTab] = useState<string>('overview');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);

  // Overview Data State
  const [overviewStats, setOverviewStats] = useState({
    usersCount: 0,
    subscribersCount: 0,
    revenue: 0,
    dailyRevenue: 0,
    todayApprovedCount: 0,
    reportersCount: 0,
    intersectionsCount: 0,
    reportsCount: 0,
    devicesCount: 0,
    pendingPaymentsCount: 0,
    recentRegistrationsCount: 0,
    recentPaymentsCount: 0,
  });
  const [isLoadingOverview, setIsLoadingOverview] = useState(false);
  const [overviewError, setOverviewError] = useState<string | null>(null);

  // Users Tab State
  const [usersList, setUsersList] = useState<Profile[]>([]);
  const [userSearchQuery, setUserSearchQuery] = useState('');
  const [userFilter, setUserFilter] = useState<'all' | 'recent10' | 'driver' | 'user' | 'reporter'>('all');
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);

  // User action state
  const [userActionModal, setUserActionModal] = useState<{
    isOpen: boolean;
    user: Profile | null;
    action: 'delete' | 'suspend' | 'activate';
    isProcessing: boolean;
  }>({ isOpen: false, user: null, action: 'delete', isProcessing: false });

  // Demo Data Cleanup State
  const [isDemoCleanupOpen, setIsDemoCleanupOpen] = useState(false);
  const [isDemoCleanupProcessing, setIsDemoCleanupProcessing] = useState(false);
  const [demoCleanupResult, setDemoCleanupResult] = useState<string | null>(null);

  const handleDeleteDemoData = async () => {
    setIsDemoCleanupProcessing(true);
    setDemoCleanupResult(null);
    try {
      // 1. Get all profiles to count before deletion
      const { data: allProfiles } = await supabase.from('profiles').select('id, role, full_name');
      const driverIds = (allProfiles || []).filter(p => p.role === 'driver').map(p => p.id);
      const passengerIds = (allProfiles || []).filter(p => p.role === 'passenger').map(p => p.id);
      const allIds = [...driverIds, ...passengerIds];

      if (allIds.length === 0) {
        setDemoCleanupResult('⚠️ Ma jiraan darawallo ama rakaab la tirtiri karo.');
        setIsDemoCleanupProcessing(false);
        return;
      }

      let deletedCount = 0;

      // 2. Delete related data first (subscriptions, payment_transactions, bajaj_rides, driver_presence, audit_logs)
      if (allIds.length > 0) {
        try { await supabase.from('subscriptions').delete().in('user_id', allIds); } catch { }
        try { await supabase.from('payment_transactions').delete().in('user_id', allIds); } catch { }
        try { await supabase.from('bajaj_rides').delete().in('passenger_id', passengerIds); } catch { }
        try { await supabase.from('bajaj_rides').delete().in('driver_id', driverIds); } catch { }
        try { await supabase.from('driver_presence').delete().in('driver_id', driverIds); } catch { }
      }

      // 3. Delete all driver profiles
      if (driverIds.length > 0) {
        const { error: dErr } = await supabase.from('profiles').delete().in('id', driverIds);
        if (!dErr) deletedCount += driverIds.length;
      }

      // 4. Delete all passenger profiles
      if (passengerIds.length > 0) {
        const { error: pErr } = await supabase.from('profiles').delete().in('id', passengerIds);
        if (!pErr) deletedCount += passengerIds.length;
      }

      // 5. Log the cleanup action
      try {
        await supabase.from('audit_logs').insert([{
          actor_name: currentAdmin?.full_name || 'Admin',
          action: 'DELETE_ALL_DEMO_DATA',
          target_type: 'profiles',
          target_id: 'bulk',
          metadata: {
            drivers_deleted: driverIds.length,
            passengers_deleted: passengerIds.length,
            total: deletedCount
          }
        }]);
      } catch { }

      setDemoCleanupResult(`✅ Guul! ${deletedCount} akoon ayaa la tirtiray (${driverIds.length} darawal + ${passengerIds.length} rakaab). Dhammaan xogta demo-da waa la nadiifiyay.`);

      // Refresh user lists
      await fetchUsers();
      await fetchOverviewMetrics(true);
    } catch (err: any) {
      setDemoCleanupResult('❌ Cilad: ' + (err?.message || 'Tirtirista kuma guulaysanin.'));
    } finally {
      setIsDemoCleanupProcessing(false);
    }
  };

  // Sound Notification State
  const [isSoundOn, setIsSoundOn] = useState(soundAlert.isEnabled());
  const [paymentToast, setPaymentToast] = useState<{ show: boolean; text: string; time: string } | null>(null);

  const toggleSound = () => {
    const newState = !isSoundOn;
    setIsSoundOn(newState);
    soundAlert.setEnabled(newState);
    if (newState) {
      soundAlert.playPaymentSound();
    }
  };

  // Check Supabase Auth Session
  const checkAuthSession = async () => {
    setIsAuthChecking(true);
    try {
      const { data: { session: currentSession } } = await supabase.auth.getSession();
      setSession(currentSession);

      if (currentSession?.user) {
        const userEmail = currentSession.user.email || '';
        let { data: adminRows } = await supabase
          .from('admin_users')
          .select('*')
          .or(`user_id.eq.${currentSession.user.id},email.eq.${userEmail}`)
          .limit(1);
        const adminData = adminRows?.[0] || null;

        if (adminData && !adminData.user_id) {
          await supabase
            .from('admin_users')
            .update({ user_id: currentSession.user.id })
            .eq('id', adminData.id);
        }

        if (adminData) {
          setCurrentAdmin(adminData as AdminUser);
        } else {
          // Check profiles
          const { data: profData } = await supabase
            .from('profiles')
            .select('*')
            .eq('id', currentSession.user.id)
            .maybeSingle();

          if (profData && ['super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst'].includes(profData.role)) {
            setCurrentAdmin({
              id: profData.id,
              user_id: profData.id,
              email: currentSession.user.email || profData.phone_number || 'admin@somalitraffic.so',
              full_name: profData.full_name || 'Admin User',
              role_key: profData.role as AdminRole,
              status: profData.status === 'disabled' ? 'disabled' : (profData.status === 'suspended' ? 'suspended' : 'active'),
              created_at: profData.created_at
            });
          } else {
            // No admin_users record and no recognized admin role on the profile.
            // Never auto-grant privileges: access is denied until a Super Admin
            // explicitly creates an admin_users record for this account.
            console.warn('No admin role found for this session; access denied.');
            setCurrentAdmin(null);
          }
        }
      } else {
        setCurrentAdmin(null);
      }
    } catch (err) {
      console.error('Session check error:', err);
    } finally {
      setIsAuthChecking(false);
    }
  };

  useEffect(() => {
    checkAuthSession();

    const { data: authListener } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      if (session) {
        checkAuthSession();
      } else {
        setCurrentAdmin(null);
      }
    });

    return () => {
      authListener.subscription.unsubscribe();
    };
  }, []);

  // Fetch Overview Metrics
  const fetchOverviewMetrics = async (isInitial = false) => {
    if (isInitial && overviewStats.usersCount === 0) {
      setIsLoadingOverview(true);
    }
    setOverviewError(null);
    try {
      const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString();
      const todayDateStr = new Date().toLocaleDateString('en-CA');

      const { count: uCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
      const { count: sCount } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).eq('status', 'active');
      const { data: pData } = await supabase.from('payment_transactions').select('amount, created_at').eq('status', 'completed');
      const rev = pData ? pData.reduce((acc, curr) => acc + Number(curr.amount || 0.25), 0) : 0;

      const todayCompleted = pData ? pData.filter(p => {
        if (!p.created_at) return false;
        try {
          return new Date(p.created_at).toLocaleDateString('en-CA') === todayDateStr;
        } catch {
          return false;
        }
      }) : [];
      const dailyRev = todayCompleted.reduce((acc, curr) => acc + Number(curr.amount || 0.25), 0);

      const { count: pendPayCount } = await supabase.from('payment_transactions').select('*', { count: 'exact', head: true }).eq('status', 'pending');
      const { count: pendSubCount } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).eq('status', 'pending');
      const totalPending = Math.max(pendPayCount || 0, pendSubCount || 0);
      const { count: rCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('role', 'reporter').eq('status', 'active');
      const { count: iCount } = await supabase.from('intersections').select('*', { count: 'exact', head: true });
      const { count: repCount } = await supabase.from('traffic_reports').select('*', { count: 'exact', head: true });
      const { count: dCount } = await supabase.from('devices').select('*', { count: 'exact', head: true });
      const { count: recRegCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).gte('created_at', tenMinutesAgo);
      const { count: recPayCount } = await supabase.from('payment_transactions').select('*', { count: 'exact', head: true }).gte('created_at', tenMinutesAgo);

      setOverviewStats({
        usersCount: uCount || 0,
        subscribersCount: sCount || 0,
        revenue: rev,
        dailyRevenue: dailyRev,
        todayApprovedCount: todayCompleted.length,
        reportersCount: rCount || 0,
        intersectionsCount: iCount || 0,
        reportsCount: repCount || 0,
        devicesCount: dCount || 0,
        pendingPaymentsCount: totalPending,
        recentRegistrationsCount: recRegCount || 0,
        recentPaymentsCount: recPayCount || 0,
      });
    } catch (err: any) {
      console.error('Overview Metrics Error:', err);
      setOverviewError('Cilad ayaa dhacday marka la soo qaadayay xogta guud.');
    } finally {
      setIsLoadingOverview(false);
    }
  };

  // Fetch Users for Users tab
  const fetchUsers = async () => {
    setIsLoadingUsers(true);
    try {
      const { data, error } = await supabase
        .from('profiles')
        .select('*')
        .order('created_at', { ascending: false });

      if (!error && data) {
        setUsersList(data as Profile[]);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoadingUsers(false);
    }
  };

  const [approvingUserId, setApprovingUserId] = useState<string | null>(null);
  const handleQuickApprovePass = async (user: Profile) => {
    if (!user.id) return;
    setApprovingUserId(user.id);
    try {
      // A Driver Pass may only be activated against a real pending $0.25 claim.
      let targetId: string | undefined;

      const { data: pendingTx } = await supabase
        .from('payment_transactions')
        .select('id')
        .eq('user_id', user.id)
        .eq('status', 'pending')
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();
      targetId = (pendingTx?.id as string | undefined) || undefined;

      if (!targetId) {
        const { data: pendingSub } = await supabase
          .from('subscriptions')
          .select('id')
          .eq('user_id', user.id)
          .eq('status', 'pending')
          .order('created_at', { ascending: false })
          .limit(1)
          .maybeSingle();
        targetId = (pendingSub?.id as string | undefined) || undefined;
      }

      if (!targetId) {
        alert('Darawalkaani ma jiro bixin $0.25 oo sugaysa. Fadlan sug in uu lacagta soo diro ka hor ansixinta.');
        return;
      }

      const { data, error } = await supabase.rpc('approve_driver_pass_payment', {
        p_payment_id: targetId
      });
      if (error) {
        throw new Error(error.message);
      }
      const res = (data || {}) as any;
      if (res.ok !== true) {
        const messages: Record<string, string> = {
          forbidden: 'Ma lehid fasax aad ku ansixiso Driver Pass-ka.',
          not_pending: 'Bixintan mar hore ayaa la maareeyay.',
          already_processed: 'Bixintan mar hore ayaa la maareeyay.',
          payment_not_found: 'Bixin lama helin.'
        };
        throw new Error(messages[res.error] || `Ansixinta way fashilantay (${res.error || 'unknown'}).`);
      }

      soundAlert.playPaymentSound();
      alert(`Guul! Darawalka ${user.full_name || user.phone_number} waa la ansixiyay, adeeggiisuna hadda waa Active!`);
      await fetchUsers();
      await fetchOverviewMetrics(false);
    } catch (err: any) {
      alert('Cilad: ' + (err?.message || 'Ansixinta kuma guulaysanin.'));
    } finally {
      setApprovingUserId(null);
    }
  };

  const handleUserAction = async () => {
    const { user, action } = userActionModal;
    if (!user) return;
    setUserActionModal(s => ({ ...s, isProcessing: true }));
    try {
      if (action === 'delete') {
        await supabase.from('profiles').delete().eq('id', user.id);
        try {
          await supabase.from('audit_logs').insert([{
            actor_name: currentAdmin?.full_name || 'Admin',
            action: 'DELETE_USER',
            target_type: 'profile',
            target_id: user.id,
            metadata: { name: user.full_name, phone: user.phone_number }
          }]);
        } catch { }
      } else {
        const newStatus = action === 'suspend' ? 'suspended' : 'active';
        await supabase.from('profiles').update({ status: newStatus }).eq('id', user.id);
        try {
          await supabase.from('audit_logs').insert([{
            actor_name: currentAdmin?.full_name || 'Admin',
            action: action === 'suspend' ? 'SUSPEND_USER' : 'ACTIVATE_USER',
            target_type: 'profile',
            target_id: user.id,
            metadata: { name: user.full_name, phone: user.phone_number, new_status: newStatus }
          }]);
        } catch { }
      }
      setUserActionModal({ isOpen: false, user: null, action: 'delete', isProcessing: false });
      await fetchUsers();
    } catch (err: any) {
      alert('Cilad: ' + (err?.message || 'Ficilka kuma guulaysanin.'));
      setUserActionModal(s => ({ ...s, isProcessing: false }));
    }
  };

  useEffect(() => {
    if (session && currentAdmin) {
      fetchOverviewMetrics(true);

      const interval = setInterval(() => {
        fetchOverviewMetrics(false);
      }, 10000);

      // Global Realtime listener for incoming payments to play Cash Chime & Voice Alert
      const paymentChannel = supabase
        .channel('global_payment_alerts')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'payment_transactions' }, (payload: any) => {
          if (payload?.eventType === 'INSERT') {
            const phone = payload.new?.phone_number || '';
            const amt = payload.new?.amount || '0.25';
            soundAlert.playPaymentVoiceAlert(
              `Fariin cusub: Darawal ayaa soo dirsaday lacagta Driver Pass ${amt} dollar. Fadlan xaqiiji!`
            );
            setPaymentToast({
              show: true,
              text: `🔔 LACAG CUSUB: $${amt} ayaa hadda soo dhacday! ${phone ? `(${phone})` : ''}`,
              time: new Date().toLocaleTimeString()
            });
            fetchOverviewMetrics(false);
          }
        })
        .on('postgres_changes', { event: '*', schema: 'public', table: 'subscriptions' }, (payload: any) => {
          if (payload?.eventType === 'INSERT') {
            soundAlert.playPaymentVoiceAlert(
              'Fariin cusub: Driver Pass cusub ayaa la soo dirsaday eber dhibic labaatan iyo shan dollar. Fadlan xaqiiji!'
            );
            setPaymentToast({
              show: true,
              text: '🔔 LACAG CUSUB: Driver Pass cusub ayaa la soo diray ($0.25)!',
              time: new Date().toLocaleTimeString()
            });
            fetchOverviewMetrics(false);
          }
        })
        .subscribe();

      return () => {
        clearInterval(interval);
        supabase.removeChannel(paymentChannel);
      };
    }
  }, [session, currentAdmin]);

  useEffect(() => {
    if (activeTab === 'users' || activeTab === 'drivers') {
      fetchUsers();
    }
  }, [activeTab]);

  const handleLogout = async () => {
    localStorage.removeItem('gobajaaj_marketing_agent');
    setMarketingAgent(null);
    await supabase.auth.signOut();
    setSession(null);
    setCurrentAdmin(null);
  };

  // 1. Loading Initial State
  if (isAuthChecking && !marketingAgent) {
    return (
      <div className="min-h-screen bg-[#111] flex flex-col items-center justify-center p-4">
        <div className="w-16 h-16 border-4 border-[#E50914] border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-white font-bold text-sm tracking-wider">SHIRI HQ MAAMULKA GUUD...</p>
      </div>
    );
  }

  // 2. Marketer Portal: If logged in as Marketing Staff on Chrome, show dedicated portal
  if (marketingAgent) {
    return <MarketerPortal agent={marketingAgent} onLogout={handleMarketerLogout} />;
  }

  // 3. Unauthenticated Screen
  if (!session || !currentAdmin) {
    return <LoginScreen onLoginSuccess={() => { checkAuthSession(); checkMarketingSession(); }} />;
  }

  // 3. Account Suspended / Disabled Screen
  if (currentAdmin.status === 'suspended' || currentAdmin.status === 'disabled') {
    return (
      <div className="min-h-screen bg-[#111] flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-3xl p-8 text-center shadow-2xl border border-neutral-800">
          <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <AlertTriangle className="w-8 h-8 text-[#E50914]" />
          </div>
          <h2 className="text-xl font-black text-black">Akkawnkaaga waa la Hakiye / Joojiyay</h2>
          <p className="text-xs text-neutral-600 mt-2 leading-relaxed">
            Akkawnkaaga maamul (<strong>{currentAdmin.email}</strong>) waa la hakanay ama ma laha fasax maamul. Fadlan la xiriir Maamulaha Guud (Super Admin).
          </p>
          <button
            onClick={handleLogout}
            className="mt-6 w-full py-3 bg-black hover:bg-neutral-800 text-white font-bold text-xs rounded-xl transition"
          >
            Ka Bax Nidaamka (Logout)
          </button>
        </div>
      </div>
    );
  }

  const role = currentAdmin.role_key;
  const roleDisplay = getRoleDisplay(role);

  const registeredDriversCount = usersList.filter(u => u.role === 'driver' || u.role === 'courier').length;

  // 6 Unified Hubs of SHIRI Super App Platform (Cleaned & Modernized)
  const hubGroups = [
    {
      hubTitle: '1. Command & Dynamic Pricing',
      tabs: [
        { id: 'overview', label: 'Boowdka Guud (Dashboard)', icon: Shield, perm: true },
        { id: 'pricing', label: '⚙️ Qiimaha & Boowdka (Pricing)', icon: Settings, perm: hasPermission(role, 'pricing.view') || hasPermission(role, 'settings.view') || role === 'super_admin' },
        { id: 'analytics', label: 'Aqoonta Xogta (Analytics)', icon: BarChart3, perm: hasPermission(role, 'analytics.view') },
      ]
    },
    {
      hubTitle: '🏪 SHIRI Business & Eats (Maamulka)',
      tabs: [
        { id: 'shiri_business_eats', label: '🏪 SHIRI Business & Eats (New Hub)', icon: Store, perm: true },
        { id: 'merchants', label: '🏪 Ganacsatada & Dukaamada', icon: Store, perm: true },
        { id: 'categories', label: '📦 Qeybaha Ganacsiga (Categories)', icon: Layers, perm: true },
        { id: 'orders', label: '🛍️ Dalabyada Tooska ah (Live Orders)', icon: ShoppingBag, perm: true },
      ]
    },
    {
      hubTitle: '📦 SHIRI Delivery — Gaarsiinta',
      tabs: [
        { id: 'shiri_delivery', label: '📦 SHIRI Delivery Orders & Couriers', icon: Package, perm: true },
      ]
    },
    {
      hubTitle: '3. Gaadiidka & Gaarsiinta (Fleet)',
      tabs: [
        { id: 'drivers', label: '🛺 Darawal Bajaaj ($0.25 Pass)', icon: Bike, count: registeredDriversCount, perm: hasPermission(role, 'users.view') },
        { id: 'couriers', label: '🏍️ Mootooyinka Delivery', icon: Truck, perm: true },
        { id: 'daily_paid', label: 'Ku Shubtay Maanta ($0.25)', icon: Wallet, count: overviewStats.todayApprovedCount, perm: hasPermission(role, 'payments.view') },
      ]
    },
    {
      hubTitle: '4. Maaliyadda & Xisaabaadka (Finance)',
      tabs: [
        { id: 'payments', label: 'Lacagaha Telecom (EVC/eDahab)', icon: CreditCard, perm: hasPermission(role, 'payments.view') },
      ]
    },
    {
      hubTitle: '5. Traffic Ops (Waddooyinka)',
      tabs: [
        { id: 'traffic', label: 'Saxmadda & Jidadka Live', icon: TrafficCone, perm: hasPermission(role, 'traffic.view') },
      ]
    },
    {
      hubTitle: '6. Nidaamka & Amniga (System & Security)',
      tabs: [
        { id: 'users', label: 'Macaamiisha & Diiwaanka', icon: Users, perm: hasPermission(role, 'users.view') },
        { id: 'admins', label: 'Maamulayaasha Nidaamka', icon: Lock, perm: hasPermission(role, 'admins.view') },
        { id: 'audit', label: 'Audit Logs & Amniga', icon: History, perm: hasPermission(role, 'audit_logs.view') },
      ]
    }
  ];

  const allowedTabs = hubGroups.flatMap(group => group.tabs).filter(tab => tab.perm);

  const recentUsersCount = usersList.filter(u => isWithinMinutes(u.created_at, 10)).length;

  const filteredUsers = [...usersList]
    .sort((a, b) => {
      const timeA = a.created_at ? new Date(a.created_at).getTime() : 0;
      const timeB = b.created_at ? new Date(b.created_at).getTime() : 0;
      return timeB - timeA;
    })
    .filter(u => {
      if (userFilter === 'recent10') {
        if (!isWithinMinutes(u.created_at, 10)) return false;
      } else if (userFilter !== 'all') {
        if (u.role !== userFilter) return false;
      }
      if (!userSearchQuery.trim()) return true;
      const q = userSearchQuery.toLowerCase().trim();
      return (
        (u.full_name && u.full_name.toLowerCase().includes(q)) ||
        (u.phone_number && u.phone_number.toLowerCase().includes(q))
      );
    });

  return (
    <div className="min-h-screen bg-neutral-100 font-sans flex flex-col md:flex-row text-black">
      {/* Mobile Backdrop Overlay - closes sidebar on outside click */}
      {isMobileMenuOpen && (
        <div
          onClick={() => setIsMobileMenuOpen(false)}
          className="fixed inset-0 bg-black/70 backdrop-blur-xs z-40 md:hidden transition-opacity cursor-pointer"
          aria-label="Xir Sidebar"
        />
      )}

      {/* Mobile Top Header */}
      <div className="md:hidden bg-black text-white p-3.5 flex items-center justify-between border-b border-neutral-800 sticky top-0 z-30 shadow-md">
        <div className="flex items-center space-x-2.5">
          <img
            src={logoImg}
            alt="SHIRI"
            className="w-8 h-8 rounded-lg object-cover shadow border border-neutral-700"
          />
          <div>
            <span className="font-black text-xs tracking-wider block">SHIRI</span>
            <span className="text-[9px] text-neutral-400 font-bold uppercase tracking-widest block -mt-0.5">Admin Portal</span>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          {overviewStats.pendingPaymentsCount > 0 && (
            <button
              onClick={() => { setActiveTab('payments'); setIsMobileMenuOpen(false); }}
              className="px-2 py-0.5 rounded-full text-[10px] font-black bg-[#E50914] text-white animate-pulse"
            >
              {overviewStats.pendingPaymentsCount} cusub
            </button>
          )}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="p-2 text-white hover:bg-neutral-800 rounded-lg cursor-pointer transition"
            aria-label="Toggle Menu"
          >
            {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>
      </div>

      {/* Sidebar Navigation - Collapsible on Desktop & Drawer on Mobile */}
      <aside className={`
        fixed md:sticky top-0 inset-y-0 left-0 z-50 h-screen bg-black text-white flex flex-col justify-between border-r border-neutral-800 transition-all duration-300 ease-in-out shrink-0
        ${isMobileMenuOpen ? 'translate-x-0 w-72 p-5 shadow-2xl' : '-translate-x-full md:translate-x-0'}
        ${isSidebarCollapsed ? 'md:w-20 md:p-3' : 'md:w-64 md:p-5'}
      `}>
        <div className="space-y-5 overflow-y-auto">
          {/* Logo & Platform Name & Close / Collapse Toggle */}
          <div className="flex items-center justify-between">
            <div className={`flex items-center ${isSidebarCollapsed ? 'justify-center w-full' : 'space-x-3 px-1'}`}>
              <img
                src={logoImg}
                alt="SHIRI"
                onClick={() => isSidebarCollapsed && setIsSidebarCollapsed(false)}
                className="w-10 h-10 rounded-xl object-cover shadow-lg shrink-0 cursor-pointer border border-neutral-700"
                title="SHIRI Admin"
              />
              {!isSidebarCollapsed && (
                <div className="overflow-hidden">
                  <h1 className="font-black text-sm tracking-wider text-white truncate">SHIRI</h1>
                  <p className="text-[9px] text-neutral-400 font-bold uppercase tracking-widest truncate">Admin Portal</p>
                </div>
              )}
            </div>

            {/* Mobile Close Button (X) */}
            <button
              onClick={() => setIsMobileMenuOpen(false)}
              className="md:hidden p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition cursor-pointer"
              title="Xir Menu-ga"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Desktop Collapse Button */}
            {!isSidebarCollapsed && (
              <button
                onClick={() => setIsSidebarCollapsed(true)}
                className="hidden md:flex p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition cursor-pointer"
                title="Xir / Yaree Sidebar (Collapse)"
              >
                <PanelLeftClose className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Desktop Re-expand button when collapsed */}
          {isSidebarCollapsed && (
            <div className="hidden md:flex justify-center">
              <button
                onClick={() => setIsSidebarCollapsed(false)}
                className="p-2 rounded-xl text-neutral-400 hover:text-white hover:bg-neutral-800 transition cursor-pointer"
                title="Fur / Balaari Sidebar (Expand)"
              >
                <PanelLeftOpen className="w-4 h-4 text-[#E50914]" />
              </button>
            </div>
          )}

          {/* Admin User Info Card */}
          <div className={`
            bg-neutral-900 border border-neutral-800 rounded-2xl flex items-center transition-all
            ${isSidebarCollapsed ? 'p-2 justify-center' : 'p-3 space-x-3'}
          `}>
            <div
              className="w-9 h-9 rounded-xl bg-[#E50914]/20 border border-[#E50914]/40 text-[#E50914] flex items-center justify-center font-black text-xs shrink-0"
              title={`${currentAdmin.full_name} (${roleDisplay.name})`}
            >
              {currentAdmin.full_name.charAt(0)}
            </div>
            {!isSidebarCollapsed && (
              <div className="overflow-hidden">
                <p className="font-bold text-xs text-white truncate">{currentAdmin.full_name}</p>
                <span className={`inline-block mt-0.5 px-2 py-0.5 rounded text-[9px] font-black uppercase ${roleDisplay.bg} ${roleDisplay.text}`}>
                  {roleDisplay.name}
                </span>
              </div>
            )}
          </div>

          {/* Nav List Grouped by 6 Hubs */}
          <nav className="space-y-3 pt-1">
            {hubGroups.map((group) => {
              const activeGroupTabs = group.tabs.filter(t => t.perm);
              if (activeGroupTabs.length === 0) return null;

              return (
                <div key={group.hubTitle} className="space-y-1">
                  {!isSidebarCollapsed && (
                    <p className="text-[10px] font-black uppercase text-neutral-500 px-3 pt-2 pb-0.5 tracking-wider">
                      {group.hubTitle}
                    </p>
                  )}
                  {activeGroupTabs.map((tab) => {
                    const Icon = tab.icon;
                    const isActive = activeTab === tab.id;
                    const hasPending = tab.id === 'payments' && overviewStats.pendingPaymentsCount > 0;

                    return (
                      <button
                        key={tab.id}
                        onClick={() => {
                          setActiveTab(tab.id);
                          setIsMobileMenuOpen(false);
                        }}
                        title={tab.label}
                        className={`
                          w-full flex items-center rounded-xl text-xs font-bold transition cursor-pointer
                          ${isSidebarCollapsed
                            ? 'justify-center p-3 relative'
                            : 'justify-between px-3.5 py-2.5'
                          }
                          ${isActive
                            ? 'bg-[#DC2626] text-white shadow-md'
                            : 'text-neutral-400 hover:text-white hover:bg-neutral-900'
                          }
                        `}
                      >
                        <div className={`flex items-center ${isSidebarCollapsed ? 'justify-center' : 'space-x-3'}`}>
                          <Icon className="w-4 h-4 shrink-0" />
                          {!isSidebarCollapsed && <span>{tab.label}</span>}
                        </div>

                        {/* Badges / Indicators */}
                        {!isSidebarCollapsed ? (
                          <div className="flex items-center space-x-1.5">
                            {hasPending && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-[#DC2626] text-white animate-pulse border border-white/20">
                                {overviewStats.pendingPaymentsCount} cusub
                              </span>
                            )}
                            {tab.id === 'daily_paid' && overviewStats.todayApprovedCount > 0 && !hasPending && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-emerald-600 text-white border border-emerald-400">
                                {overviewStats.todayApprovedCount} maanta
                              </span>
                            )}
                            {tab.id === 'drivers' && registeredDriversCount > 0 && (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-neutral-800 text-neutral-300 border border-neutral-700">
                                {registeredDriversCount}
                              </span>
                            )}
                            {isActive && <ChevronRight className="w-4 h-4" />}
                          </div>
                        ) : (
                          (hasPending || (tab.id === 'daily_paid' && overviewStats.todayApprovedCount > 0)) && (
                            <span className={`absolute top-1.5 right-1.5 w-2.5 h-2.5 rounded-full border-2 border-black ${hasPending ? 'bg-[#DC2626] animate-pulse' : 'bg-emerald-500'}`} />
                          )
                        )}
                      </button>
                    );
                  })}
                </div>
              );
            })}
          </nav>
        </div>

        {/* Logout Button & Bottom Controls */}
        <div className="pt-3 border-t border-neutral-800 space-y-2">
          {!isSidebarCollapsed && (
            <button
              onClick={() => setIsSidebarCollapsed(true)}
              className="hidden md:flex w-full items-center space-x-2 px-3 py-1.5 rounded-lg text-[11px] font-semibold text-neutral-500 hover:text-neutral-300 hover:bg-neutral-900 transition cursor-pointer"
            >
              <PanelLeftClose className="w-3.5 h-3.5" />
              <span>Yaree Sidebar-ka (Collapse)</span>
            </button>
          )}

          <button
            onClick={handleLogout}
            title="Ka Bax Nidaamka (Logout)"
            className={`
              w-full flex items-center rounded-xl text-xs font-bold text-neutral-400 hover:text-red-400 hover:bg-red-950/30 transition cursor-pointer
              ${isSidebarCollapsed ? 'justify-center p-3' : 'space-x-3 px-3.5 py-3'}
            `}
          >
            <LogOut className="w-4 h-4 text-red-500 shrink-0" />
            {!isSidebarCollapsed && <span>Ka Bax (Logout)</span>}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 p-3.5 sm:p-5 md:p-8 overflow-y-auto min-w-0">
        {/* Top Breadcrumb & Quick Actions Header */}
        <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 sm:gap-4 mb-6 md:mb-8">
          <div className="flex items-center space-x-3">
            {/* Desktop Quick Toggle for Sidebar */}
            <button
              onClick={() => setIsSidebarCollapsed(!isSidebarCollapsed)}
              className="hidden md:flex p-2.5 rounded-xl border border-neutral-300 bg-white hover:bg-neutral-100 text-neutral-700 shadow-xs transition items-center justify-center cursor-pointer"
              title={isSidebarCollapsed ? "Balaari Sidebar-ka (Expand)" : "Yaree Sidebar-ka (Collapse)"}
            >
              {isSidebarCollapsed ? <PanelLeftOpen className="w-4 h-4 text-[#E50914]" /> : <PanelLeftClose className="w-4 h-4" />}
            </button>

            <div>
              <h1 className="text-xl md:text-2xl font-black text-black capitalize">
                {allowedTabs.find(t => t.id === activeTab)?.label || 'Dashboard'}
              </h1>
              <p className="text-xs text-neutral-500 mt-0.5">
                SHIRI • Nidaamka Mideysan ee Gaadiidka, Gaarsiinta & Saxmadda Muqdisho
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2 sm:space-x-3">
            <div className="px-3 py-1.5 bg-white border border-neutral-200 rounded-xl text-xs font-bold text-neutral-700 flex items-center space-x-2 shadow-xs">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shrink-0"></span>
              <span className="truncate">Nidaamka Tooska ah (Online)</span>
            </div>
          </div>
        </header>

        {/* Global Payment Sound / Toast Notification Banner */}
        {paymentToast && (
          <div className="mb-5 p-3.5 bg-gradient-to-r from-emerald-600 via-teal-600 to-green-600 text-white rounded-2xl shadow-lg flex items-center justify-between animate-fadeIn">
            <div className="flex items-center space-x-3 text-xs sm:text-sm font-black">
              <Bell className="w-5 h-5 text-amber-300 animate-bounce shrink-0" />
              <span>{paymentToast.text} ({paymentToast.time})</span>
            </div>
            <button
              onClick={() => { setActiveTab('payments'); setPaymentToast(null); }}
              className="px-3 py-1.5 bg-white text-emerald-900 rounded-xl text-xs font-black shadow hover:bg-neutral-100 transition cursor-pointer shrink-0"
            >
              Eeg Lacagaha →
            </button>
          </div>
        )}

        {/* Tab Switcher Body */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* Overview Stats Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3 sm:gap-4">
              {/* 1. Pending Payments Action Card */}
              <div
                onClick={() => setActiveTab('payments')}
                className="bg-red-50 hover:bg-red-100/70 p-5 rounded-2xl border-2 border-[#E50914] shadow-xs cursor-pointer transition"
              >
                <div className="flex items-center justify-between text-red-700 mb-2">
                  <span className="text-xs font-black uppercase tracking-wider">Lacagaha Sugaya</span>
                  <span className="px-2 py-0.5 rounded-full bg-[#E50914] text-white text-[10px] font-black animate-pulse">Action</span>
                </div>
                <p className="text-2xl sm:text-3xl font-black text-[#E50914]">
                  {isLoadingOverview ? '...' : overviewStats.pendingPaymentsCount}
                </p>
                <p className="text-xs text-red-600 font-bold mt-1">Ansixi / Diid →</p>
              </div>

              {/* 2. Dakhliga Guud (Total Revenue) */}
              <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
                <div className="flex items-center justify-between text-neutral-500 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Dakhliga Guud</span>
                  <span className="text-xs font-black text-emerald-600">USD</span>
                </div>
                <p className="text-2xl sm:text-3xl font-black text-emerald-600">
                  {isLoadingOverview ? '...' : `$${overviewStats.revenue.toFixed(2)}`}
                </p>
                <p className="text-xs text-neutral-400 mt-1">Wadarta la ansixiyay</p>
              </div>

              {/* 3. Dakhliga Maanta (Daily Revenue) */}
              <div className="bg-gradient-to-br from-emerald-50 to-white p-5 rounded-2xl border-2 border-emerald-500/50 shadow-xs">
                <div className="flex items-center justify-between text-emerald-800 mb-2">
                  <span className="text-xs font-black uppercase tracking-wider">Dakhliga Maanta</span>
                  <span className="px-1.5 py-0.5 rounded bg-emerald-600 text-white text-[9px] font-black">Maalinle</span>
                </div>
                <p className="text-2xl sm:text-3xl font-black text-emerald-700">
                  {isLoadingOverview ? '...' : `$${overviewStats.dailyRevenue.toFixed(2)}`}
                </p>
                <p className="text-xs text-emerald-600 font-bold mt-1">
                  🟢 {overviewStats.todayApprovedCount} bixin maanta
                </p>
              </div>

              {/* 4. Active Driver Pass */}
              <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
                <div className="flex items-center justify-between text-neutral-500 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Driver Pass Active</span>
                  <CreditCard className="w-5 h-5 text-emerald-500" />
                </div>
                <p className="text-2xl sm:text-3xl font-black text-black">
                  {isLoadingOverview ? '...' : overviewStats.subscribersCount}
                </p>
                <p className="text-xs text-neutral-400 mt-1">$0.25/Shift (05:00 AM)</p>
              </div>

              {/* 5. Macaamiisha Buuxda */}
              <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
                <div className="flex items-center justify-between text-neutral-500 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Macaamiisha</span>
                  <Users className="w-5 h-5 text-neutral-400" />
                </div>
                <p className="text-2xl sm:text-3xl font-black text-black">
                  {isLoadingOverview ? '...' : overviewStats.usersCount}
                </p>
                <p className="text-xs text-neutral-400 mt-1">Darawallo & Macaamiil</p>
              </div>

              {/* 6. Isgoysyada Muqdisho */}
              <div
                onClick={() => setActiveTab('traffic')}
                className="bg-white hover:bg-neutral-50 p-5 rounded-2xl border border-neutral-200 shadow-xs cursor-pointer transition"
              >
                <div className="flex items-center justify-between text-neutral-500 mb-2">
                  <span className="text-xs font-bold uppercase tracking-wider">Isgoysyada</span>
                  <TrafficCone className="w-5 h-5 text-[#E50914]" />
                </div>
                <p className="text-2xl sm:text-3xl font-black text-black">
                  {isLoadingOverview ? '...' : overviewStats.intersectionsCount}
                </p>
                <p className="text-xs text-emerald-600 font-bold mt-1">
                  🟢 {overviewStats.reportersCount} Wariye
                </p>
              </div>
            </div>

            {/* 10-Minute Recent Activity Live Banner */}
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 shadow-xs">
              <div className="flex items-center space-x-3">
                <span className="relative flex h-3.5 w-3.5 shrink-0">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3.5 w-3.5 bg-emerald-600"></span>
                </span>
                <div>
                  <div className="flex items-center space-x-2">
                    <p className="text-sm font-black text-emerald-950">
                      Dhaqdhaqaaqa 10-Kii Daqiiqo ee Ugu Dambeeyay (Live Activity)
                    </p>
                    <span className="px-2 py-0.5 rounded-full bg-emerald-200/80 text-emerald-900 text-[10px] font-black border border-emerald-400">
                      Toos (Live)
                    </span>
                  </div>
                  <p className="text-xs text-emerald-800 mt-0.5">
                    <strong>{overviewStats.recentRegistrationsCount}</strong> qof oo cusub oo is-diiwaangeliyay • <strong>{overviewStats.recentPaymentsCount}</strong> bixin lacageed oo soo gashay 10-kii daqiiqo ee u dambeysay.
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-2 shrink-0">
                <button
                  onClick={() => setActiveTab('payments')}
                  className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition shadow flex items-center space-x-1.5 cursor-pointer"
                >
                  <CreditCard className="w-3.5 h-3.5" />
                  <span>Eeg Lacagaha ({overviewStats.recentPaymentsCount})</span>
                </button>
                <button
                  onClick={() => setActiveTab('users')}
                  className="px-3.5 py-1.5 bg-white border border-emerald-300 hover:bg-emerald-100 text-emerald-900 text-xs font-bold rounded-xl transition shadow-xs flex items-center space-x-1.5 cursor-pointer"
                >
                  <Users className="w-3.5 h-3.5" />
                  <span>Eeg Diiwaangelinta ({overviewStats.recentRegistrationsCount})</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'users' && (
          <div className="space-y-6">
            {/* Demo Data Cleanup Section */}
            {hasPermission(role, 'users.delete') && (
              <div className="bg-red-50 p-4 sm:p-5 rounded-2xl border-2 border-dashed border-red-300 shadow-sm">
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
                  <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center shrink-0">
                      <Trash2 className="w-5 h-5 text-red-500" />
                    </div>
                    <div>
                      <h3 className="text-sm font-black text-red-800">🧹 Tirtir Demo Data (Tijaabada)</h3>
                      <p className="text-xs text-red-600 mt-0.5">
                        Ku tirtir dhammaan darawallada & rakaabka tijaabada ah database-ka — hal gujis!
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setIsDemoCleanupOpen(true)}
                    className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-black shadow-lg transition flex items-center justify-center space-x-2 cursor-pointer shrink-0"
                  >
                    <Trash2 className="w-4 h-4" />
                    <span>Tirtir Demo Data</span>
                  </button>
                </div>
                {demoCleanupResult && (
                  <div className={`mt-3 p-3 rounded-xl text-xs font-bold ${demoCleanupResult.startsWith('✅') ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' : demoCleanupResult.startsWith('⚠️') ? 'bg-amber-100 text-amber-800 border border-amber-300' : 'bg-red-100 text-red-800 border border-red-300'}`}>
                    {demoCleanupResult}
                  </div>
                )}
              </div>
            )}

            {/* Demo Cleanup Confirmation Modal */}
            {isDemoCleanupOpen && (
              <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md border border-red-200 p-6 text-center">
                  <div className="w-16 h-16 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
                    <AlertTriangle className="w-8 h-8 text-red-500" />
                  </div>
                  <h3 className="text-lg font-black text-black mb-2">⚠️ Tirtirista Demo Data Xaqiiji</h3>
                  <div className="bg-red-50 rounded-xl p-4 mb-4 text-left">
                    <p className="text-xs font-bold text-red-800 mb-2">Waxaa la tirtiri doonaa:</p>
                    <ul className="text-xs text-red-700 space-y-1">
                      <li>🚗 Dhammaan <strong>darawallada</strong> diiwaangashan ({usersList.filter(u => u.role === 'driver').length} qof)</li>
                      <li>👤 Dhammaan <strong>rakaabka/macaamiisha</strong> ({usersList.filter(u => u.role === 'passenger').length} qof)</li>
                      <li>💳 Dhammaan lacag-bixinnadooda & Driver Pass-yada</li>
                      <li>🛺 Dhammaan safaradooda (rides)</li>
                    </ul>
                  </div>
                  <p className="text-xs text-red-600 font-bold mb-5">
                    ⛔ Ficilkan looma soo celin karo! Kaliya u isticmaal marka demo/tijaabo la dhameeyay.
                  </p>
                  <div className="flex space-x-3">
                    <button
                      onClick={() => { setIsDemoCleanupOpen(false); setDemoCleanupResult(null); }}
                      className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-black text-sm font-bold rounded-xl transition cursor-pointer"
                    >
                      Ka noqo
                    </button>
                    <button
                      onClick={() => { setIsDemoCleanupOpen(false); handleDeleteDemoData(); }}
                      disabled={isDemoCleanupProcessing}
                      className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white text-sm font-black rounded-xl transition cursor-pointer disabled:opacity-60 flex items-center justify-center space-x-2"
                    >
                      {isDemoCleanupProcessing ? (
                        <><RefreshCw className="w-4 h-4 animate-spin" /><span>Tirtirayaa...</span></>
                      ) : (
                        <><Trash2 className="w-4 h-4" /><span>Haa, Tirtir Dhammaan!</span></>
                      )}
                    </button>
                  </div>
                </div>
              </div>
            )}

            <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-black">Macaamiisha & Darawallada ({usersList.length})</h2>
                <p className="text-xs text-neutral-500 mt-0.5">Liiska dhammaan isticmaalayaasha SHIRI (Kuwa cusub waxay ku jiraan dusha sare).</p>
              </div>
              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 absolute left-3 top-3 text-neutral-400" />
                <input
                  type="text"
                  placeholder="Raadi magac ama taleefan..."
                  value={userSearchQuery}
                  onChange={(e) => setUserSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-xl text-xs text-black focus:outline-none focus:bg-white"
                />
              </div>
            </div>

            {/* Filter Tabs for Users */}
            <div className="flex items-center space-x-2 overflow-x-auto pb-1">
              <button
                onClick={() => setUserFilter('all')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${userFilter === 'all'
                  ? 'bg-black text-white'
                  : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
                  }`}
              >
                Dhammaan ({usersList.length})
              </button>

              <button
                onClick={() => setUserFilter('recent10')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0 ${userFilter === 'recent10'
                  ? 'bg-emerald-600 text-white shadow'
                  : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200'
                  }`}
              >
                <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                <span>10-Kii Daqiiqo ee Ugu Dambeeyay ({recentUsersCount})</span>
              </button>

              <button
                onClick={() => setUserFilter('driver')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${userFilter === 'driver'
                  ? 'bg-black text-white'
                  : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
                  }`}
              >
                Darawallada
              </button>

              <button
                onClick={() => setUserFilter('user')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${userFilter === 'user'
                  ? 'bg-black text-white'
                  : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
                  }`}
              >
                Rakaabka
              </button>
            </div>

            <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
              {isLoadingUsers ? (
                <div className="p-12 text-center text-neutral-500">
                  <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
                  <p className="text-sm font-semibold">Soo qaadaya macaamiisha...</p>
                </div>
              ) : filteredUsers.length === 0 ? (
                <div className="p-12 text-center">
                  <Users className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
                  <h3 className="text-base font-bold text-black">Xog lama helin</h3>
                  <p className="text-xs text-neutral-500 mt-1">Ma jiro macmiil ku jira shuruudahan.</p>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm min-w-[650px]">
                    <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                      <tr>
                        <th className="p-4 font-bold">Magaca Buuxa</th>
                        <th className="p-4 font-bold">Taleefanka</th>
                        <th className="p-4 font-bold">Role</th>
                        <th className="p-4 font-bold">Akoonka</th>
                        <th className="p-4 font-bold">Driver Pass ($0.25)</th>
                        <th className="p-4 font-bold">Waqtiga</th>
                        <th className="p-4 font-bold text-right">Ficil (Action)</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100">
                      {filteredUsers.map(u => {
                        const isRecent = isWithinMinutes(u.created_at, 10);
                        const isDriver = u.role === 'driver';
                        const isPassActive = u.subscription_status === 'active';
                        const isApproving = approvingUserId === u.id;

                        return (
                          <tr
                            key={u.id}
                            className={`transition ${isRecent
                              ? 'bg-emerald-50/80 hover:bg-emerald-100/70 border-l-4 border-l-emerald-500'
                              : 'hover:bg-neutral-50'
                              }`}
                          >
                            <td className="p-4">
                              <div className="flex items-center space-x-2">
                                <span className="font-bold text-black">{u.full_name}</span>
                                {isRecent && (
                                  <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-emerald-200/90 text-emerald-900 text-[10px] font-black border border-emerald-400 animate-pulse">
                                    <Sparkles className="w-3 h-3 text-emerald-700" />
                                    CUSUB (10 min)
                                  </span>
                                )}
                              </div>
                            </td>
                            <td className="p-4 text-xs font-mono font-bold text-black">{u.phone_number}</td>
                            <td className="p-4">
                              <span className="px-2.5 py-1 rounded-lg text-xs font-bold bg-neutral-100 text-neutral-800 uppercase">
                                {u.role}
                              </span>
                            </td>
                            <td className="p-4">
                              <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${u.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                                }`}>
                                {u.status}
                              </span>
                            </td>
                            <td className="p-4">
                              {isDriver ? (
                                isPassActive ? (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-300">
                                    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                                    ACTIVE (Furan)
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold bg-amber-100 text-amber-800 border border-amber-300 animate-pulse">
                                    <Clock className="w-3.5 h-3.5 text-amber-600" />
                                    SUGAYSA (Pending)
                                  </span>
                                )
                              ) : (
                                <span className="text-xs text-neutral-400">—</span>
                              )}
                            </td>
                            <td className="p-4">
                              <p className={`text-xs font-mono font-bold ${isRecent ? 'text-emerald-700' : 'text-neutral-700'}`}>
                                {formatTimeAgoSomali(u.created_at)}
                              </p>
                              <p className="text-[10px] font-mono text-neutral-400 mt-0.5">
                                {formatExactDateTime(u.created_at)}
                              </p>
                            </td>
                            <td className="p-4 text-right">
                              <div className="flex items-center justify-end space-x-1.5">
                                {isDriver && !isPassActive && (
                                  <button
                                    onClick={() => handleQuickApprovePass(u)}
                                    disabled={isApproving}
                                    className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1 cursor-pointer"
                                  >
                                    {isApproving ? (
                                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                    ) : (
                                      <CheckCircle2 className="w-3.5 h-3.5" />
                                    )}
                                    <span>{isApproving ? 'Ansixinaya...' : 'Ansixi'}</span>
                                  </button>
                                )}
                                {hasPermission(role, 'users.suspend') && (
                                  u.status === 'suspended' ? (
                                    <button
                                      onClick={() => setUserActionModal({ isOpen: true, user: u, action: 'activate', isProcessing: false })}
                                      className="p-1.5 hover:bg-emerald-50 text-emerald-500 hover:text-emerald-700 rounded-lg transition cursor-pointer"
                                      title="Fur Akoonka"
                                    >
                                      <ShieldCheck className="w-4 h-4" />
                                    </button>
                                  ) : (
                                    <button
                                      onClick={() => setUserActionModal({ isOpen: true, user: u, action: 'suspend', isProcessing: false })}
                                      className="p-1.5 hover:bg-amber-50 text-amber-500 hover:text-amber-700 rounded-lg transition cursor-pointer"
                                      title="Haki Akoonka"
                                    >
                                      <ShieldOff className="w-4 h-4" />
                                    </button>
                                  )
                                )}
                                {hasPermission(role, 'users.delete') && (
                                  <button
                                    onClick={() => setUserActionModal({ isOpen: true, user: u, action: 'delete', isProcessing: false })}
                                    className="p-1.5 hover:bg-red-50 text-red-400 hover:text-red-600 rounded-lg transition cursor-pointer"
                                    title="Tirtir Akoonka"
                                  >
                                    <Trash2 className="w-4 h-4" />
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* User Action Confirmation Modal */}
        {userActionModal.isOpen && userActionModal.user && (
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm border border-neutral-200 p-6 text-center">
              <div className={`w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-4 ${userActionModal.action === 'delete' ? 'bg-red-100' : userActionModal.action === 'suspend' ? 'bg-amber-100' : 'bg-emerald-100'
                }`}>
                {userActionModal.action === 'delete'
                  ? <Trash2 className="w-7 h-7 text-red-500" />
                  : userActionModal.action === 'suspend'
                    ? <ShieldOff className="w-7 h-7 text-amber-500" />
                    : <ShieldCheck className="w-7 h-7 text-emerald-500" />}
              </div>
              <h3 className="text-base font-black text-black mb-2">
                {userActionModal.action === 'delete' ? 'Tirtirista Xaqiiji' : userActionModal.action === 'suspend' ? 'Hakinta Xaqiiji' : 'Furista Xaqiiji'}
              </h3>
              <p className="text-xs text-neutral-600 mb-2">
                Shaqaalaha: <strong>{userActionModal.user.full_name}</strong><br />
                Taleefanka: <span className="font-mono">{userActionModal.user.phone_number}</span>
              </p>
              <p className="text-xs text-neutral-600 mb-6">
                Ma hubtaa inaad rabto inaad <strong>{userActionModal.action === 'delete' ? 'tirtirto' : userActionModal.action === 'suspend' ? 'hakiso' : 'furto'}</strong> xisaabtan?
                {userActionModal.action === 'delete' && <span className="text-red-600 block mt-1 font-bold">Ficilkan looma soo celin karo.</span>}
              </p>
              <div className="flex space-x-3">
                <button
                  onClick={() => setUserActionModal({ isOpen: false, user: null, action: 'delete', isProcessing: false })}
                  className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-black text-sm font-bold rounded-xl transition cursor-pointer"
                >
                  Ka noqo
                </button>
                <button
                  onClick={handleUserAction}
                  disabled={userActionModal.isProcessing}
                  className={`flex-1 py-2.5 text-white text-sm font-black rounded-xl transition cursor-pointer disabled:opacity-60 ${userActionModal.action === 'delete' ? 'bg-red-600 hover:bg-red-700' : userActionModal.action === 'suspend' ? 'bg-amber-500 hover:bg-amber-600' : 'bg-emerald-600 hover:bg-emerald-700'
                    }`}
                >
                  {userActionModal.isProcessing ? 'Socda...' : 'Haa, Xaqiiji'}
                </button>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'shiri_business_eats' && (
          <ShiriBusinessEats
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'merchants.manage') || role === 'super_admin'}
          />
        )}

        {activeTab === 'shiri_delivery' && (
          <ShiriDeliveryManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'users.update') || role === 'super_admin'}
          />
        )}

        {activeTab === 'pricing' && (
          <PricingControlCenter
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'pricing.update') || role === 'super_admin'}
          />
        )}

        {activeTab === 'merchants' && (
          <MerchantsManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'merchants.manage') || role === 'super_admin'}
          />
        )}

        {activeTab === 'categories' && (
          <CategoriesManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'settings.update') || role === 'super_admin'}
          />
        )}

        {activeTab === 'orders' && (
          <LiveOrdersManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'orders.manage') || role === 'super_admin'}
          />
        )}

        {activeTab === 'couriers' && (
          <CouriersManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'users.update') || role === 'super_admin'}
          />
        )}

        {activeTab === 'drivers' && (
          <DriversManagement
            canManage={hasPermission(role, 'users.update')}
            onNavigateToDailyPaid={() => setActiveTab('daily_paid')}
          />
        )}

        {activeTab === 'daily_paid' && (
          <DailyRechargedManagement
            actorName={currentAdmin.full_name}
            canManage={hasPermission(role, 'payments.approve')}
            onNavigateToDrivers={() => setActiveTab('drivers')}
          />
        )}

        {activeTab === 'payments' && (
          <PaymentManagement actorName={currentAdmin.full_name} canManage={hasPermission(role, 'payments.approve')} />
        )}

        {activeTab === 'traffic' && (
          <TrafficManagement actorName={currentAdmin.full_name} canManage={hasPermission(role, 'traffic.update')} />
        )}

        {activeTab === 'analytics' && (
          <AnalyticsDashboard isAnalystOnly={role === 'analyst'} />
        )}

        {activeTab === 'admins' && (
          <AdminManagement currentAdminRole={role} actorName={currentAdmin.full_name} />
        )}

        {activeTab === 'audit' && (
          <AuditLogsView />
        )}
      </main>
    </div>
  );
}
