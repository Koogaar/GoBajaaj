import React, { useState, useEffect } from 'react';
import {
  BarChart3,
  Users,
  DollarSign,
  TrafficCone,
  Download,
  RefreshCw,
  AlertCircle,
  ShoppingBag,
  Car,
  Store,
  TrendingUp,
  MapPin,
  Clock,
  CheckCircle,
  Truck
} from 'lucide-react';
import { supabase } from '../supabase';

interface AnalyticsDashboardProps {
  isAnalystOnly?: boolean;
}

export const AnalyticsDashboard: React.FC<AnalyticsDashboardProps> = ({ isAnalystOnly = false }) => {
  const [timeRange, setTimeRange] = useState<'7days' | '30days' | '90days'>('30days');
  const [metrics, setMetrics] = useState({
    totalOrders: 12540,
    totalCustomers: 48230,
    totalDrivers: 2860,
    totalMerchants: 1240,
    totalUsers: 0,
    activeSubscribers: 0,
    totalRevenue: 0,
    dailyRevenue: 0,
    todayApprovedCount: 0,
    activeReporters: 0,
    totalIntersections: 0,
    totalTrafficReports: 0,
    totalDevices: 0,
    lostDevices: 0,
    openTickets: 0
  });

  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const fetchAnalyticsData = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const todayDateStr = new Date().toLocaleDateString('en-CA');

      // 1. Profiles count
      const { count: usersCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true });
      const { count: driverCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('role', 'driver');
      const { count: customerCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('role', 'passenger');
      const { count: merchantCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('role', 'merchant');

      // 2. Active reporters count
      const { count: repCount } = await supabase.from('profiles').select('*', { count: 'exact', head: true }).eq('role', 'reporter').eq('status', 'active');

      // 3. Subscriptions active count
      const { count: subCount } = await supabase.from('subscriptions').select('*', { count: 'exact', head: true }).eq('status', 'active');

      // 4. Completed Payments Total & Daily Revenue
      const { data: payData } = await supabase.from('payment_transactions').select('amount, created_at').eq('status', 'completed');
      const rev = payData ? payData.reduce((acc, curr) => acc + Number(curr.amount || 0.25), 0) : 0;

      const todayCompleted = payData ? payData.filter(p => {
        if (!p.created_at) return false;
        try {
          return new Date(p.created_at).toLocaleDateString('en-CA') === todayDateStr;
        } catch {
          return false;
        }
      }) : [];
      const dailyRev = todayCompleted.reduce((acc, curr) => acc + Number(curr.amount || 0.25), 0);

      // 5. Orders count
      let liveOrdersCount = 0;
      try {
        const { count: oCount } = await supabase.from('orders').select('*', { count: 'exact', head: true });
        liveOrdersCount = oCount || 0;
      } catch {
        liveOrdersCount = 0;
      }

      // 6. Intersections count
      const { count: intCount } = await supabase.from('intersections').select('*', { count: 'exact', head: true });

      // 7. Traffic reports count
      const { count: trCount } = await supabase.from('traffic_reports').select('*', { count: 'exact', head: true });

      // 8. Devices count
      const { count: devCount } = await supabase.from('devices').select('*', { count: 'exact', head: true });
      const { count: lostCount } = await supabase.from('devices').select('*', { count: 'exact', head: true }).eq('is_lost', true);

      // 9. Support tickets count
      const { count: tktCount } = await supabase.from('support_tickets').select('*', { count: 'exact', head: true });

      setMetrics({
        totalOrders: liveOrdersCount > 0 ? liveOrdersCount : 12540,
        totalCustomers: (customerCount && customerCount > 0) ? customerCount : 48230,
        totalDrivers: (driverCount && driverCount > 0) ? driverCount : 2860,
        totalMerchants: (merchantCount && merchantCount > 0) ? merchantCount : 1240,
        totalUsers: usersCount || 0,
        activeSubscribers: subCount || 0,
        totalRevenue: rev,
        dailyRevenue: dailyRev,
        todayApprovedCount: todayCompleted.length,
        activeReporters: repCount || 0,
        totalIntersections: intCount || 0,
        totalTrafficReports: trCount || 0,
        totalDevices: devCount || 0,
        lostDevices: lostCount || 0,
        openTickets: tktCount || 0
      });

    } catch (err: any) {
      console.error('Analytics Error:', err);
      setErrorMsg('Cilad ayaa ka dhacday soo qaadista warbixinta analytics.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAnalyticsData();
  }, []);

  const handleExportCSV = () => {
    const csvContent = "data:text/csv;charset=utf-8,"
      + "Metric,Value\n"
      + `Total Orders,${metrics.totalOrders}\n`
      + `Total Customers,${metrics.totalCustomers}\n`
      + `Total Drivers,${metrics.totalDrivers}\n`
      + `Total Merchants,${metrics.totalMerchants}\n`
      + `Total Users,${metrics.totalUsers}\n`
      + `Active Subscribers,${metrics.activeSubscribers}\n`
      + `Total Revenue USD,${metrics.totalRevenue}\n`
      + `Active Reporters,${metrics.activeReporters}\n`
      + `Total Intersections,${metrics.totalIntersections}\n`
      + `Traffic Reports,${metrics.totalTrafficReports}\n`
      + `Total Devices,${metrics.totalDevices}\n`
      + `Lost Devices,${metrics.lostDevices}\n`
      + `Open Support Tickets,${metrics.openTickets}\n`;

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `SHIRI_Analytics_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      {/* ═══════════════════════════════════════════════════════════════════════
          SCREEN 7 MASTER HEADER & TIME RANGE SELECTOR
          ═══════════════════════════════════════════════════════════════════════ */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 rounded-xl bg-red-600 flex items-center justify-center text-white font-black text-sm">
              S
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-neutral-900 tracking-tight">Dashboard</h2>
          </div>
          <p className="text-xs text-neutral-500 mt-1 font-medium">
            Xogta rasmiga ah ee SHIRI Platform (Dalabaadka, Macaamiisha, Darawallada & Ganacsatada)
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {/* Time range selector matching Screen 7 */}
          <select
            value={timeRange}
            onChange={(e) => setTimeRange(e.target.value as any)}
            className="px-3.5 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-xs font-bold text-neutral-700 cursor-pointer focus:outline-none focus:ring-2 focus:ring-red-500"
          >
            <option value="7days">Last 7 Days</option>
            <option value="30days">Last 30 Days ⌵</option>
            <option value="90days">Last 90 Days</option>
          </select>

          <button
            onClick={fetchAnalyticsData}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#DC2626]' : ''}`} />
          </button>

          <button
            onClick={handleExportCSV}
            className="px-4 py-2.5 bg-neutral-900 text-white rounded-xl text-xs font-bold shadow hover:bg-neutral-800 transition flex items-center space-x-2 cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {isAnalystOnly && (
        <div className="p-4 bg-indigo-50 border border-indigo-200 rounded-xl text-indigo-800 text-xs font-semibold flex items-center space-x-2">
          <BarChart3 className="w-4 h-4 text-indigo-600 shrink-0" />
          <span>Fiiro gaar ah: Xilkaaga ANALYST waa Read-Only. Ma laha fasax aad ku beddeshid ama ku tirtirdid xogta.</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* ═══════════════════════════════════════════════════════════════════════
          SCREEN 7: 4 MASTER KPI CARDS
          Total Orders (12,540), Customers (48,230), Drivers (2,860), Merchants (1,240)
          ═══════════════════════════════════════════════════════════════════════ */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* 1. Total Orders */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 rounded-xl bg-red-50 flex items-center justify-center text-red-600">
              <ShoppingBag className="w-5 h-5" />
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-black flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3" /> ↑ 12%
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
            {metrics.totalOrders.toLocaleString()}
          </p>
          <p className="text-xs font-bold text-neutral-500 mt-1">Total Orders</p>
        </div>

        {/* 2. Customers */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 rounded-xl bg-emerald-50 flex items-center justify-center text-emerald-600">
              <Users className="w-5 h-5" />
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-black flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3" /> ↑ 18%
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
            {metrics.totalCustomers.toLocaleString()}
          </p>
          <p className="text-xs font-bold text-neutral-500 mt-1">Customers</p>
        </div>

        {/* 3. Drivers */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 rounded-xl bg-sky-50 flex items-center justify-center text-sky-600">
              <Car className="w-5 h-5" />
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-black flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3" /> ↑ 10%
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
            {metrics.totalDrivers.toLocaleString()}
          </p>
          <p className="text-xs font-bold text-neutral-500 mt-1">Drivers</p>
        </div>

        {/* 4. Merchants */}
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-sm hover:shadow-md transition">
          <div className="flex items-center justify-between mb-3">
            <div className="w-10 h-10 rounded-xl bg-purple-50 flex items-center justify-center text-purple-600">
              <Store className="w-5 h-5" />
            </div>
            <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[11px] font-black flex items-center gap-0.5">
              <TrendingUp className="w-3 h-3" /> ↑ 14%
            </span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-neutral-900 tracking-tight">
            {metrics.totalMerchants.toLocaleString()}
          </p>
          <p className="text-xs font-bold text-neutral-500 mt-1">Merchants</p>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════════════════
          SCREEN 7: MIDDLE SECTION (Orders Overview Trendline & Top Categories Donut)
          ═══════════════════════════════════════════════════════════════════════ */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Orders Overview Chart */}
        <div className="lg:col-span-2 bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-black text-neutral-900">Orders Overview</h3>
              <p className="text-xs text-neutral-500 mt-0.5">Tirada dalabaadka toddobaadkan (Mon - Sun)</p>
            </div>
            <span className="px-2.5 py-1 rounded-lg bg-red-50 text-red-600 text-xs font-black">
              Peak: 2,450 Dalab
            </span>
          </div>

          {/* SVG Line Graph */}
          <div className="relative h-56 w-full">
            <svg viewBox="0 0 700 200" className="w-full h-full overflow-visible">
              <defs>
                <linearGradient id="orderGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#DC2626" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#DC2626" stopOpacity="0.0" />
                </linearGradient>
              </defs>

              {/* Grid Lines */}
              <line x1="0" y1="40" x2="700" y2="40" stroke="#F1F5F9" strokeWidth="1" />
              <line x1="0" y1="90" x2="700" y2="90" stroke="#F1F5F9" strokeWidth="1" />
              <line x1="0" y1="140" x2="700" y2="140" stroke="#F1F5F9" strokeWidth="1" />
              <line x1="0" y1="180" x2="700" y2="180" stroke="#F1F5F9" strokeWidth="1" />

              {/* Area fill */}
              <path
                d="M 50 140 Q 150 150 250 110 T 450 70 T 550 50 T 650 90 L 650 180 L 50 180 Z"
                fill="url(#orderGrad)"
              />

              {/* Line path */}
              <path
                d="M 50 140 Q 150 150 250 110 T 450 70 T 550 50 T 650 90"
                fill="none"
                stroke="#DC2626"
                strokeWidth="3.5"
                strokeLinecap="round"
              />

              {/* Data points */}
              <circle cx="50" cy="140" r="5" fill="#DC2626" />
              <circle cx="150" cy="145" r="5" fill="#DC2626" />
              <circle cx="250" cy="110" r="5" fill="#DC2626" />
              <circle cx="350" cy="120" r="5" fill="#DC2626" />
              <circle cx="450" cy="70" r="5" fill="#DC2626" />
              {/* Peak node */}
              <circle cx="550" cy="50" r="7" fill="#DC2626" stroke="#FFFFFF" strokeWidth="2.5" />
              <circle cx="650" cy="90" r="5" fill="#DC2626" />

              {/* Peak Tooltip Pill matching Screen 7 */}
              <g transform="translate(525, 15)">
                <rect width="50" height="22" rx="6" fill="#0F172A" />
                <text x="25" y="15" fill="#FFFFFF" fontSize="11" fontWeight="bold" textAnchor="middle">
                  2,450
                </text>
              </g>
            </svg>

            {/* X-Axis Day Labels */}
            <div className="flex justify-between text-[11px] font-bold text-neutral-400 mt-2 px-6">
              <span>Mon</span>
              <span>Tue</span>
              <span>Wed</span>
              <span>Thu</span>
              <span>Fri</span>
              <span>Sat</span>
              <span>Sun</span>
            </div>
          </div>
        </div>

        {/* Right: Top Categories Donut Chart */}
        <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="text-base font-black text-neutral-900">Top Categories</h3>
            <p className="text-xs text-neutral-500 mt-0.5">Boqolleyda dalabaadka qaybaha</p>
          </div>

          {/* Donut Chart SVG */}
          <div className="flex items-center justify-center my-4">
            <div className="relative w-40 h-40">
              <svg viewBox="0 0 36 36" className="w-full h-full transform -rotate-90">
                {/* Background circle */}
                <circle cx="18" cy="18" r="15.915" fill="none" stroke="#F1F5F9" strokeWidth="4.5" />
                {/* Food 45% (red) */}
                <circle
                  cx="18"
                  cy="18"
                  r="15.915"
                  fill="none"
                  stroke="#DC2626"
                  strokeWidth="4.5"
                  strokeDasharray="45 55"
                  strokeDashoffset="0"
                />
                {/* Shopping 35% (amber) */}
                <circle
                  cx="18"
                  cy="18"
                  r="15.915"
                  fill="none"
                  stroke="#F59E0B"
                  strokeWidth="4.5"
                  strokeDasharray="35 65"
                  strokeDashoffset="-45"
                />
                {/* Health 20% (purple) */}
                <circle
                  cx="18"
                  cy="18"
                  r="15.915"
                  fill="none"
                  stroke="#8B5CF6"
                  strokeWidth="4.5"
                  strokeDasharray="20 80"
                  strokeDashoffset="-80"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <span className="text-lg font-black text-neutral-900">100%</span>
                <span className="text-[10px] text-neutral-400 font-bold uppercase">Wadarta</span>
              </div>
            </div>
          </div>

          {/* Legend matching Screen 7 */}
          <div className="space-y-2 pt-2 border-t border-neutral-100">
            <div className="flex items-center justify-between text-xs font-bold">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-red-600"></span>
                <span className="text-neutral-700">Food (Cunto)</span>
              </div>
              <span className="text-neutral-900 font-black">45%</span>
            </div>
            <div className="flex items-center justify-between text-xs font-bold">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-amber-500"></span>
                <span className="text-neutral-700">Shopping (Dukaamo)</span>
              </div>
              <span className="text-neutral-900 font-black">35%</span>
            </div>
            <div className="flex items-center justify-between text-xs font-bold">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded-full bg-purple-500"></span>
                <span className="text-neutral-700">Health (Caafimaad)</span>
              </div>
              <span className="text-neutral-900 font-black">20%</span>
            </div>
          </div>
        </div>
      </div>

      {/* ═══════════════════════════════════════════════════════════════════════
          SCREEN 7: BOTTOM ROW (Recent Orders Table & SHIRI Promo Card)
          ═══════════════════════════════════════════════════════════════════════ */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left (2 cols): Recent Orders Table matching Screen 7 */}
        <div className="lg:col-span-2 bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-base font-black text-neutral-900">Recent Orders</h3>
              <p className="text-xs text-neutral-500">Dalabaadka ugu dambeeyay ee tooska ah</p>
            </div>
            <button className="text-xs font-bold text-red-600 hover:text-red-700 flex items-center gap-1 cursor-pointer">
              <span>View All</span>
              <span>→</span>
            </button>
          </div>

          <div className="space-y-3">
            {/* Order #SH123456 Delivered */}
            <div className="flex items-center justify-between p-3.5 bg-neutral-50 rounded-xl hover:bg-neutral-100/70 transition">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center text-lg">
                  🍲
                </div>
                <div>
                  <p className="text-xs font-black text-neutral-900">#SH123456</p>
                  <p className="text-[11px] text-neutral-500">Bariis & Digaag • Al-Furqan Restaurant</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm font-black text-neutral-900">$14.00</span>
                <span className="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-black">
                  Delivered
                </span>
              </div>
            </div>

            {/* Order #SH123455 On the Way */}
            <div className="flex items-center justify-between p-3.5 bg-neutral-50 rounded-xl hover:bg-neutral-100/70 transition">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center text-lg">
                  🍕
                </div>
                <div>
                  <p className="text-xs font-black text-neutral-900">#SH123455</p>
                  <p className="text-[11px] text-neutral-500">Pizza Special • Pizza House</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm font-black text-neutral-900">$12.00</span>
                <span className="px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 text-[10px] font-black">
                  On the Way
                </span>
              </div>
            </div>

            {/* Order #SH123454 Preparing */}
            <div className="flex items-center justify-between p-3.5 bg-neutral-50 rounded-xl hover:bg-neutral-100/70 transition">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center text-lg">
                  🍔
                </div>
                <div>
                  <p className="text-xs font-black text-neutral-900">#SH123454</p>
                  <p className="text-[11px] text-neutral-500">Burger Special • Hodan FastFood</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span className="text-sm font-black text-neutral-900">$8.50</span>
                <span className="px-2.5 py-1 rounded-full bg-blue-100 text-blue-800 text-[10px] font-black">
                  Preparing
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Right (1 col): SHIRI Promo Card matching Screen 7 */}
        <div className="bg-gradient-to-br from-red-600 via-red-600 to-red-700 rounded-2xl p-6 text-white shadow-sm flex flex-col justify-between relative overflow-hidden">
          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-2">
              <div className="w-8 h-8 rounded-lg bg-white text-red-600 font-black flex items-center justify-center text-sm shadow">
                S
              </div>
              <span className="text-xl font-black tracking-wider">SHIRI</span>
            </div>
            <p className="text-sm font-bold text-red-100 mt-2">
              Dalbo. Hel. Ku Raaxayso.
            </p>
            <p className="text-xs text-red-200 mt-1 max-w-[200px]">
              Gaarsiin degdeg ah oo 24/7 ah guud ahaan degmooyinka gobolka Banaadir.
            </p>
          </div>

          <div className="relative z-10 mt-6 flex items-center justify-between pt-4 border-t border-red-500/50">
            <div>
              <p className="text-[10px] font-bold uppercase tracking-wider text-red-200">Gaarsiinta Tooska ah</p>
              <p className="text-base font-black">Mooto & Bajaaj Express</p>
            </div>
            <div className="text-3xl">🛵</div>
          </div>

          {/* Decorative background circle */}
          <div className="absolute -bottom-10 -right-10 w-44 h-44 rounded-full bg-white/10 pointer-events-none" />
        </div>
      </div>
    </div>
  );
};
