import React, { useState, useEffect, useCallback } from 'react';
import {
  Megaphone,
  Users,
  CheckCircle2,
  Clock,
  Copy,
  Check,
  Search,
  RefreshCw,
  LogOut,
  Car,
  Phone,
  Calendar,
  AlertCircle,
  ShieldCheck,
  ChevronRight
} from 'lucide-react';
import { supabase } from '../supabase';
import { MarketingAgent } from '../types';
import logoImg from '../assets/logo.png';

interface MarketerPortalProps {
  agent: MarketingAgent;
  onLogout: () => void;
}

interface ReferredDriver {
  id: string;
  full_name: string;
  phone_number: string;
  plate_number?: string;
  bajaaj_model?: string;
  role: string;
  subscription_status?: string;
  subscription_expiry?: string;
  created_at: string;
}

export const MarketerPortal: React.FC<MarketerPortalProps> = ({ agent, onLogout }) => {
  const [drivers, setDrivers] = useState<ReferredDriver[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'active' | 'inactive'>('all');
  const [isCopied, setIsCopied] = useState(false);
  const [refreshError, setRefreshError] = useState<string | null>(null);

  const fetchReferredDrivers = useCallback(async () => {
    setIsLoading(true);
    setRefreshError(null);
    try {
      // 1. Fetch all profiles where referred_by equals this agent's code
      const { data: profilesData, error: profilesError } = await supabase
        .from('profiles')
        .select('*')
        .eq('referred_by', agent.code)
        .order('created_at', { ascending: false });

      if (profilesError) throw profilesError;

      // 2. Fetch latest active subscriptions for these users to determine active pass status
      const driverList: ReferredDriver[] = profilesData || [];

      // Enrich with subscription status check
      const enriched = await Promise.all(
        driverList.map(async (driver) => {
          try {
            const { data: subData } = await supabase
              .from('subscriptions')
              .select('status, end_date')
              .eq('user_id', driver.id)
              .order('created_at', { ascending: false })
              .limit(1)
              .maybeSingle();

            let isActive = false;
            if (subData && ['active', 'approved', 'completed'].includes(subData.status?.toLowerCase())) {
              if (subData.end_date) {
                const expiry = new Date(subData.end_date).getTime();
                isActive = expiry > Date.now();
              }
            }

            return {
              ...driver,
              subscription_status: isActive ? 'active' : (subData?.status || 'inactive'),
              subscription_expiry: subData?.end_date || driver.subscription_expiry
            };
          } catch {
            return driver;
          }
        })
      );

      setDrivers(enriched);
    } catch (err: any) {
      console.error('Error fetching referred drivers:', err);
      setRefreshError('Cilad ayaa dhacday marka la soo xareynayay darawallada. Fadlan dib u cusboonaysii.');
    } finally {
      setIsLoading(false);
    }
  }, [agent.code]);

  useEffect(() => {
    fetchReferredDrivers();
  }, [fetchReferredDrivers]);

  const handleCopyCode = () => {
    navigator.clipboard.writeText(agent.code).catch(() => {});
    setIsCopied(true);
    setTimeout(() => setIsCopied(false), 2500);
  };

  // Stats calculation
  const totalDrivers = drivers.length;
  const activeCount = drivers.filter(d => d.subscription_status === 'active').length;
  const inactiveCount = totalDrivers - activeCount;

  // Filtered drivers
  const filteredDrivers = drivers.filter(d => {
    const matchesSearch =
      (d.full_name || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
      (d.phone_number || '').includes(searchQuery) ||
      (d.plate_number || '').toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    if (statusFilter === 'active') return d.subscription_status === 'active';
    if (statusFilter === 'inactive') return d.subscription_status !== 'active';
    return true;
  });

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return '-';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('so-SO', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Navigation */}
      <header className="bg-slate-900/90 border-b border-slate-800 sticky top-0 z-30 backdrop-blur-md">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <img src={logoImg} alt="GoBajaaj" className="h-10 w-10 object-contain rounded-xl bg-slate-800 p-1 border border-slate-700" />
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg text-white">GoBajaaj</span>
                <span className="bg-emerald-500/10 text-emerald-400 text-xs px-2 py-0.5 rounded-full font-semibold border border-emerald-500/20">
                  Marketing Portal
                </span>
              </div>
              <p className="text-xs text-slate-400">Xafiiska Suuq-geynta & Referral-ka</p>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div className="hidden sm:flex items-center space-x-3 bg-slate-800/80 px-3.5 py-1.5 rounded-lg border border-slate-700">
              <div className="w-8 h-8 rounded-full bg-emerald-600 flex items-center justify-center font-bold text-white text-sm">
                {agent.full_name?.charAt(0).toUpperCase() || 'M'}
              </div>
              <div className="text-left">
                <p className="text-sm font-semibold text-slate-200">{agent.full_name}</p>
                <p className="text-xs text-slate-400">@{agent.username} • <span className="text-emerald-400 font-mono font-bold">{agent.code}</span></p>
              </div>
            </div>

            <button
              onClick={onLogout}
              className="flex items-center space-x-1.5 px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 rounded-lg text-xs font-semibold border border-rose-500/30 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Ka Bax</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-6">
        {/* Welcome & Referral Code Hero Banner */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-emerald-950/80 via-slate-900 to-slate-900 p-6 sm:p-8 border border-emerald-500/20 shadow-2xl">
          <div className="absolute top-0 right-0 -mr-16 -mt-16 w-64 h-64 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />
          
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
            <div>
              <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 text-xs font-semibold mb-3">
                <Megaphone className="w-3.5 h-3.5" />
                <span>Ku soo dhowaw xafiiska suuq-geynta, {agent.full_name}!</span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
                Koodhkaaga Suuq-geynta ee GoBajaaj
              </h1>
              <p className="text-slate-400 text-sm mt-1 max-w-xl">
                Koodhkan sii darawallada cusub ee aad la hadasho. Marka ay app-ka iska diiwaangelinayaan, ha ku qoraan sanduuqa <strong className="text-emerald-400 font-medium">"Koodhka Referral"</strong>. Waxaad halkan si toos ah uga arki doontaa darawal kasta oo ku diiwaangashan.
              </p>
            </div>

            <div className="bg-slate-950/90 border border-emerald-500/40 rounded-xl p-4 sm:p-5 flex flex-col items-center justify-center min-w-[260px] shadow-lg">
              <span className="text-xs text-slate-400 uppercase tracking-wider font-semibold">Koodhkaaga Gaarka ah</span>
              <div className="my-2 px-5 py-2.5 bg-slate-900 border border-emerald-500/50 rounded-lg text-2xl sm:text-3xl font-black font-mono tracking-widest text-emerald-400 select-all">
                {agent.code}
              </div>
              <button
                onClick={handleCopyCode}
                className="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg font-semibold text-sm transition-all shadow-md active:scale-95"
              >
                {isCopied ? (
                  <>
                    <Check className="w-4 h-4 text-emerald-200" />
                    <span>Waa la Koobiyay!</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    <span>Koobbi Koodhka</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* 3 KPI Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Wadarta Darawallada</p>
                <h3 className="text-3xl font-black text-white mt-1">{totalDrivers}</h3>
              </div>
              <div className="p-3 bg-blue-500/10 rounded-xl border border-blue-500/20 text-blue-400">
                <Users className="w-6 h-6" />
              </div>
            </div>
            <p className="text-xs text-slate-500 mt-3">Dhammaan darawallada ku diiwaangashan koodhkaaga</p>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">Kuwa Pass-ku u Shaqeynayo</p>
                <h3 className="text-3xl font-black text-emerald-400 mt-1">{activeCount}</h3>
              </div>
              <div className="p-3 bg-emerald-500/10 rounded-xl border border-emerald-500/20 text-emerald-400">
                <CheckCircle2 className="w-6 h-6" />
              </div>
            </div>
            <p className="text-xs text-slate-500 mt-3">Darawallada hadda shaqada ku jira oo pass-koodu active yahay</p>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold text-amber-400 uppercase tracking-wider">Pass-ku ka Dhacay / Inactive</p>
                <h3 className="text-3xl font-black text-amber-400 mt-1">{inactiveCount}</h3>
              </div>
              <div className="p-3 bg-amber-500/10 rounded-xl border border-amber-500/20 text-amber-400">
                <Clock className="w-6 h-6" />
              </div>
            </div>
            <p className="text-xs text-slate-500 mt-3">Darawallada pass-koodu dhacay ama aan weli dib u shuban</p>
          </div>
        </div>

        {/* Error Notification */}
        {refreshError && (
          <div className="p-4 bg-rose-500/10 border border-rose-500/30 rounded-xl flex items-center space-x-3 text-rose-400 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span>{refreshError}</span>
          </div>
        )}

        {/* Drivers Table Section */}
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
          {/* Table Header Controls */}
          <div className="p-4 sm:p-5 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold text-white flex items-center space-x-2">
                <span>Darawallada Aad Diiwaangelisay</span>
                <span className="bg-slate-800 text-slate-300 text-xs px-2.5 py-0.5 rounded-full font-semibold border border-slate-700">
                  {filteredDrivers.length}
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">Xogta tooska ah ee darawallada ku xiran koodhkaaga</p>
            </div>

            <div className="flex flex-wrap items-center gap-2.5">
              {/* Search input */}
              <div className="relative min-w-[220px]">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Raadi magac, taleefan, taariko..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-emerald-500"
                />
              </div>

              {/* Status Filter buttons */}
              <div className="flex bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs font-semibold">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-3 py-1 rounded-md transition-colors ${statusFilter === 'all' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'}`}
                >
                  Dhammaan ({totalDrivers})
                </button>
                <button
                  onClick={() => setStatusFilter('active')}
                  className={`px-3 py-1 rounded-md transition-colors ${statusFilter === 'active' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'}`}
                >
                  Active ({activeCount})
                </button>
                <button
                  onClick={() => setStatusFilter('inactive')}
                  className={`px-3 py-1 rounded-md transition-colors ${statusFilter === 'inactive' ? 'bg-emerald-600 text-white' : 'text-slate-400 hover:text-white'}`}
                >
                  Inactive ({inactiveCount})
                </button>
              </div>

              {/* Refresh button */}
              <button
                onClick={fetchReferredDrivers}
                disabled={isLoading}
                className="p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg border border-slate-700 transition-colors disabled:opacity-50"
                title="Cusboonaysii Xogta"
              >
                <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-emerald-400' : ''}`} />
              </button>
            </div>
          </div>

          {/* Table List */}
          <div className="overflow-x-auto">
            {isLoading ? (
              <div className="py-16 flex flex-col items-center justify-center text-slate-400 space-y-3">
                <RefreshCw className="w-8 h-8 animate-spin text-emerald-400" />
                <p className="text-sm">Soo xareynayaa darawallada ku diiwaangashan koodhkaaga...</p>
              </div>
            ) : filteredDrivers.length === 0 ? (
              <div className="py-16 px-4 text-center">
                <div className="w-16 h-16 bg-slate-800/80 rounded-2xl flex items-center justify-center mx-auto text-slate-500 mb-3 border border-slate-700">
                  <Car className="w-8 h-8" />
                </div>
                <h3 className="text-base font-semibold text-slate-200">
                  {searchQuery ? 'Wax darawal ah laguma helin raadintaada' : 'Weli wax darawal ah kuma diiwaangashan koodhkaaga'}
                </h3>
                <p className="text-xs text-slate-400 mt-1 max-w-sm mx-auto">
                  {searchQuery
                    ? 'Fadlan hubi magaca ama lambarka aad raadinayso.'
                    : `La wadaag darawallada koodhkaaga (${agent.code}) marka ay app-ka GoBajaaj iska diiwaangelinayaan.`}
                </p>
              </div>
            ) : (
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950/60 text-slate-400 uppercase tracking-wider text-[11px] border-b border-slate-800">
                  <tr>
                    <th className="px-5 py-3.5">#</th>
                    <th className="px-5 py-3.5">Darawalka</th>
                    <th className="px-5 py-3.5">Taleefanka</th>
                    <th className="px-5 py-3.5">Bajaajta / Taarikada</th>
                    <th className="px-5 py-3.5">Taariikhda</th>
                    <th className="px-5 py-3.5">Driver Pass</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {filteredDrivers.map((driver, index) => {
                    const isActive = driver.subscription_status === 'active';
                    return (
                      <tr key={driver.id} className="hover:bg-slate-800/40 transition-colors">
                        <td className="px-5 py-4 font-mono text-slate-500">{index + 1}</td>
                        <td className="px-5 py-4">
                          <div className="flex items-center space-x-3">
                            <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 font-bold">
                              {driver.full_name?.charAt(0).toUpperCase() || 'D'}
                            </div>
                            <div>
                              <p className="font-semibold text-slate-200 text-sm">{driver.full_name || 'Darawal Aan Magac Lahayn'}</p>
                              <p className="text-[11px] text-slate-500 capitalize">{driver.role}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-5 py-4">
                          <span className="font-mono text-slate-300 bg-slate-950 px-2 py-1 rounded border border-slate-800">
                            {driver.phone_number || '-'}
                          </span>
                        </td>
                        <td className="px-5 py-4">
                          <div className="flex items-center space-x-1.5 text-slate-300">
                            <Car className="w-3.5 h-3.5 text-slate-500" />
                            <span className="font-mono font-medium">{driver.plate_number || 'Aan la qorin'}</span>
                          </div>
                          {driver.bajaaj_model && (
                            <span className="text-[10px] text-slate-500 block mt-0.5">{driver.bajaaj_model}</span>
                          )}
                        </td>
                        <td className="px-5 py-4 text-slate-400">
                          {formatDate(driver.created_at)}
                        </td>
                        <td className="px-5 py-4">
                          {isActive ? (
                            <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Active Pass</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                              <Clock className="w-3.5 h-3.5" />
                              <span>Dhacay / Inactive</span>
                            </span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-slate-900/50 border-t border-slate-800 py-4 text-center text-xs text-slate-500">
        © 2026 GoBajaaj Marketing Portal. Dhammaan xuquuqda waa dhowran tahay.
      </footer>
    </div>
  );
};
