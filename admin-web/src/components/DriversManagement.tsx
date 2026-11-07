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
  const [filter, setFilter] = useState<'all' | 'driver' | 'courier' | 'merchant' | 'pending_approval' | 'recent10' | 'active'>('all');
  const [copiedPhone, setCopiedPhone] = useState<string | null>(null);
  const [approvingDriverId, setApprovingDriverId] = useState<string | null>(null);

  // New Merchant Modal state
  const [isNewMerchantModalOpen, setIsNewMerchantModalOpen] = useState(false);
  const [newBizName, setNewBizName] = useState('');
  const [newOwnerName, setNewOwnerName] = useState('');
  const [newBizPhone, setNewBizPhone] = useState('');
  const [newBizType, setNewBizType] = useState('Maqaayad / Cunto');
  const [newDistrict, setNewDistrict] = useState('Hodan');
  const [newTillNumber, setNewTillNumber] = useState('');
  const [isSubmittingMerchant, setIsSubmittingMerchant] = useState(false);

  const fetchDrivers = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('profiles')
        .select('*')
        .in('role', ['driver', 'courier', 'merchant'])
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

  const handleApproveAccount = async (profile: Profile) => {
    if (!profile.id || !canManage) return;
    setApprovingDriverId(profile.id);
    try {
      const { error } = await supabase
        .from('profiles')
        .update({
          verification_status: 'approved',
          status: 'active',
          subscription_status: 'active'
        })
        .eq('id', profile.id);

      if (!error) {
        soundAlert.playSuccess();
        alert(`Akoonka (${profile.business_name || profile.full_name}) si rasmi ah ayaa loo ansixiyay!`);
        await fetchDrivers(true);
      } else {
        alert('Cilad ansixinta: ' + error.message);
      }
    } catch (e: any) {
      alert('Cilad: ' + e.message);
    } finally {
      setApprovingDriverId(null);
    }
  };

  const handleCreateMerchant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newBizName.trim() || !newBizPhone.trim()) {
      alert('Fadlan geli magaca ganacsiga iyo telefoonka.');
      return;
    }
    setIsSubmittingMerchant(true);
    try {
      const cleanPhone = newBizPhone.trim().replace(/\s+/g, '');
      const e164 = cleanPhone.startsWith('+') ? cleanPhone : (cleanPhone.startsWith('252') ? `+${cleanPhone}` : `+252${cleanPhone}`);
      const fakeId = 'biz_' + Date.now();

      const { error } = await supabase.from('profiles').insert([
        {
          id: fakeId,
          full_name: newOwnerName.trim() || newBizName.trim(),
          phone_number: e164,
          role: 'merchant',
          business_name: newBizName.trim(),
          business_type: newBizType,
          district: newDistrict.trim(),
          merchant_payment_number: newTillNumber.trim() || cleanPhone,
          status: 'active',
          verification_status: 'approved',
          subscription_status: 'active',
          monthly_fee: 5.00
        }
      ]);

      if (error) throw error;
      soundAlert.playSuccess();
      alert(`Maqaayadda / Ganacsiga "${newBizName}" si guul leh ayaa loo diiwaangeliyay!`);
      setIsNewMerchantModalOpen(false);
      setNewBizName('');
      setNewOwnerName('');
      setNewBizPhone('');
      setNewTillNumber('');
      await fetchDrivers(true);
    } catch (err: any) {
      alert('Diiwaangelinta maqaayaddu way fashilantay: ' + err.message);
    } finally {
      setIsSubmittingMerchant(false);
    }
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
      ? `Ma hubtaa inaad joojiso (Suspend) akoonka "${driver.business_name || driver.full_name}"?`
      : `Ma hubtaa inaad dib u hawlgeliso (Activate) "${driver.business_name || driver.full_name}"?`;

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

  const bajajCount = drivers.filter(d => d.role === 'driver').length;
  const courierCount = drivers.filter(d => d.role === 'courier').length;
  const merchantCount = drivers.filter(d => d.role === 'merchant').length;
  const pendingApprovalCount = drivers.filter(d =>
    d.verification_status === 'pending_approval' ||
    d.verification_status === 'pending_verification' ||
    d.status === 'pending'
  ).length;
  const recentDriversCount = drivers.filter(d => isWithinMinutes(d.created_at, 10)).length;

  const filteredDrivers = drivers
    .filter(d => {
      if (filter === 'driver' && d.role !== 'driver') return false;
      if (filter === 'courier' && d.role !== 'courier') return false;
      if (filter === 'merchant' && d.role !== 'merchant') return false;
      if (filter === 'pending_approval' &&
        d.verification_status !== 'pending_approval' &&
        d.verification_status !== 'pending_verification' &&
        d.status !== 'pending') return false;
      if (filter === 'recent10' && !isWithinMinutes(d.created_at, 10)) return false;
      if (filter === 'active' && d.subscription_status !== 'active') return false;

      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase().trim();
      return (
        (d.full_name && d.full_name.toLowerCase().includes(q)) ||
        (d.business_name && d.business_name.toLowerCase().includes(q)) ||
        (d.phone_number && d.phone_number.toLowerCase().includes(q)) ||
        (d.plate_number && d.plate_number.toLowerCase().includes(q)) ||
        (d.merchant_payment_number && d.merchant_payment_number.toLowerCase().includes(q)) ||
        (d.license_number && d.license_number.toLowerCase().includes(q)) ||
        (d.cid_letter_number && d.cid_letter_number.toLowerCase().includes(q))
      );
    });

  return (
    <div className="space-y-6">
      {/* Top Header Card */}
      <div className="bg-white p-5 sm:p-6 rounded-2xl border border-neutral-200 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2.5">
            <div className="w-10 h-10 rounded-xl bg-[#DC2626] text-white flex items-center justify-center shadow-md">
              <Bike className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-xl font-black text-black">
                Gaadiidka & Ganacsiyada ({drivers.length})
              </h2>
              <p className="text-xs text-neutral-500 mt-0.5">
                SHIRI Fleet • Bajaajta ($0.25), Mootooyinka ($5.00) & Maqaayadaha ($5.00).
              </p>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 w-full md:w-auto">
          {canManage && (
            <button
              onClick={() => setIsNewMerchantModalOpen(true)}
              className="px-4 py-2 bg-[#DC2626] hover:bg-[#B70011] text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1.5 cursor-pointer shrink-0"
            >
              <span>+ Diiwaangeli Maqaayad ($5)</span>
            </button>
          )}

          {onNavigateToDailyPaid && (
            <button
              onClick={onNavigateToDailyPaid}
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-black shadow transition flex items-center space-x-1.5 cursor-pointer shrink-0"
            >
              <Zap className="w-3.5 h-3.5 fill-white" />
              <span>Ku Shubtay Maanta ($0.25)</span>
            </button>
          )}

          <button
            onClick={() => fetchDrivers(false)}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer shrink-0"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#DC2626]' : ''}`} />
          </button>
        </div>
      </div>

      {/* KPI Cards Row */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[11px] font-bold text-neutral-500 uppercase tracking-wider">Wadarta Guud</p>
          <p className="text-2xl font-black text-black mt-1">{drivers.length}</p>
          <p className="text-[10px] text-neutral-400 mt-0.5">Bajaaj, Mooto & Ganacsi</p>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[11px] font-bold text-amber-600 uppercase tracking-wider">🛺 Bajaajle ($0.25)</p>
          <p className="text-2xl font-black text-black mt-1">{bajajCount}</p>
          <p className="text-[10px] text-neutral-400 mt-0.5">Gaadiidka Rakaabka</p>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[11px] font-bold text-blue-600 uppercase tracking-wider">🏍️ Mooto ($5.00)</p>
          <p className="text-2xl font-black text-black mt-1">{courierCount}</p>
          <p className="text-[10px] text-neutral-400 mt-0.5">Gaarsiiyayaasha Cuntada</p>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[11px] font-bold text-purple-600 uppercase tracking-wider">🏪 Maqaayado ($5.00)</p>
          <p className="text-2xl font-black text-black mt-1">{merchantCount}</p>
          <p className="text-[10px] text-neutral-400 mt-0.5">Ganacsiyada ku xiran</p>
        </div>

        <div className="bg-gradient-to-br from-red-50 to-white p-4 rounded-2xl border border-red-200 shadow-xs">
          <p className="text-[11px] font-bold text-[#DC2626] uppercase tracking-wider">⏳ Sugaya Ansixin</p>
          <p className="text-2xl font-black text-[#DC2626] mt-1">{pendingApprovalCount}</p>
          <p className="text-[10px] text-red-500 mt-0.5">U baahan ansixin</p>
        </div>
      </div>

      {/* Search & Filter Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Filters */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1">
          <button
            onClick={() => setFilter('all')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${filter === 'all'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
              }`}
          >
            Dhammaan ({drivers.length})
          </button>

          <button
            onClick={() => setFilter('driver')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${filter === 'driver'
                ? 'bg-black text-white'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
              }`}
          >
            🛺 Bajaajle ({bajajCount})
          </button>

          <button
            onClick={() => setFilter('courier')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${filter === 'courier'
                ? 'bg-blue-600 text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
              }`}
          >
            🏍️ Mooto Courier ({courierCount})
          </button>

          <button
            onClick={() => setFilter('merchant')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${filter === 'merchant'
                ? 'bg-purple-600 text-white shadow'
                : 'bg-white text-neutral-600 hover:bg-neutral-100 border border-neutral-200'
              }`}
          >
            🏪 Maqaayadaha ({merchantCount})
          </button>

          <button
            onClick={() => setFilter('pending_approval')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer shrink-0 ${filter === 'pending_approval'
                ? 'bg-[#DC2626] text-white shadow'
                : 'bg-red-50 text-red-700 hover:bg-red-100 border border-red-200'
              }`}
          >
            ⏳ Sugaya Ansixin ({pendingApprovalCount})
          </button>
        </div>

        {/* Search Input */}
        <div className="relative w-full sm:w-72">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-neutral-400" />
          <input
            type="text"
            placeholder="Raadi ganacsi, darawal, nambar ama till..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-neutral-100 border border-neutral-200 rounded-xl text-xs text-black focus:outline-none focus:bg-white"
          />
        </div>
      </div>

      {/* Fleet & Merchants Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#DC2626]" />
            <p className="text-sm font-semibold">Soo qaadaya xogta...</p>
          </div>
        ) : filteredDrivers.length === 0 ? (
          <div className="p-12 text-center">
            <Bike className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Waxba lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1">Ma jirto xog ku habboon shaandhadaada.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[850px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-3.5 font-bold">Magaca & Doorka</th>
                  <th className="p-3.5 font-bold">Taleefanka & Till Number</th>
                  <th className="p-3.5 font-bold">Faahfaahinta / Liisanka</th>
                  <th className="p-3.5 font-bold">Rukhumadda</th>
                  <th className="p-3.5 font-bold">Ansixinta</th>
                  {canManage && <th className="p-3.5 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {filteredDrivers.map((driver) => {
                  const isRecent = isWithinMinutes(driver.created_at, 10);
                  const isMerchantRole = driver.role === 'merchant';
                  const isCourierRole = driver.role === 'courier';
                  const isDriverRole = driver.role === 'driver';
                  const isPendingApproval = driver.verification_status === 'pending_approval' ||
                    driver.verification_status === 'pending_verification' ||
                    driver.status === 'pending';
                  const isApproving = approvingDriverId === driver.id;

                  return (
                    <tr
                      key={driver.id}
                      className={`transition ${isRecent
                          ? 'bg-emerald-50/80 hover:bg-emerald-100/70 border-l-4 border-l-emerald-500'
                          : 'hover:bg-neutral-50'
                        }`}
                    >
                      <td className="p-3.5">
                        <div className="flex items-center space-x-3">
                          <div className={`w-9 h-9 rounded-full text-white flex items-center justify-center text-xs font-black shrink-0 ${isMerchantRole ? 'bg-purple-600' : isCourierRole ? 'bg-blue-600' : 'bg-[#DC2626]'
                            }`}>
                            {isMerchantRole ? '🏪' : isCourierRole ? '🏍️' : '🛺'}
                          </div>
                          <div>
                            <div className="flex items-center space-x-1.5">
                              <span className="font-bold text-black">
                                {driver.business_name || driver.full_name}
                              </span>
                              {isRecent && (
                                <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded-md bg-emerald-200 text-emerald-900 text-[10px] font-black border border-emerald-400">
                                  <Sparkles className="w-3 h-3 text-emerald-700" />
                                  CUSUB
                                </span>
                              )}
                            </div>
                            <div className="flex items-center gap-1.5 mt-0.5">
                              <span className={`px-1.5 py-0.2 rounded text-[10px] font-bold uppercase ${isMerchantRole ? 'bg-purple-100 text-purple-800' : isCourierRole ? 'bg-blue-100 text-blue-800' : 'bg-neutral-100 text-neutral-800'
                                }`}>
                                {isMerchantRole ? 'Maqaayad' : isCourierRole ? 'Mooto Courier' : 'Bajaajle'}
                              </span>
                              {driver.business_type && (
                                <span className="text-[11px] text-neutral-500">({driver.business_type})</span>
                              )}
                            </div>
                          </div>
                        </div>
                      </td>

                      <td className="p-3.5">
                        <div className="flex flex-col text-xs">
                          <div className="flex items-center space-x-1 font-mono font-bold text-black">
                            <span>{driver.phone_number}</span>
                            <button
                              onClick={() => handleCopyPhone(driver.phone_number)}
                              className="p-1 hover:bg-neutral-200 rounded text-neutral-500 hover:text-black cursor-pointer transition"
                              title="Koobiyee"
                            >
                              {copiedPhone === driver.phone_number ? (
                                <Check className="w-3 h-3 text-emerald-600" />
                              ) : (
                                <Copy className="w-3 h-3" />
                              )}
                            </button>
                          </div>
                          {driver.merchant_payment_number && (
                            <div className="mt-1 flex items-center gap-1 text-[11px] text-emerald-700 font-bold bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200 w-fit">
                              <CreditCard className="w-3 h-3" />
                              <span>Till: {driver.merchant_payment_number}</span>
                            </div>
                          )}
                        </div>
                      </td>

                      <td className="p-3.5 text-xs">
                        {isMerchantRole ? (
                          <div>
                            <p className="font-bold text-black">{driver.district ? `Degmada ${driver.district}` : 'Muqdisho'}</p>
                            <p className="text-[11px] text-neutral-500">Mulkiile: {driver.full_name}</p>
                          </div>
                        ) : isCourierRole ? (
                          <div>
                            <p className="font-bold text-black">
                              {driver.license_number ? `Shati: ${driver.license_number}` : 'Shati la\'aan'}
                            </p>
                            <p className="text-[11px] text-neutral-500">
                              CID: {driver.cid_letter_number || 'N/A'} • Damiin: {driver.guarantor_name || 'N/A'}
                            </p>
                          </div>
                        ) : (
                          <div>
                            <p className="font-bold text-black">
                              {driver.plate_number ? `Taargo: ${driver.plate_number}` : 'Taargo la\'aan'}
                            </p>
                            <p className="text-[11px] text-neutral-500">{driver.bajaaj_model || 'Bajaaj Caadi'}</p>
                          </div>
                        )}
                      </td>

                      <td className="p-3.5 text-xs">
                        <span className="font-black text-black">
                          {isMerchantRole ? '$5.00 / Bil' : isCourierRole ? '$5.00 / Bil' : '$0.25 / Maalin'}
                        </span>
                        <p className="text-[10px] text-neutral-500">
                          {driver.subscription_status === 'active' ? '🟢 Bixiyay' : '⚪ Sugaya'}
                        </p>
                      </td>

                      <td className="p-3.5">
                        <span className={`px-2 py-0.5 rounded-lg text-xs font-bold ${isPendingApproval
                            ? 'bg-amber-100 text-amber-800 border border-amber-300'
                            : 'bg-emerald-100 text-emerald-800'
                          }`}>
                          {isPendingApproval ? '⏳ Sugaya Ansixin' : '✅ Ansixan'}
                        </span>
                      </td>

                      {canManage && (
                        <td className="p-3.5 text-right">
                          <div className="flex items-center justify-end space-x-2">
                            {isPendingApproval && (
                              <button
                                onClick={() => handleApproveAccount(driver)}
                                disabled={isApproving}
                                className="px-2.5 py-1 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-black shadow transition cursor-pointer flex items-center space-x-1"
                              >
                                <CheckCircle2 className="w-3.5 h-3.5" />
                                <span>{isApproving ? '...' : 'Anxi (Approve)'}</span>
                              </button>
                            )}

                            <button
                              onClick={() => handleToggleStatus(driver)}
                              className={`px-2.5 py-1 text-xs font-bold rounded-lg border transition cursor-pointer ${driver.status === 'active'
                                  ? 'border-amber-200 text-amber-700 hover:bg-amber-50'
                                  : 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                                }`}
                            >
                              {driver.status === 'active' ? 'Jooji' : 'Fasax'}
                            </button>
                          </div>
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

      {/* Modal: Diiwaangeli Ganacsi / Maqaayad Cusub */}
      {isNewMerchantModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="text-lg font-black text-black">🏪 Diiwaangeli Ganacsi / Maqaayad Cusub</h3>
              <button
                onClick={() => setIsNewMerchantModalOpen(false)}
                className="text-neutral-400 hover:text-black font-black text-xl"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateMerchant} className="space-y-3">
              <div>
                <label className="text-xs font-bold text-neutral-700 block mb-1">Magaca Maqaayadda ama Dukaanka</label>
                <input
                  type="text"
                  required
                  placeholder="Tusaale: Maqaayadda Liido ama Somali Fresh"
                  value={newBizName}
                  onChange={(e) => setNewBizName(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-neutral-700 block mb-1">Nooca Ganacsiga</label>
                  <select
                    value={newBizType}
                    onChange={(e) => setNewBizType(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                  >
                    <option value="Maqaayad / Cunto">Maqaayad / Cunto</option>
                    <option value="Fast Food & Cafe">Fast Food & Cafe</option>
                    <option value="Dukaan / Grocery">Dukaan / Grocery</option>
                    <option value="Farmashiye">Farmashiye</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs font-bold text-neutral-700 block mb-1">Degmada</label>
                  <input
                    type="text"
                    required
                    placeholder="Hodan, Waaberi..."
                    value={newDistrict}
                    onChange={(e) => setNewDistrict(e.target.value)}
                    className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-neutral-700 block mb-1">Magaca Mulkiilaha ama Maamulaha</label>
                <input
                  type="text"
                  placeholder="Magaca qofka mas'uulka ah"
                  value={newOwnerName}
                  onChange={(e) => setNewOwnerName(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-neutral-700 block mb-1">Telefoonka Xiriirka (+252)</label>
                <input
                  type="text"
                  required
                  placeholder="61XXXXXXX"
                  value={newBizPhone}
                  onChange={(e) => setNewBizPhone(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-neutral-700 block mb-1">
                  Nambarka Till-ka / Lacag-qabashada ee Macmiilka
                </label>
                <input
                  type="text"
                  placeholder="Nambarka EVC/eDahab ama Till-ka lacagta cuntada"
                  value={newTillNumber}
                  onChange={(e) => setNewTillNumber(e.target.value)}
                  className="w-full px-3 py-2 border rounded-xl text-xs focus:outline-none focus:border-[#DC2626]"
                />
                <p className="text-[11px] text-neutral-500 mt-0.5">
                  Nambarkan ayay macaamiishu lacagta cuntada toos ugu dirayaan.
                </p>
              </div>

              <div className="bg-neutral-50 p-3 rounded-xl border text-xs text-neutral-600">
                <p className="font-bold text-black">💰 Qiimaha Rukhumadda SHIRI: $5.00 / Bishii</p>
                <p className="text-[11px] text-neutral-500">Akoonka maqaayadda toos ayaa loo ansixinayaa marka la abuuro.</p>
              </div>

              <div className="flex items-center justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsNewMerchantModalOpen(false)}
                  className="px-4 py-2 border rounded-xl text-xs font-bold hover:bg-neutral-100"
                >
                  Ka Noqo
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingMerchant}
                  className="px-5 py-2 bg-[#DC2626] hover:bg-[#B70011] text-white rounded-xl text-xs font-black shadow"
                >
                  {isSubmittingMerchant ? 'Diiwaangelinaya...' : 'Keydi Ganacsiga ($5)'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
