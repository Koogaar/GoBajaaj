import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import {
  Package, Bike, RefreshCw, CheckCircle2, Clock, XCircle,
  MapPin, Phone, DollarSign, Star, Eye, ShieldCheck, AlertCircle
} from 'lucide-react';

// ── Types ──────────────────────────────────────────────────────────────────
interface DeliveryOrder {
  id: string;
  order_number: string;
  customer_name: string;
  customer_phone: string;
  merchant_name: string;
  courier_name: string | null;
  pickup_address: string;
  delivery_address: string;
  total_amount: number;
  delivery_fee: number;
  status: 'pending' | 'assigned' | 'picked_up' | 'delivered' | 'cancelled';
  created_at: string;
}

interface DeliveryCourier {
  id: string;
  full_name: string;
  phone_number: string;
  vehicle_type: string;
  is_online: boolean;
  today_earnings: number;
  completed_deliveries: number;
  rating: number;
  verification_status: string;
  subscription_status: string;
}

interface ShiriDeliveryManagementProps {
  actorName: string;
  canManage: boolean;
}

// ── Status badge helper ────────────────────────────────────────────────────
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const map: Record<string, { label: string; cls: string }> = {
    pending:   { label: '⏳ Sugaya',       cls: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
    assigned:  { label: '🏍️ La Qoondeeyay', cls: 'bg-blue-100 text-blue-800 border-blue-300' },
    picked_up: { label: '📦 La Qaatay',    cls: 'bg-purple-100 text-purple-800 border-purple-300' },
    delivered: { label: '✅ La Gaarsiyay', cls: 'bg-green-100 text-green-800 border-green-300' },
    cancelled: { label: '❌ La Joojiyay',  cls: 'bg-red-100 text-red-800 border-red-300' },
    pending_approval: { label: '⏳ Admin Sugaya', cls: 'bg-orange-100 text-orange-800 border-orange-300' },
    approved:  { label: '✅ Oggolaadey',   cls: 'bg-green-100 text-green-800 border-green-300' },
    rejected:  { label: '❌ Diiday',       cls: 'bg-red-100 text-red-800 border-red-300' },
    active:    { label: '🟢 Firfircoon',   cls: 'bg-green-100 text-green-800 border-green-300' },
    inactive:  { label: '⚫ Offline',       cls: 'bg-neutral-100 text-neutral-700 border-neutral-300' },
  };
  const s = map[status] ?? { label: status, cls: 'bg-neutral-100 text-neutral-700 border-neutral-300' };
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold border ${s.cls}`}>
      {s.label}
    </span>
  );
};

// ── Mock data ──────────────────────────────────────────────────────────────
const MOCK_ORDERS: DeliveryOrder[] = [
  { id: '1', order_number: 'SHR-ORD-00142', customer_name: 'Fadumo Axmed', customer_phone: '0618091234', merchant_name: 'Maqaayada Xaafadda', courier_name: 'Maxamed Courier', pickup_address: 'Hodan, Muqdisho', delivery_address: 'Waaberi, Muqdisho', total_amount: 15.00, delivery_fee: 2.50, status: 'delivered', created_at: new Date(Date.now() - 3600000).toISOString() },
  { id: '2', order_number: 'SHR-ORD-00143', customer_name: 'Cali Maxamed', customer_phone: '0615559012', merchant_name: 'Supermarket Hodan', courier_name: 'Faarax Courier', pickup_address: 'Hodan Market', delivery_address: 'Daynile, Muqdisho', total_amount: 32.00, delivery_fee: 3.50, status: 'picked_up', created_at: new Date(Date.now() - 1800000).toISOString() },
  { id: '3', order_number: 'SHR-ORD-00144', customer_name: 'Hawa Jama', customer_phone: '0617771234', merchant_name: 'Pizza House Mogadishu', courier_name: null, pickup_address: 'Xamarweyne', delivery_address: 'Hodan District', total_amount: 22.00, delivery_fee: 2.00, status: 'pending', created_at: new Date(Date.now() - 600000).toISOString() },
  { id: '4', order_number: 'SHR-ORD-00145', customer_name: 'Aamina Xasan', customer_phone: '0612345678', merchant_name: 'Farmashiye Waaberi', courier_name: null, pickup_address: 'Waaberi Pharmacy', delivery_address: 'KM4, Muqdisho', total_amount: 8.50, delivery_fee: 1.50, status: 'pending', created_at: new Date(Date.now() - 300000).toISOString() },
];

const MOCK_COURIERS: DeliveryCourier[] = [
  { id: 'c1', full_name: 'Maxamed Axmed Xasan', phone_number: '0618092060', vehicle_type: 'Mooto', is_online: true, today_earnings: 49.00, completed_deliveries: 14, rating: 4.9, verification_status: 'approved', subscription_status: 'active' },
  { id: 'c2', full_name: 'Cali Xasan Cilmi', phone_number: '0615551234', vehicle_type: 'Mooto', is_online: true, today_earnings: 32.50, completed_deliveries: 9, rating: 4.8, verification_status: 'approved', subscription_status: 'active' },
  { id: 'c3', full_name: 'Sahra Nuur Warsame', phone_number: '0617889900', vehicle_type: 'Mooto', is_online: false, today_earnings: 0, completed_deliveries: 0, rating: 0, verification_status: 'pending_approval', subscription_status: 'pending' },
];

// ── Main Component ─────────────────────────────────────────────────────────
export const ShiriDeliveryManagement: React.FC<ShiriDeliveryManagementProps> = ({ actorName, canManage }) => {
  const [subTab, setSubTab] = useState<'orders' | 'couriers'>('orders');
  const [orders, setOrders] = useState<DeliveryOrder[]>([]);
  const [couriers, setCouriers] = useState<DeliveryCourier[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  useEffect(() => { fetchData(); }, [subTab]);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      if (subTab === 'orders') {
        const { data, error } = await supabase
          .from('delivery_orders')
          .select('*')
          .order('created_at', { ascending: false })
          .limit(100);
        setOrders(!error && data?.length ? data : MOCK_ORDERS);
      } else {
        const { data, error } = await supabase
          .from('users')
          .select('*')
          .eq('role', 'courier')
          .order('created_at', { ascending: false });
        setCouriers(!error && data?.length ? data.map((u: any) => ({
          id: u.id,
          full_name: u.full_name ?? u.name ?? 'Magac la\'aan',
          phone_number: u.phone_number ?? '',
          vehicle_type: u.vehicle_model ?? 'Mooto',
          is_online: u.is_online ?? false,
          today_earnings: u.today_earnings ?? 0,
          completed_deliveries: u.completed_trips ?? 0,
          rating: u.rating ?? 0,
          verification_status: u.verification_status ?? 'pending_approval',
          subscription_status: u.subscription_status ?? 'pending',
        })) : MOCK_COURIERS);
      }
    } catch (e) {
      setOrders(MOCK_ORDERS);
      setCouriers(MOCK_COURIERS);
    } finally {
      setIsLoading(false);
    }
  };

  const approveCourier = async (courierId: string, name: string) => {
    if (!canManage) return;
    try {
      await supabase.from('users').update({ verification_status: 'approved', subscription_status: 'active' }).eq('id', courierId);
      setCouriers(prev => prev.map(c => c.id === courierId ? { ...c, verification_status: 'approved', subscription_status: 'active' } : c));
      setActionMsg(`✅ ${name} waa la oggolaadey!`);
      setTimeout(() => setActionMsg(null), 4000);
    } catch (e) {
      setActionMsg('❌ Khalad ayaa dhacay. Dib u isku day.');
    }
  };

  const rejectCourier = async (courierId: string, name: string) => {
    if (!canManage) return;
    try {
      await supabase.from('users').update({ verification_status: 'rejected', subscription_status: 'inactive' }).eq('id', courierId);
      setCouriers(prev => prev.map(c => c.id === courierId ? { ...c, verification_status: 'rejected', subscription_status: 'inactive' } : c));
      setActionMsg(`❌ ${name} waa la diiday.`);
      setTimeout(() => setActionMsg(null), 4000);
    } catch (e) {
      setActionMsg('Khalad ayaa dhacay.');
    }
  };

  const filteredOrders = filterStatus === 'all' ? orders : orders.filter(o => o.status === filterStatus);
  const pendingCouriers = couriers.filter(c => c.verification_status === 'pending_approval');
  const activeCouriers = couriers.filter(c => c.is_online);
  const totalEarningsToday = couriers.reduce((sum, c) => sum + c.today_earnings, 0);

  return (
    <div className="space-y-6">
      {/* ── Header ── */}
      <div className="bg-gradient-to-r from-red-700 via-red-600 to-red-800 text-white p-6 rounded-2xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-1 bg-white/20 text-white text-[10px] font-black rounded-lg uppercase tracking-wider">
              🚀 SHIRI Delivery
            </span>
            <span className="text-xs text-red-200 font-medium">Maamulka Gaarsiinta</span>
          </div>
          <h2 className="text-2xl font-black text-white flex items-center gap-2">
            📦 SHIRI Delivery — Maamulka Dalabyada &amp; Couriers
          </h2>
          <p className="text-xs text-red-100 mt-1 leading-relaxed">
            Dalabyada tooska ah raac, couriers-ka oggolow ama dii, dakhliga maanta eeg.
          </p>
        </div>
        <button
          onClick={fetchData}
          disabled={isLoading}
          className="px-4 py-2 bg-white/20 hover:bg-white/30 text-white text-xs font-bold rounded-xl border border-white/30 transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shrink-0"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Dib u soo cusbooneysii
        </button>
      </div>

      {/* ── Stats Row ── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Dalabyada Jira', value: orders.filter(o => o.status === 'pending').length, icon: '⏳', color: 'bg-yellow-50 border-yellow-200 text-yellow-700' },
          { label: 'Couriers Online', value: activeCouriers.length, icon: '🏍️', color: 'bg-green-50 border-green-200 text-green-700' },
          { label: 'Oggolaanshiis Sugaya', value: pendingCouriers.length, icon: '🔔', color: 'bg-orange-50 border-orange-200 text-orange-700' },
          { label: 'Dakhliga Maanta', value: `$${totalEarningsToday.toFixed(2)}`, icon: '💰', color: 'bg-blue-50 border-blue-200 text-blue-700' },
        ].map(stat => (
          <div key={stat.label} className={`${stat.color} border rounded-xl p-4 flex items-center gap-3`}>
            <span className="text-2xl">{stat.icon}</span>
            <div>
              <div className="text-xl font-black">{stat.value}</div>
              <div className="text-[11px] font-semibold">{stat.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* ── Pending Couriers Alert ── */}
      {pendingCouriers.length > 0 && (
        <div className="p-4 bg-orange-50 border-2 border-orange-400 rounded-2xl flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-orange-600 shrink-0 mt-0.5" />
          <div>
            <div className="text-sm font-black text-orange-900">
              🔔 {pendingCouriers.length} Courier oo Oggolaanshiis Sugaya!
            </div>
            <div className="text-xs text-orange-700 mt-0.5">
              Couriers-kan waxay diiwaangeliyeen laakiin oggolaanshahaaga ayaa sugaya. Tab-ka "Couriers" ka eeg.
            </div>
          </div>
        </div>
      )}

      {actionMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-400 rounded-xl text-sm font-bold text-emerald-900">
          {actionMsg}
        </div>
      )}

      {/* ── Sub-Tabs ── */}
      <div className="flex gap-2 bg-neutral-100 p-1 rounded-xl w-fit">
        {[
          { id: 'orders', label: '📦 Dalabyada Live', count: orders.filter(o => o.status === 'pending').length },
          { id: 'couriers', label: '🏍️ Couriers', count: pendingCouriers.length },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setSubTab(tab.id as 'orders' | 'couriers')}
            className={`px-4 py-2 rounded-lg text-xs font-black transition cursor-pointer flex items-center gap-1.5 ${
              subTab === tab.id ? 'bg-red-600 text-white shadow' : 'text-neutral-600 hover:bg-neutral-200'
            }`}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-black ${subTab === tab.id ? 'bg-white text-red-600' : 'bg-red-600 text-white'}`}>
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* ── ORDERS TAB ── */}
      {subTab === 'orders' && (
        <div className="space-y-4">
          {/* Filter */}
          <div className="flex flex-wrap gap-2">
            {[
              { v: 'all', l: 'Dhammaanba' },
              { v: 'pending', l: '⏳ Sugaya' },
              { v: 'assigned', l: '🏍️ La Qoondeeyay' },
              { v: 'picked_up', l: '📦 La Qaatay' },
              { v: 'delivered', l: '✅ La Gaarsiyay' },
              { v: 'cancelled', l: '❌ La Joojiyay' },
            ].map(f => (
              <button
                key={f.v}
                onClick={() => setFilterStatus(f.v)}
                className={`px-3 py-1 rounded-full text-[11px] font-bold border transition cursor-pointer ${
                  filterStatus === f.v
                    ? 'bg-red-600 text-white border-red-600'
                    : 'bg-white text-neutral-600 border-neutral-300 hover:border-red-400'
                }`}
              >
                {f.l}
              </button>
            ))}
          </div>

          {/* Orders List */}
          {isLoading ? (
            <div className="text-center py-12 text-neutral-400">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2" />
              <p className="text-sm">Dalabyada soo xidanaya...</p>
            </div>
          ) : filteredOrders.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl border border-neutral-200">
              <Package className="w-10 h-10 mx-auto text-neutral-300 mb-2" />
              <p className="text-sm text-neutral-500">Dalab ma jiro xaaladdan.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {filteredOrders.map(order => (
                <div key={order.id} className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs hover:shadow-sm transition">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-black text-neutral-900">{order.order_number}</span>
                        <StatusBadge status={order.status} />
                      </div>
                      <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-1 text-xs text-neutral-600">
                        <div className="flex items-center gap-1">
                          <span className="font-semibold">👤 Macmiil:</span> {order.customer_name}
                          <a href={`tel:${order.customer_phone}`} className="ml-1 text-blue-600 hover:underline">
                            <Phone className="w-3 h-3 inline" /> {order.customer_phone}
                          </a>
                        </div>
                        <div className="flex items-center gap-1">
                          <span className="font-semibold">🏪 Dukaan:</span> {order.merchant_name}
                        </div>
                        <div className="flex items-center gap-1">
                          <MapPin className="w-3 h-3 text-red-500 shrink-0" />
                          <span className="font-semibold">Ururinta:</span> {order.pickup_address}
                        </div>
                        <div className="flex items-center gap-1">
                          <MapPin className="w-3 h-3 text-green-600 shrink-0" />
                          <span className="font-semibold">Gaarsiinta:</span> {order.delivery_address}
                        </div>
                        {order.courier_name && (
                          <div className="flex items-center gap-1">
                            <Bike className="w-3 h-3 text-blue-500 shrink-0" />
                            <span className="font-semibold">Courier:</span> {order.courier_name}
                          </div>
                        )}
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <div className="text-sm font-black text-neutral-900">${order.total_amount.toFixed(2)}</div>
                      <div className="text-[11px] text-neutral-500">+${order.delivery_fee.toFixed(2)} Gaarsiinta</div>
                      <div className="text-[10px] text-neutral-400 mt-1">
                        {new Date(order.created_at).toLocaleTimeString('so-SO', { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── COURIERS TAB ── */}
      {subTab === 'couriers' && (
        <div className="space-y-3">
          {isLoading ? (
            <div className="text-center py-12 text-neutral-400">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2" />
              <p className="text-sm">Couriers-ka soo xidanaya...</p>
            </div>
          ) : couriers.length === 0 ? (
            <div className="text-center py-12 bg-white rounded-2xl border border-neutral-200">
              <Bike className="w-10 h-10 mx-auto text-neutral-300 mb-2" />
              <p className="text-sm text-neutral-500">Courier ma jiro.</p>
            </div>
          ) : (
            couriers.map(courier => (
              <div
                key={courier.id}
                className={`bg-white rounded-2xl border p-4 shadow-xs transition ${
                  courier.verification_status === 'pending_approval'
                    ? 'border-orange-300 ring-1 ring-orange-200'
                    : 'border-neutral-200'
                }`}
              >
                <div className="flex items-start justify-between gap-3 flex-wrap">
                  <div className="flex items-center gap-3">
                    {/* Avatar */}
                    <div className={`w-11 h-11 rounded-xl flex items-center justify-center font-black text-lg ${
                      courier.is_online ? 'bg-green-100 text-green-700' : 'bg-neutral-100 text-neutral-500'
                    }`}>
                      🏍️
                    </div>
                    <div>
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-black text-neutral-900">{courier.full_name}</span>
                        <StatusBadge status={courier.verification_status} />
                        {courier.is_online && <span className="text-[10px] font-bold text-green-600 bg-green-50 border border-green-200 px-1.5 py-0.5 rounded-full">🟢 Online</span>}
                      </div>
                      <div className="text-xs text-neutral-500 mt-0.5 flex items-center gap-3 flex-wrap">
                        <span><Phone className="w-3 h-3 inline mr-0.5" />{courier.phone_number}</span>
                        <span>🏍️ {courier.vehicle_type}</span>
                        {courier.rating > 0 && (
                          <span className="flex items-center gap-0.5">
                            <Star className="w-3 h-3 text-yellow-500 fill-yellow-500" />
                            {courier.rating.toFixed(1)}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-4 flex-wrap">
                    {/* Earnings & Deliveries */}
                    <div className="text-right text-xs">
                      <div className="font-black text-neutral-900">${courier.today_earnings.toFixed(2)}</div>
                      <div className="text-neutral-400">Maanta</div>
                    </div>
                    <div className="text-right text-xs">
                      <div className="font-black text-neutral-900">{courier.completed_deliveries}</div>
                      <div className="text-neutral-400">Deliveries</div>
                    </div>

                    {/* Action Buttons for Pending Couriers */}
                    {courier.verification_status === 'pending_approval' && canManage && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => approveCourier(courier.id, courier.full_name)}
                          className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white text-xs font-bold rounded-lg transition cursor-pointer"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          Oggolow
                        </button>
                        <button
                          onClick={() => rejectCourier(courier.id, courier.full_name)}
                          className="flex items-center gap-1 px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-lg transition cursor-pointer"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                          Dii
                        </button>
                      </div>
                    )}
                    {courier.verification_status === 'approved' && (
                      <div className="flex items-center gap-1 text-xs text-green-700 font-bold">
                        <ShieldCheck className="w-4 h-4" /> Oggolaadey
                      </div>
                    )}
                  </div>
                </div>

                {/* Subscription info for pending */}
                {courier.verification_status === 'pending_approval' && (
                  <div className="mt-3 pt-3 border-t border-orange-100 text-xs text-orange-700 bg-orange-50 rounded-lg p-2">
                    ℹ️ Courier-kan wuxuu diiwaangeliyey laakiin oggolaanshahaaga ayaa sugaya. 
                    Ka dib oggolaanshaha, waxaa laga dalbanayaa <strong>$3.00/Bishii</strong> subscription.
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
};

