import React, { useState, useEffect } from 'react';
import { 
  Zap, 
  CreditCard, 
  Search, 
  RefreshCw, 
  CheckCircle2, 
  XCircle, 
  Copy, 
  Check, 
  AlertCircle, 
  DollarSign, 
  Smartphone, 
  Clock, 
  Sparkles,
  Bike,
  Volume2,
  VolumeX
} from 'lucide-react';
import { PaymentTransaction, Profile } from '../types';
import { supabase } from '../supabase';
import { soundAlert } from '../utils/soundAlert';

interface DailyRechargedManagementProps {
  actorName: string;
  canManage: boolean;
  onNavigateToDrivers?: () => void;
}

export const DailyRechargedManagement: React.FC<DailyRechargedManagementProps> = ({
  actorName,
  canManage,
  onNavigateToDrivers
}) => {
  const [payments, setPayments] = useState<PaymentTransaction[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | 'pending' | 'completed' | 'rejected'>('all');
  const [processingId, setProcessingId] = useState<string | null>(null);
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [soundEnabled, setSoundEnabled] = useState(soundAlert.isEnabled());

  const todayDateStr = new Date().toLocaleDateString('en-CA'); // YYYY-MM-DD

  const fetchTodayPayments = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    setErrorMsg(null);
    try {
      // 1. Fetch payments for today
      const { data: payData, error: payErr } = await supabase
        .from('payment_transactions')
        .select('*')
        .gte('created_at', `${todayDateStr}T00:00:00Z`)
        .order('created_at', { ascending: false });

      if (payErr) {
        throw payErr;
      }

      // 2. Lookup profiles to attach driver names & plate numbers
      const { data: profData } = await supabase
        .from('profiles')
        .select('*');

      const profMap = new Map<string, Profile>();
      if (profData) {
        profData.forEach((p: Profile) => {
          profMap.set(p.id, p);
          if (p.phone_number) profMap.set(p.phone_number, p);
        });
      }

      const list: PaymentTransaction[] = (payData || []).map((p: any) => {
        const prof = profMap.get(p.user_id) || (p.phone_number ? profMap.get(p.phone_number) : null);
        return {
          id: p.id,
          subscription_id: p.subscription_id,
          user_id: p.user_id,
          user_name: prof?.full_name || (p.phone_number ? `Darawal (${p.phone_number.slice(-6)})` : 'Darawal Bajaaj'),
          phone_number: p.phone_number || prof?.phone_number || '',
          amount: Number(p.amount) || 0.25,
          currency: p.currency || 'USD',
          payment_method: p.payment_method || 'EVC Plus',
          reference_number: p.reference_number || '',
          status: p.status || 'pending',
          rejection_reason: p.rejection_reason,
          created_at: p.created_at
        };
      });

      setPayments(list);
    } catch (err: any) {
      console.error('Fetch daily payments error:', err);
      setErrorMsg('Cilad ayaa dhacday marka la soo qaadayay lacagaha maanta: ' + (err.message || ''));
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTodayPayments(false);

    const interval = setInterval(() => fetchTodayPayments(true), 8000);

    const channel = supabase
      .channel('realtime_daily_recharges')
      .on('postgres_changes', { event: '*', schema: 'public', table: 'payment_transactions' }, (payload: any) => {
        if (payload?.eventType === 'INSERT') {
          soundAlert.playPaymentSound();
        }
        fetchTodayPayments(true);
      })
      .subscribe();

    return () => {
      clearInterval(interval);
      supabase.removeChannel(channel);
    };
  }, []);

  const handleCopyPhone = (phone: string) => {
    if (!phone) return;
    navigator.clipboard.writeText(phone);
    setCopiedPhone(phone);
    setTimeout(() => setCopiedPhone(null), 2000);
  };

  const handleApprove = async (payment: PaymentTransaction) => {
    if (!canManage || !payment.id) return;
    setProcessingId(payment.id);
    try {
      const { data, error } = await supabase.rpc('approve_driver_pass_payment', {
        p_payment_id: payment.id
      });

      if (error) throw error;
      const res = (data || {}) as any;
      if (res.ok !== true) {
        throw new Error(res.error || 'Ansixinta kuma guulaysanin');
      }

      soundAlert.playPaymentSound();
      await fetchTodayPayments(true);
    } catch (err: any) {
      alert('Cilad ansixinta: ' + err.message);
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (payment: PaymentTransaction) => {
    if (!canManage || !payment.id) return;
    const reason = window.prompt('Fadlan qor sababta loo diiday:', 'Tixraaca lacag-bixinta lama xaqiijin karo');
    if (!reason) return;

    setProcessingId(payment.id);
    try {
      const { data, error } = await supabase.rpc('reject_driver_pass_payment', {
        p_payment_id: payment.id,
        p_reason: reason.trim()
      });

      if (error) throw error;
      await fetchTodayPayments(true);
    } catch (err: any) {
      alert('Cilad diidmada: ' + err.message);
    } finally {
      setProcessingId(null);
    }
  };

  const handleBulkApprove = async () => {
    const pendings = payments.filter(p => p.status === 'pending');
    if (pendings.length === 0 || !canManage) return;

    if (!window.confirm(`Ma hubtaa inaad hal mar wada ansixiso dhammaan ${pendings.length} darawal ee maanta lacagta soo shubtay?`)) {
      return;
    }

    setIsLoading(true);
    let success = 0;
    for (const p of pendings) {
      try {
        await supabase.rpc('approve_driver_pass_payment', { p_payment_id: p.id });
        success++;
      } catch (e) {
        console.error('Bulk error on', p.id, e);
      }
    }
    soundAlert.playPaymentSound();
    await fetchTodayPayments(true);
    setIsLoading(false);
    alert(`Guul! ${success} bixin oo maanta ah ayaa la wada ansixiyay!`);
  };

  const todayCompleted = payments.filter(p => p.status === 'completed');
  const todayPending = payments.filter(p => p.status === 'pending');
  const todayRejected = payments.filter(p => p.status === 'rejected');
  const todayRevenue = todayCompleted.reduce((sum, p) => sum + (p.amount || 0.25), 0);

  const filteredList = payments.filter(p => {
    if (filterStatus !== 'all' && p.status !== filterStatus) return false;
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase().trim();
    return (
      (p.user_name && p.user_name.toLowerCase().includes(q)) ||
      (p.phone_number && p.phone_number.toLowerCase().includes(q)) ||
      (p.reference_number && p.reference_number.toLowerCase().includes(q))
    );
  });

  return (
    <div className="space-y-6">
      {/* Top Header Card */}
      <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <div className="w-10 h-10 rounded-xl bg-emerald-600 text-white flex items-center justify-center shadow-md">
              <Zap className="w-5 h-5 fill-white" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-xl font-black text-black">
                  Darawallada Ku Shubtay Maanta ({payments.length})
                </h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-black bg-emerald-100 text-emerald-800 border border-emerald-300">
                  {todayDateStr}
                </span>
              </div>
              <p className="text-xs text-neutral-500 mt-0.5">
                Liiska goonida ah ee dhammaan darawallada maanta lacagta $0.25 ku shubtay (EVC Plus, Sahal, ZAAD).
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {todayPending.length > 0 && canManage && (
            <button
              onClick={handleBulkApprove}
              disabled={isLoading}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1.5 cursor-pointer shrink-0"
              title="Hal mar wada ansixi maanta kuwa sugaya"
            >
              <CheckCircle2 className="w-4 h-4" />
              <span>Ansixi Dhammaan Maanta ({todayPending.length})</span>
            </button>
          )}

          {onNavigateToDrivers && (
            <button
              onClick={onNavigateToDrivers}
              className="px-3.5 py-2 border border-neutral-300 hover:bg-neutral-50 text-neutral-800 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer shrink-0"
            >
              <Bike className="w-4 h-4 text-[#E50914]" />
              <span>Dhammaan Darawallada</span>
            </button>
          )}

          <button
            onClick={() => {
              const next = !soundEnabled;
              setSoundEnabled(next);
              soundAlert.setEnabled(next);
              if (next) soundAlert.playPaymentSound();
            }}
            className={`p-2 border rounded-xl text-xs font-bold transition flex items-center space-x-1 cursor-pointer shrink-0 ${
              soundEnabled ? 'border-emerald-300 bg-emerald-50 text-emerald-700' : 'border-neutral-200 text-neutral-400'
            }`}
            title={soundEnabled ? 'Codka waa shidan yahay' : 'Codka waa dansan yahay'}
          >
            {soundEnabled ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
          </button>

          <button
            onClick={() => fetchTodayPayments(false)}
            disabled={isLoading}
            className="p-2 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer shrink-0"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="bg-gradient-to-br from-emerald-50 to-white p-5 rounded-2xl border-2 border-emerald-500/40 shadow-xs">
          <p className="text-xs font-black text-emerald-800 uppercase tracking-wider">Tirada Maanta Shubatay</p>
          <p className="text-3xl font-black text-emerald-700 mt-2">{payments.length} Darawal</p>
          <p className="text-[11px] text-emerald-600 font-semibold mt-1">Kuwa maanta lacag soo diray</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Dakhliga Maanta ($0.25)</p>
            <span className="p-1 rounded-lg bg-emerald-50 text-emerald-600">
              <DollarSign className="w-4 h-4" />
            </span>
          </div>
          <p className="text-3xl font-black text-emerald-600 mt-2">${todayRevenue.toFixed(2)} USD</p>
          <p className="text-[11px] text-neutral-400 mt-1">{todayCompleted.length} bixino maanta la ansixiyay</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Sugaya Ansixinta (Pending)</p>
          <p className="text-3xl font-black text-[#E50914] mt-2">{todayPending.length}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Bixino cusub oo maanta u baahan aqbalid</p>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-xs font-bold text-neutral-500 uppercase tracking-wider">La Diiday Maanta (Rejected)</p>
          <p className="text-3xl font-black text-neutral-600 mt-2">{todayRejected.length}</p>
          <p className="text-[11px] text-neutral-400 mt-1">Bixino aan sax ahayn oo la diiday</p>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Filter Chips & Search */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-2 overflow-x-auto pb-1">
          <button
            onClick={() => setFilterStatus('all')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'all'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            Dhammaan Maanta ({payments.length})
          </button>

          <button
            onClick={() => setFilterStatus('pending')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'pending'
                ? 'bg-[#E50914] text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            🟡 Sugaya Ansixin ({todayPending.length})
          </button>

          <button
            onClick={() => setFilterStatus('completed')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'completed'
                ? 'bg-emerald-600 text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            🟢 La Ansixiyay ({todayCompleted.length})
          </button>

          <button
            onClick={() => setFilterStatus('rejected')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${
              filterStatus === 'rejected'
                ? 'bg-neutral-800 text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
            }`}
          >
            🔴 La Diiday ({todayRejected.length})
          </button>
        </div>

        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-neutral-400" />
          <input
            type="text"
            placeholder="Raadi darawal, taleefan, ama Tx ID..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-xl text-xs text-black focus:outline-none focus:bg-white"
          />
        </div>
      </div>

      {/* Daily Payments Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-emerald-600" />
            <p className="text-sm font-semibold">Soo qaadaya bixinnada maanta...</p>
          </div>
        ) : filteredList.length === 0 ? (
          <div className="p-12 text-center">
            <Zap className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Maanta darawal lacag ma soo shubin</h3>
            <p className="text-xs text-neutral-500 mt-1">Bixinnada maanta soo dhaca halkan ayay toos uga soo muuqan doonaan.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[750px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Darawalka</th>
                  <th className="p-4 font-bold">Nambarka Laga Soo Diray</th>
                  <th className="p-4 font-bold">Qiimaha</th>
                  <th className="p-4 font-bold">Habka Bixinta</th>
                  <th className="p-4 font-bold">Transaction Ref</th>
                  <th className="p-4 font-bold">Waqtiga Maanta</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  {canManage && <th className="p-4 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredList.map((payment) => {
                  const isPending = payment.status === 'pending';
                  const isProcessing = processingId === payment.id;
                  const timeFormatted = payment.created_at
                    ? new Date(payment.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                    : 'Hadda';

                  return (
                    <tr 
                      key={payment.id} 
                      className={`transition ${
                        isPending 
                          ? 'bg-amber-50/70 hover:bg-amber-100/60 border-l-4 border-l-[#E50914]' 
                          : 'hover:bg-neutral-50'
                      }`}
                    >
                      <td className="p-4 font-bold text-black">
                        <div className="flex items-center space-x-2.5">
                          <div className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center text-xs font-black shrink-0">
                            {payment.user_name?.charAt(0) || 'D'}
                          </div>
                          <div>
                            <p className="text-black font-bold">{payment.user_name}</p>
                            <p className="text-[11px] text-neutral-400 font-mono">ID: {payment.id.slice(0, 8)}</p>
                          </div>
                        </div>
                      </td>

                      <td className="p-4">
                        <div className="flex items-center space-x-1.5 font-mono text-xs font-bold text-black">
                          <span>{payment.phone_number || 'Lama yaqaan'}</span>
                          {payment.phone_number && (
                            <button
                              onClick={() => handleCopyPhone(payment.phone_number!)}
                              className="p-1 hover:bg-neutral-200 rounded text-neutral-500 hover:text-black cursor-pointer transition"
                              title="Koobiyee lambarka"
                            >
                              {copiedPhone === payment.phone_number ? (
                                <Check className="w-3.5 h-3.5 text-emerald-600" />
                              ) : (
                                <Copy className="w-3.5 h-3.5" />
                              )}
                            </button>
                          )}
                        </div>
                      </td>

                      <td className="p-4">
                        <span className="font-black text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-lg text-xs">
                          ${payment.amount?.toFixed(2) || '0.25'} USD
                        </span>
                      </td>

                      <td className="p-4 text-xs font-bold text-neutral-700">
                        {payment.payment_method || 'EVC Plus'}
                      </td>

                      <td className="p-4 text-xs font-mono text-neutral-600">
                        {payment.reference_number || '—'}
                      </td>

                      <td className="p-4 text-xs font-semibold text-black">
                        <div className="flex items-center space-x-1">
                          <Clock className="w-3.5 h-3.5 text-neutral-400" />
                          <span>{timeFormatted}</span>
                        </div>
                      </td>

                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          payment.status === 'completed'
                            ? 'bg-emerald-100 text-emerald-800'
                            : payment.status === 'pending'
                              ? 'bg-amber-100 text-amber-800 animate-pulse'
                              : 'bg-red-100 text-red-800'
                        }`}>
                          {payment.status === 'completed' ? '🟢 Ansixsan' : payment.status === 'pending' ? '🟡 Sugaysa' : '🔴 La Diiday'}
                        </span>
                      </td>

                      {canManage && (
                        <td className="p-4 text-right">
                          {isPending ? (
                            <div className="flex items-center justify-end space-x-2">
                              <button
                                onClick={() => handleApprove(payment)}
                                disabled={isProcessing}
                                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition cursor-pointer flex items-center space-x-1"
                              >
                                {isProcessing ? (
                                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                                ) : (
                                  <CheckCircle2 className="w-3.5 h-3.5" />
                                )}
                                <span>Ansixi</span>
                              </button>

                              <button
                                onClick={() => handleReject(payment)}
                                disabled={isProcessing}
                                className="px-2.5 py-1.5 border border-red-200 text-red-600 hover:bg-red-50 rounded-xl text-xs font-bold transition cursor-pointer"
                              >
                                Diid
                              </button>
                            </div>
                          ) : (
                            <span className="text-xs text-neutral-400 font-semibold">—</span>
                          )}
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
