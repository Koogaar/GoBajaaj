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
  Bell
} from 'lucide-react';
import { supabase } from './supabase';
import { soundAlert } from './utils/soundAlert';
import { AdminUser, Profile, AdminRole } from './types';
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

export default function App() {
  const [session, setSession] = useState<any>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);
  const [currentAdmin, setCurrentAdmin] = useState<AdminUser | null>(null);
  
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
        // Fetch Admin profile from admin_users or profiles
        let { data: adminData } = await supabase
          .from('admin_users')
          .select('*')
          .or(`user_id.eq.${currentSession.user.id},email.eq.${userEmail}`)
          .maybeSingle();

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
            // Default Super Admin for root workspace session if no record exists
            setCurrentAdmin({
              id: currentSession.user.id,
              user_id: currentSession.user.id,
              email: currentSession.user.email || 'superadmin@somalitraffic.so',
              full_name: currentSession.user.email?.split('@')[0] || 'Super Admin',
              role_key: 'super_admin',
              status: 'active',
              created_at: new Date().toISOString()
            });
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
      const now = new Date();
      let expiry = new Date(now);
      if (now.getHours() < 5) {
        expiry.setHours(5, 0, 0, 0);
      } else {
        expiry.setDate(expiry.getDate() + 1);
        expiry.setHours(5, 0, 0, 0);
      }

      // 1. Update Profile
      await supabase
        .from('profiles')
        .update({
          subscription_status: 'active',
          subscription_expiry: expiry.toISOString()
        })
        .eq('id', user.id);

      // 2. Upsert Subscription
      await supabase.from('subscriptions').insert([{
        user_id: user.id,
        plan_name: 'Driver Pass (Maalinle 05:00 AM)',
        amount: 0.25,
        currency: 'USD',
        status: 'active',
        payment_method: 'EVC Plus',
        payment_reference: `PASS_${user.phone_number?.slice(-4) || '2026'}_${Date.now() % 10000}`,
        start_date: now.toISOString(),
        end_date: expiry.toISOString()
      }]);

      // 3. Insert or update completed payment
      await supabase.from('payment_transactions').insert([{
        user_id: user.id,
        amount: 0.25,
        currency: 'USD',
        payment_method: 'EVC Plus',
        reference_number: `TX_${user.phone_number?.slice(-4) || '2026'}_${Date.now() % 10000}`,
        phone_number: user.phone_number || '',
        status: 'completed'
      }]);

      soundAlert.playPaymentSound();
      alert(`Guul! Darawalka ${user.full_name || user.phone_number} waa la ansixiyay, adeeggiisuna hadda waa Active!`);
      await fetchUsers();
      await fetchOverviewMetrics(false);
    } catch (err: any) {
      alert('Cilad: ' + err?.message);
    } finally {
      setApprovingUserId(null);
    }
  };

  useEffect(() => {
    if (session && currentAdmin) {
      fetchOverviewMetrics(true);

      const interval = setInterval(() => {
        fetchOverviewMetrics(false);
      }, 10000);

      // Global Realtime listener for incoming payments to play Cash Chime
      const paymentChannel = supabase
        .channel('global_payment_alerts')
        .on('postgres_changes', { event: '*', schema: 'public', table: 'payment_transactions' }, (payload: any) => {
          if (payload?.eventType === 'INSERT') {
            soundAlert.playPaymentSound();
            const phone = payload.new?.phone_number || '';
            const amt = payload.new?.amount || '0.25';
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
            soundAlert.playPaymentSound();
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
    if (activeTab === 'users') {
      fetchUsers();
    }
  }, [activeTab]);

  const handleLogout = async () => {
    await supabase.auth.signOut();
    setSession(null);
    setCurrentAdmin(null);
  };

  // 1. Loading Initial State
  if (isAuthChecking) {
    return (
      <div className="min-h-screen bg-[#111] flex flex-col items-center justify-center p-4">
        <div className="w-16 h-16 border-4 border-[#E50914] border-t-transparent rounded-full animate-spin mb-4"></div>
        <p className="text-white font-bold text-sm tracking-wider">GOBAJAAJ MAAMULKA GUUD...</p>
      </div>
    );
  }

  // 2. Unauthenticated Screen
  if (!session || !currentAdmin) {
    return <LoginScreen onLoginSuccess={checkAuthSession} />;
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

  // Dynamic Allowed Navigation Tabs based on RBAC Permissions
  const allowedTabs = [
    { id: 'overview', label: 'Tusmada Guud', icon: Shield, perm: true },
    { id: 'ride_support', label: 'GoBajaaj & Cabashooyinka', icon: Bike, perm: hasPermission(role, 'ride.view') || hasPermission(role, 'support.view') || true },
    { id: 'device_support', label: 'Taageerada & Taleefannada Lumay', icon: Headphones, perm: hasPermission(role, 'support.view') || hasPermission(role, 'protection.view') || true },
    { id: 'users', label: 'Macaamiisha', icon: Users, perm: hasPermission(role, 'users.view') },
    { id: 'payments', label: 'Lacagaha ($0.25 Pass)', icon: CreditCard, perm: hasPermission(role, 'payments.view') },
    { id: 'traffic', label: 'Saxmadda & Isgoysyada', icon: TrafficCone, perm: hasPermission(role, 'traffic.view') },
    { id: 'reporters', label: 'Shaqaalaha & Wariyeyaasha', icon: UserCheck, perm: hasPermission(role, 'reporters.view') },
    { id: 'analytics', label: 'Aqoonta Xogta (Analytics)', icon: BarChart3, perm: hasPermission(role, 'analytics.view') },
    { id: 'admins', label: 'Maamulayaasha', icon: Lock, perm: hasPermission(role, 'admins.view') },
    { id: 'audit', label: 'Audit Logs', icon: History, perm: hasPermission(role, 'audit_logs.view') },
  ].filter(tab => tab.perm);

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
            alt="GoBajaaj" 
            className="w-8 h-8 rounded-lg object-cover shadow border border-neutral-700" 
          />
          <div>
            <span className="font-black text-xs tracking-wider block">GOBAJAAJ</span>
            <span className="text-[9px] text-[#E50914] font-bold uppercase tracking-widest block -mt-0.5">Admin Portal</span>
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
                alt="GoBajaaj" 
                onClick={() => isSidebarCollapsed && setIsSidebarCollapsed(false)}
                className="w-10 h-10 rounded-xl object-cover shadow-lg shrink-0 cursor-pointer border border-neutral-700"
                title="GoBajaaj Admin"
              />
              {!isSidebarCollapsed && (
                <div className="overflow-hidden">
                  <h1 className="font-black text-sm tracking-wider text-white truncate">GOBAJAAJ</h1>
                  <p className="text-[9px] text-[#E50914] font-bold uppercase tracking-widest truncate">Maamulka Guud</p>
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

          {/* Nav List */}
          <nav className="space-y-1.5 pt-1">
            {allowedTabs.map((tab) => {
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
                      : 'justify-between px-3.5 py-3'
                    }
                    ${isActive 
                      ? 'bg-[#E50914] text-white shadow-md' 
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
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-[#E50914] text-white animate-pulse border border-white/20">
                          {overviewStats.pendingPaymentsCount} cusub
                        </span>
                      )}
                      {isActive && <ChevronRight className="w-4 h-4" />}
                    </div>
                  ) : (
                    hasPending && (
                      <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 rounded-full bg-[#E50914] border-2 border-black animate-pulse" />
                    )
                  )}
                </button>
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
                GoBajaaj • Nidaamka Saxmadda & Gaadiidka Muqdisho
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
            <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-black">Macaamiisha & Darawallada ({usersList.length})</h2>
                <p className="text-xs text-neutral-500 mt-0.5">Liiska dhammaan isticmaalayaasha GoBajaaj (Kuwa cusub waxay ku jiraan dusha sare).</p>
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
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
                  userFilter === 'all'
                    ? 'bg-black text-white'
                    : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
                }`}
              >
                Dhammaan ({usersList.length})
              </button>

              <button
                onClick={() => setUserFilter('recent10')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0 ${
                  userFilter === 'recent10'
                    ? 'bg-emerald-600 text-white shadow'
                    : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200'
                }`}
              >
                <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
                <span>10-Kii Daqiiqo ee Ugu Dambeeyay ({recentUsersCount})</span>
              </button>

              <button
                onClick={() => setUserFilter('driver')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
                  userFilter === 'driver'
                    ? 'bg-black text-white'
                    : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
                }`}
              >
                Darawallada
              </button>

              <button
                onClick={() => setUserFilter('user')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
                  userFilter === 'user'
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
                            className={`transition ${
                              isRecent 
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
                              <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                                u.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
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
                              {isDriver && !isPassActive && (
                                <button
                                  onClick={() => handleQuickApprovePass(u)}
                                  disabled={isApproving}
                                  className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1.5 ml-auto cursor-pointer"
                                >
                                  {isApproving ? (
                                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                  ) : (
                                    <CheckCircle2 className="w-3.5 h-3.5" />
                                  )}
                                  <span>{isApproving ? 'Ansixinaya...' : '✓ Ansixi Pass'}</span>
                                </button>
                              )}
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

        {activeTab === 'ride_support' && (
          <RideSupportManagement />
        )}

        {activeTab === 'device_support' && (
          <DeviceSupportManagement />
        )}

        {activeTab === 'payments' && (
          <PaymentManagement actorName={currentAdmin.full_name} canManage={hasPermission(role, 'payments.approve')} />
        )}

        {activeTab === 'traffic' && (
          <TrafficManagement actorName={currentAdmin.full_name} canManage={hasPermission(role, 'traffic.update')} />
        )}

        {activeTab === 'reporters' && (
          <ReporterManagement actorName={currentAdmin.full_name} canManage={hasPermission(role, 'reporters.create')} />
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
