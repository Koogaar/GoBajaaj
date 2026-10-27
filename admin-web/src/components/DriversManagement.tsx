import React, { useState, useEffect } from 'react';
import { 
  Bike, 
  Search, 
  RefreshCw, 
  CheckCircle2, 
  XCircle, 
  Clock, 
  Zap, 
  Sparkles, 
  Copy, 
  Check, 
  ShieldAlert, 
  AlertTriangle,
  UserCheck,
  CreditCard,
  Phone
} from 'lucide-react';
import { Profile } from '../types';
import { supabase } from '../supabase';
import { soundAlert } from '../utils/soundAlert';

const isWithinMinutes = (dateStr?: string, minutes: number = 10) => {
  if (!dateStr) return false;
  const diff = Date.now() - new Date(dateStr).getTime();
  return diff >= 0 && diff <= minutes * 60 * 1000;
};

const formatTimeAgoSomali = (dateStr?: string) => {
  if (!dateStr) return 'Lama yaqaan';
  const diffSec = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (diffSec < 60) return 'Hadda';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin} daqiiqo kahor`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours} saac kahor`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays} maalmood kahor`;
};

interface DriversManagementProps {
  canManage: boolean;
  onNavigateToDailyPaid?: () => void;
}

export const DriversManagement: React.FC<DriversManagementProps> = ({ canManage, onNavigateToDailyPaid }) => {
  const [drivers, setDrivers] = useState<Profile[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState<'all' | 'recent10' | 'active' | 'pending' | 'expired'>('all');
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [approvingDriverId, setApprovingDriverId] = useState<string | null>(null);

  const fetchDrivers = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('profiles')
        .select('*')
        .eq('role', 'driver')
        .order('created_at', { ascending: false });

      if (!error && data) {
        setDrivers(data as Profile[]);
      }
    } catch (err) {
      console.error('Fetch drivers error:', err);
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDrivers(false);
    const interval = setInterval(() => fetchDrivers(true), 12000);
    return () => clearInterval(interval);
  }, []);

  const handleCopyPhone = (phone: string) => {
    if (!phone) return;
    navigator.clipboard.writeText(phone);
    setCopiedPhone(phone);
    setTimeout(() => setCopiedPhone(null), 2000);
  };

  const handleQuickApprovePass = async (driver: Profile) => {
    if (!driver.id || !canManage) return;
    setApprovingDriverId(driver.id);
    try {
      // Find pending payment
      let targetId: string | undefined;
      const { data: pendingTx } = await supabase
        .from('payment_transactions')
        .select('id')
        .eq('user_id', driver.id)
        .eq('status', 'pending')
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();

      targetId = pendingTx?.id;

      if (!targetId) {
        const { data: pendingSub } = await supabase
          .from('subscriptions')
          .select('id')
          .eq('user_id', driver.id)
          .eq('status', 'pending')
          .order('created_at', { ascending: false })
          .limit(1)
          .maybeSingle();
        targetId = pendingSub?.id;
      }

      if (!targetId) {
        alert('Darawalkani ma jiro bixin $0.25 oo hadda sugaysa (Pending). Fadlan sug inuu lacagta soo shubo.');
        return;
      }

      const { data, error } = await supabase.rpc('approve_driver_pass_payment', {
        p_payment_id: targetId
      });

      if (error) {
        throw new Error(error.message);
      }

      soundAlert.playPaymentSound();
      alert(`✅ Guul! Driver Pass-ka (${driver.full_name || driver.phone_number}) waa la ansixiyay!`);
      await fetchDrivers(true);
    } catch (err: any) {
      alert('Cilad ansixinta: ' + (err?.message || 'Dib u tijaabi'));
    } finally {
      setApprovingDriverId(null);
    }
  };

  const handleToggleStatus = async (driver: Profile) => {
    if (!canManage) return;
    const nextStatus = driver.status === 'active' ? 'suspended' : 'active';
    const confirmMsg = nextStatus === 'suspended'
      ? `Ma hubtaa inaad joojiso (Suspend) akoonka darawalka "${driver.full_name}"?`
      : `Ma hubtaa inaad dib u hawlgeliso (Activate) darawalka "${driver.full_name}"?`;

    if (!window.confirm(confirmMsg)) return;

    try {
      const { error } = await supabase
        .from('profiles')
        .update({ status: nextStatus, updated_at: new Date().toISOString() })
        .eq('id', driver.id);

      if (error) throw error;
      await fetchDrivers(true);
    } catch (err: any) {
      alert('Cilad badelidda xaaladda: ' + err.message);
    }
  };

  const recentDriversCount = drivers.filter(d => isWithinMinutes(d.created_at, 10)).length;
  const activePassCount = drivers.filter(d => d.subscription_status === 'active').length;
  const pendingPassCount = drivers.filter(d => d.subscription_status === 'pending').length;

  const filteredDrivers = drivers
    .filter(d => {
      if (filter === 'recent10' && !isWithinMinutes(d.created_at, 10)) return false;
      if (filter === 'active' && d.subscription_status !== 'active') return false;
      if (filter === 'pending' && d.subscription_status !== 'pending') return false;
      if (filter === 'expired' && d.subscription_status !== 'expired' && d.subscription_status !== 'rejected') return false;

      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase().trim();
      return (
        (d.full_name && d.full_name.toLowerCase().includes(q)) ||
        (d.phone_number && d.phone_number.toLowerCase().includes(q)) ||
        (d.plate_number && d.plate_number.toLowerCase().includes(q)) ||
        (d.bajaaj_model && d.bajaaj_model.toLowerCase().includes(q))
      );
    });

  return (
    <div className="space-y-6">
      {/* Top Header Card */}
      <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <div className="w-10 h-10 rounded-xl bg-[#E50914] text-white flex items-center justify-center shadow-md">
              <Bike className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl font-black text-black">
                Darawallada Diiwaangashan ({drivers.length})
              </h2>
              <p className="text-xs text-neutral-500 mt-0.5">
                Liiska goonida ah ee dhammaan darawallada bajaajta ee ku diiwaangashan GoBajaaj.
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center space-x-2.5 w-full md:w-auto">
          {onNavigateToDailyPaid && (
            <button
              onClick={onNavigateToDailyPaid}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1.5 cursor-pointer shrink-0"
            >
              <Zap className="w-3.5 h-3.5 fill-white" />
              <span>Eeg Kuwa Ku Shubtay Maanta ($0.25)</span>
            </button>
          )}

          <button
            onClick={() => fetchDrivers(false)}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer shrink-0"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Wadarta Darawallada</p>
          <p className="text-3xl font-black text-black mt-2">{drivers.length}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Dhammaan darawallada la keydiyay</p>
        </div>

        <div className="bg-gradient-to-br from-emerald-50 to-white p-5 rounded-2xl border-2 border-emerald-500/40 shadow-xs">
          <p className="text-xs font-black text-emerald-800 uppercase tracking-wider">Pass Shaqaynaya (Active)</p>
          <p className="text-3xl font-black text-emerald-700 mt-2">{activePassCount}</p>
          <p className="text-[11px] text-emerald-600 font-semibold mt-1">Darawallo leh ruqsad 24h ah</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Sugaya Ansixin (Pending)</p>
          <p className="text-3xl font-black text-[#E50914] mt-2">{pendingPassCount}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Bixino sugaya aqbalidda maamulka</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Cusub (10 Min)</p>
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
          </div>
          <p className="text-3xl font-black text-emerald-800 mt-2">{recentDriversCount}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Hadda soo diiwaangashaday</p>
        </div>
      </div>

      {/* Search & Filter Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Filters */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filter === 'all'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            Dhammaan ({drivers.length})
          </button>

          <button
            onClick={() => setFilter('recent10')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0 ${
              filter === 'recent10'
                ? 'bg-emerald-600 text-white shadow'
                : 'bg-emerald-50 text-emerald-800 hover:bg-emerald-100 border border-emerald-200'
            }`}
          >
            <Zap className="w-3.5 h-3.5 text-amber-500 fill-amber-500" />
            <span>10-Kii Daqiiqo ee Ugu Dambeeyay ({recentDriversCount})</span>
          </button>

          <button
            onClick={() => setFilter('active')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filter === 'active'
                ? 'bg-emerald-600 text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            🟢 Pass Shaqaynaya ({activePassCount})
          </button>

          <button
            onClick={() => setFilter('pending')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filter === 'pending'
                ? 'bg-[#E50914] text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            🟡 Sugaya Ansixin ({pendingPassCount})
          </button>
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-neutral-400" />
          <input
            type="text"
            placeholder="Raadi magac, taleefan ama taargo..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-xl text-xs text-black focus:outline-none focus:bg-white"
          />
        </div>
      </div>

      {/* Drivers Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya darawallada...</p>
          </div>
        ) : filteredDrivers.length === 0 ? (
          <div className="p-12 text-center">
            <Bike className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Darawal lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1">Ma jiro darawal ku jira shuruudahan.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[750px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Darawalka</th>
                  <th className="p-4 font-bold">Nambarka Taleefanka</th>
                  <th className="p-4 font-bold">Taargada & Nooca</th>
                  <th className="p-4 font-bold">Xaaladda Akoonka</th>
                  <th className="p-4 font-bold">Driver Pass ($0.25)</th>
                  <th className="p-4 font-bold">Diiwaangelinta</th>
                  {canManage && <th className="p-4 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredDrivers.map((driver) => {
                  const isRecent = isWithinMinutes(driver.created_at, 10);
                  const isPassActive = driver.subscription_status === 'active';
                  const isPassPending = driver.subscription_status === 'pending';
                  const isApproving = approvingDriverId === driver.id;

                  return (
                    <tr 
                      key={driver.id}
                      className={`transition ${
                        isRecent 
                          ? 'bg-emerald-50/80 hover:bg-emerald-100/70 border-l-4 border-l-emerald-500' 
                          : 'hover:bg-neutral-50'
                      }`}
                    >
                      <td className="p-4">
                        <div className="flex items-center space-x-3">
                          <div className="w-9 h-9 rounded-full bg-[#E50914] text-white flex items-center justify-center text-xs font-black shrink-0">
                            {driver.full_name?.charAt(0) || 'D'}
                          </div>
                          <div>
                            <div className="flex items-center space-x-1.5">
                              <span className="font-bold text-black">{driver.full_name || 'Darawal GoBajaaj'}</span>
                              {isRecent && (
                                <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded-md bg-emerald-200 text-emerald-900 text-[10px] font-black border border-emerald-400">
                                  <Sparkles className="w-3 h-3 text-emerald-700" />
                                  CUSUB
                                </span>
                              )}
                            </div>
                            <p className="text-[11px] text-neutral-400 font-mono">ID: {driver.id.slice(0, 8)}...</p>
                          </div>
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="flex items-center space-x-1.5 font-mono text-xs font-bold text-black">
                          <span>{driver.phone_number}</span>
                          <button
                            onClick={() => handleCopyPhone(driver.phone_number)}
                            className="p-1 hover:bg-neutral-200 rounded text-neutral-500 hover:text-black cursor-pointer transition"
                            title="Koobiyee lambarka"
                          >
                            {copiedPhone === driver.phone_number ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </div>
                      </td>

                      <td className="p-4 text-xs">
                        <p className="font-black text-black">
                          {driver.plate_number ? `Taargo: ${driver.plate_number}` : 'Taargo la\'aan'}
                        </p>
                        <p className="text-[11px] text-neutral-500">
                          {driver.bajaaj_model || 'Bajaaj Caadi'}
                        </p>
                      </td>

                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          driver.status === 'active' 
                            ? 'bg-emerald-100 text-emerald-800' 
                            : 'bg-red-100 text-red-800'
                        }`}>
                          {driver.status === 'active' ? '🟢 Active' : '🔴 Joojiyay'}
                        </span>
                      </td>

                      <td className="p-4">
                        <div className="flex flex-col space-y-1">
                          <span className={`px-2.5 py-1 rounded-lg text-xs font-bold w-fit ${
                            isPassActive
                              ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                              : isPassPending
                                ? 'bg-amber-100 text-amber-800 border border-amber-200 animate-pulse'
                                : 'bg-neutral-100 text-neutral-600'
                          }`}>
                            {isPassActive 
                              ? '🟢 Shaqaynaya' 
                              : isPassPending 
                                ? '🟡 Sugaya $0.25' 
                                : '⚪ Ma haysto'}
                          </span>

                          {isPassPending && canManage && (
                            <button
                              onClick={() => handleQuickApprovePass(driver)}
                              disabled={isApproving}
                              className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-[11px] font-black shadow transition cursor-pointer flex items-center space-x-1 w-fit"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>{isApproving ? 'Ansixin...' : 'Ansixi Hadda'}</span>
                            </button>
                          )}
                        </div>
                      </td>

                      <td className="p-4 text-xs text-neutral-600">
                        <p className="font-semibold text-black">
                          {driver.created_at ? new Date(driver.created_at).toLocaleDateString() : 'N/A'}
                        </p>
                        <p className="text-[11px] text-neutral-400">
                          {formatTimeAgoSomali(driver.created_at)}
                        </p>
                      </td>

                      {canManage && (
                        <td className="p-4 text-right">
                          <button
                            onClick={() => handleToggleStatus(driver)}
                            className={`px-3 py-1.5 text-xs font-bold rounded-xl border transition cursor-pointer ${
                              driver.status === 'active'
                                ? 'border-amber-200 text-amber-700 hover:bg-amber-50'
                                : 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                            }`}
                          >
                            {driver.status === 'active' ? 'Jooji (Suspend)' : 'Fasax (Activate)'}
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
