import React, { useState, useEffect } from 'react';
import { BarChart3, Users, DollarSign, TrafficCone, Shield, Download, RefreshCw, AlertCircle } from 'lucide-react';
import { supabase } from '../supabase';

interface AnalyticsDashboardProps {
  isAnalystOnly?: boolean;
}

export const AnalyticsDashboard: React.FC<AnalyticsDashboardProps> = ({ isAnalystOnly = false }) => {
  const [metrics, setMetrics] = useState({
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

      // 5. Intersections count
      const { count: intCount } = await supabase.from('intersections').select('*', { count: 'exact', head: true });

      // 6. Traffic reports count
      const { count: trCount } = await supabase.from('traffic_reports').select('*', { count: 'exact', head: true });

      // 7. Devices count
      const { count: devCount } = await supabase.from('devices').select('*', { count: 'exact', head: true });
      const { count: lostCount } = await supabase.from('devices').select('*', { count: 'exact', head: true }).eq('is_lost', true);

      // 8. Support tickets count
      const { count: tktCount } = await supabase.from('support_tickets').select('*', { count: 'exact', head: true });

      setMetrics({
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
    link.setAttribute("download", `SomaliTraffic_Analytics_${new Date().toISOString().split('T')[0]}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <BarChart3 className="w-6 h-6 text-[#E50914] shrink-0" />
            <h2 className="text-lg sm:text-xl font-black text-black">Aqoonta & Shaxanka Xogta (Real Analytics)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Tirakoobka rasmiga ah ee xogta tooska ah ee GoBajaaj. {isAnalystOnly && '(Habka Akhriska Keliya - Read Only)'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button 
            onClick={fetchAnalyticsData}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>
          
          <button 
            onClick={handleExportCSV}
            className="px-4 py-2.5 bg-black text-white rounded-xl text-sm font-bold shadow hover:bg-neutral-800 transition flex items-center space-x-2 cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>La Soo Deg Report (CSV)</span>
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

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 sm:gap-4">
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Macaamiisha Buuxda</span>
            <Users className="w-5 h-5 text-neutral-400" />
          </div>
          <p className="text-2xl sm:text-3xl font-black text-black">{isLoading ? '...' : metrics.totalUsers}</p>
          <p className="text-xs text-neutral-400 mt-1">Darawallada & Wariyeyasha</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Driver Pass Active</span>
            <DollarSign className="w-5 h-5 text-emerald-500" />
          </div>
          <p className="text-2xl sm:text-3xl font-black text-emerald-600">{isLoading ? '...' : metrics.activeSubscribers}</p>
          <p className="text-xs text-neutral-400 mt-1">$0.25/Shift Pass</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Dakhliga Guud</span>
            <DollarSign className="w-5 h-5 text-emerald-600" />
          </div>
          <p className="text-2xl sm:text-3xl font-black text-black">{isLoading ? '...' : `$${metrics.totalRevenue.toFixed(2)}`}</p>
          <p className="text-xs text-neutral-400 mt-1">Wadarta Guud ee la ansixiyay</p>
        </div>

        <div className="bg-gradient-to-br from-emerald-50 to-white p-5 rounded-2xl border-2 border-emerald-500/50 shadow-xs">
          <div className="flex items-center justify-between text-emerald-800 mb-2">
            <span className="text-xs font-black uppercase tracking-wider">Dakhliga Maanta</span>
            <span className="px-1.5 py-0.5 rounded bg-emerald-600 text-white text-[9px] font-black">Maalinle</span>
          </div>
          <p className="text-2xl sm:text-3xl font-black text-emerald-700">{isLoading ? '...' : `$${metrics.dailyRevenue.toFixed(2)}`}</p>
          <p className="text-xs text-emerald-600 font-bold mt-1">🟢 {metrics.todayApprovedCount} bixino maanta</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between text-neutral-500 mb-2">
            <span className="text-xs font-bold uppercase tracking-wider">Isgoysyada & Reports</span>
            <TrafficCone className="w-5 h-5 text-[#E50914]" />
          </div>
          <p className="text-2xl sm:text-3xl font-black text-[#E50914]">{isLoading ? '...' : `${metrics.totalIntersections} / ${metrics.totalTrafficReports}`}</p>
          <p className="text-xs text-neutral-400 mt-1">Intersections / Live Reports</p>
        </div>
      </div>

      {/* Secondary Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4 sm:gap-6">
        <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex items-center space-x-4">
          <div className="w-12 h-12 rounded-2xl bg-sky-100 text-sky-700 flex items-center justify-center font-black shrink-0">
            {metrics.activeReporters}
          </div>
          <div>
            <p className="font-bold text-black text-sm">Wariyeyaasha Shakalka ah</p>
            <p className="text-xs text-neutral-500">Reporters operating at intersections</p>
          </div>
        </div>

        <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex items-center space-x-4">
          <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center font-black shrink-0">
            {metrics.lostDevices}
          </div>
          <div>
            <p className="font-bold text-black text-sm">Taleefannada Dhiman/Raadjoog</p>
            <p className="text-xs text-neutral-500">Lost or stolen devices in system</p>
          </div>
        </div>

        <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex items-center space-x-4 sm:col-span-2 md:col-span-1">
          <div className="w-12 h-12 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center font-black shrink-0">
            {metrics.openTickets}
          </div>
          <div>
            <p className="font-bold text-black text-sm">Cabashooyinka Taageerada</p>
            <p className="text-xs text-neutral-500">Support tickets registered</p>
          </div>
        </div>
      </div>
    </div>
  );
};
