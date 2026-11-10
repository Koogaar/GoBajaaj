import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import {
  Store, CheckCircle2, XCircle, Search, RefreshCw, Package,
  Star, MapPin, Phone, ShieldCheck, TrendingUp, DollarSign,
  Clock, AlertCircle, Eye, Edit3, X, Trash2, Plus, ChevronDown,
  ChevronUp, BarChart3, ShoppingBag, Utensils
} from 'lucide-react';

// ── Types ──────────────────────────────────────────────────────────────────
interface Merchant {
  id: string;
  name: string;
  owner_name?: string;
  category: string;
  district: string;
  phone: string;
  rating: number;
  review_count: number;
  delivery_time_minutes: number;
  min_order: number;
  delivery_fee: number;
  is_open: boolean;
  verification_status: string;
  commission_rate: number;
  subscription_status?: string;
  monthly_subscription_usd?: number;
  total_orders?: number;
  total_revenue?: number;
  created_at?: string;
}

interface MerchantProduct {
  id: string;
  name: string;
  price: number;
  category: string;
  in_stock: boolean;
  image_emoji?: string;
}

interface FoodOrder {
  id: string;
  order_number: string;
  merchant_name: string;
  customer_name: string;
  items_summary: string;
  total_amount: number;
  commission_amount: number;
  status: string;
  created_at: string;
}

interface ShiriBusinessEatsProps {
  actorName: string;
  canManage: boolean;
}

// ── Category config ────────────────────────────────────────────────────────
const CATEGORIES = [
  { id: 'all',           label: 'Dhammaan',       emoji: '🏪' },
  { id: 'cunto',         label: 'Cunto / Maqaayad', emoji: '🍽️' },
  { id: 'fast_food',     label: 'Fast Food',       emoji: '🍔' },
  { id: 'supermarket',   label: 'Supermarket',     emoji: '🛒' },
  { id: 'daawo',         label: 'Farmashiye',      emoji: '💊' },
  { id: 'hilib_qudaar',  label: 'Hilib & Qudaar',  emoji: '🥩' },
  { id: 'electronics',   label: 'Electronics',     emoji: '📱' },
  { id: 'dharka',        label: 'Dharka',          emoji: '👗' },
];

// ── Status badge ───────────────────────────────────────────────────────────
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
  const map: Record<string, { label: string; cls: string }> = {
    verified:         { label: '✅ Xaqiijin',         cls: 'bg-green-100 text-green-800 border-green-300' },
    pending_approval: { label: '⏳ Sugaya Admin',      cls: 'bg-orange-100 text-orange-800 border-orange-300' },
    pending:          { label: '⏳ Sugaya',            cls: 'bg-yellow-100 text-yellow-800 border-yellow-300' },
    rejected:         { label: '❌ Diiday',            cls: 'bg-red-100 text-red-800 border-red-300' },
    active:           { label: '🟢 Firfircoon',        cls: 'bg-green-100 text-green-800 border-green-300' },
    inactive:         { label: '⚫ Inactive',           cls: 'bg-neutral-100 text-neutral-700 border-neutral-300' },
  };
  const s = map[status] ?? { label: status, cls: 'bg-neutral-100 text-neutral-700 border-neutral-300' };
  return <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold border ${s.cls}`}>{s.label}</span>;
};

// ── Mock Data ──────────────────────────────────────────────────────────────
const MOCK_MERCHANTS: Merchant[] = [
  { id: 'm1', name: 'Pizza House Mogadishu', owner_name: 'Axmed Faarax', category: 'fast_food', district: 'Hodan', phone: '0615551024', rating: 4.9, review_count: 142, delivery_time_minutes: 25, min_order: 3.00, delivery_fee: 2.00, is_open: true, verification_status: 'verified', commission_rate: 15.00, subscription_status: 'active', monthly_subscription_usd: 3.00, total_orders: 847, total_revenue: 4235.50, created_at: '2026-09-01T10:00:00Z' },
  { id: 'm2', name: 'Al-Najax Pharmacy', owner_name: 'Fatuma Cali', category: 'daawo', district: 'Waaberi', phone: '0615554911', rating: 4.9, review_count: 88, delivery_time_minutes: 15, min_order: 2.00, delivery_fee: 1.50, is_open: true, verification_status: 'verified', commission_rate: 12.00, subscription_status: 'active', monthly_subscription_usd: 3.00, total_orders: 523, total_revenue: 2614.00, created_at: '2026-09-15T10:00:00Z' },
  { id: 'm3', name: 'Bakaaro Fresh Supermarket', owner_name: 'Maxamed Nuur', category: 'supermarket', district: 'Bakaaro', phone: '0615553392', rating: 4.7, review_count: 210, delivery_time_minutes: 30, min_order: 5.00, delivery_fee: 2.00, is_open: true, verification_status: 'verified', commission_rate: 10.00, subscription_status: 'active', monthly_subscription_usd: 3.00, total_orders: 1203, total_revenue: 6015.00, created_at: '2026-08-20T10:00:00Z' },
  { id: 'm4', name: 'Hilibka Geela & Ariga', owner_name: 'Cali Maxamed', category: 'hilib_qudaar', district: 'Hodan', phone: '0615556012', rating: 4.8, review_count: 75, delivery_time_minutes: 20, min_order: 5.00, delivery_fee: 1.50, is_open: false, verification_status: 'verified', commission_rate: 12.00, subscription_status: 'active', monthly_subscription_usd: 3.00, total_orders: 445, total_revenue: 2225.00, created_at: '2026-10-01T10:00:00Z' },
  { id: 'm5', name: 'Shaah & Canjeero Waaberi', owner_name: 'Hawa Jama', category: 'cunto', district: 'Waaberi', phone: '0617991100', rating: 4.6, review_count: 55, delivery_time_minutes: 20, min_order: 2.00, delivery_fee: 1.00, is_open: true, verification_status: 'pending_approval', commission_rate: 15.00, subscription_status: 'pending', monthly_subscription_usd: 3.00, total_orders: 0, total_revenue: 0, created_at: '2026-11-09T10:00:00Z' },
  { id: 'm6', name: 'Muqdisho Fashion Store', owner_name: 'Nasra Ahmed', category: 'dharka', district: 'KM4', phone: '0618005522', rating: 0, review_count: 0, delivery_time_minutes: 45, min_order: 10.00, delivery_fee: 2.50, is_open: false, verification_status: 'pending_approval', commission_rate: 15.00, subscription_status: 'pending', monthly_subscription_usd: 3.00, total_orders: 0, total_revenue: 0, created_at: '2026-11-10T08:00:00Z' },
];

const MOCK_ORDERS: FoodOrder[] = [
  { id: 'o1', order_number: 'SHR-ORD-00140', merchant_name: 'Pizza House Mogadishu', customer_name: 'Fadumo Axmed', items_summary: '2x Margherita Pizza, 1x Fries', total_amount: 19.50, commission_amount: 2.93, status: 'delivered', created_at: new Date(Date.now() - 3600000).toISOString() },
  { id: 'o2', order_number: 'SHR-ORD-00141', merchant_name: 'Al-Najax Pharmacy', customer_name: 'Cali Maxamed', items_summary: 'Panadol x2, Vitamin C x1', total_amount: 8.00, commission_amount: 0.96, status: 'delivered', created_at: new Date(Date.now() - 7200000).toISOString() },
  { id: 'o3', order_number: 'SHR-ORD-00142', merchant_name: 'Bakaaro Fresh Supermarket', customer_name: 'Hawa Jama', items_summary: 'Tomatoes 1kg, Rice 2kg, Oil', total_amount: 12.50, commission_amount: 1.25, status: 'picked_up', created_at: new Date(Date.now() - 1800000).toISOString() },
  { id: 'o4', order_number: 'SHR-ORD-00143', merchant_name: 'Pizza House Mogadishu', customer_name: 'Aamina Xasan', items_summary: '1x Chicken Burger, 2x Drinks', total_amount: 9.00, commission_amount: 1.35, status: 'pending', created_at: new Date(Date.now() - 300000).toISOString() },
  { id: 'o5', order_number: 'SHR-ORD-00144', merchant_name: 'Shaah & Canjeero Waaberi', customer_name: 'Mahad Omar', items_summary: '3x Canjeero, 2x Shaah', total_amount: 5.50, commission_amount: 0.83, status: 'pending', created_at: new Date(Date.now() - 120000).toISOString() },
];

const MOCK_PRODUCTS: MerchantProduct[] = [
  { id: 'p1', name: 'Margherita Pizza (Large)', price: 8.50, category: 'Pizza', in_stock: true, image_emoji: '🍕' },
  { id: 'p2', name: 'Chicken Burger Special', price: 5.00, category: 'Burger', in_stock: true, image_emoji: '🍔' },
  { id: 'p3', name: 'Crispy French Fries', price: 2.50, category: 'Sides', in_stock: true, image_emoji: '🍟' },
  { id: 'p4', name: 'Somali Shawarma', price: 4.50, category: 'Fast Food', in_stock: false, image_emoji: '🌯' },
  { id: 'p5', name: 'Fresh Orange Juice', price: 2.00, category: 'Drinks', in_stock: true, image_emoji: '🥤' },
];

// ── Main Component ─────────────────────────────────────────────────────────
export const ShiriBusinessEats: React.FC<ShiriBusinessEatsProps> = ({ actorName, canManage }) => {
  const [subTab, setSubTab] = useState<'merchants' | 'orders' | 'pending'>('merchants');
  const [merchants, setMerchants] = useState<Merchant[]>([]);
  const [orders, setOrders] = useState<FoodOrder[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [expandedMerchant, setExpandedMerchant] = useState<string | null>(null);
  const [selectedMerchantProducts, setSelectedMerchantProducts] = useState<Merchant | null>(null);
  const [products, setProducts] = useState<MerchantProduct[]>(MOCK_PRODUCTS);
  const [newProdName, setNewProdName] = useState('');
  const [newProdPrice, setNewProdPrice] = useState('');
  const [newProdCategory, setNewProdCategory] = useState('Cunto');
  const [newProdEmoji, setNewProdEmoji] = useState('🍽️');
  const [actionMsg, setActionMsg] = useState<{ type: 'ok' | 'err'; text: string } | null>(null);
  const [editCommission, setEditCommission] = useState<{ id: string; value: string } | null>(null);

  useEffect(() => { fetchData(); }, [subTab]);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const { data: mData } = await supabase.from('merchants').select('*').order('created_at', { ascending: false });
      setMerchants(mData?.length ? mData : MOCK_MERCHANTS);

      if (subTab === 'orders') {
        const { data: oData } = await supabase.from('food_orders').select('*').order('created_at', { ascending: false }).limit(50);
        setOrders(oData?.length ? oData : MOCK_ORDERS);
      }
    } catch {
      setMerchants(MOCK_MERCHANTS);
      setOrders(MOCK_ORDERS);
    } finally {
      setIsLoading(false);
    }
  };

  const notify = (type: 'ok' | 'err', text: string) => {
    setActionMsg({ type, text });
    setTimeout(() => setActionMsg(null), 4000);
  };

  const approveMerchant = async (id: string, name: string) => {
    if (!canManage) return;
    try {
      await supabase.from('merchants').update({ verification_status: 'verified', is_open: true, subscription_status: 'active' }).eq('id', id);
      setMerchants(prev => prev.map(m => m.id === id ? { ...m, verification_status: 'verified', is_open: true, subscription_status: 'active' } : m));
      notify('ok', `✅ ${name} waa la oggolaadey — hadda app-ka ku muuqdaa!`);
    } catch { notify('err', 'Khalad ayaa dhacay. Dib u isku day.'); }
  };

  const rejectMerchant = async (id: string, name: string) => {
    if (!canManage) return;
    try {
      await supabase.from('merchants').update({ verification_status: 'rejected', is_open: false }).eq('id', id);
      setMerchants(prev => prev.map(m => m.id === id ? { ...m, verification_status: 'rejected', is_open: false } : m));
      notify('err', `❌ ${name} waa la diiday.`);
    } catch { notify('err', 'Khalad.'); }
  };

  const toggleOpen = async (id: string, current: boolean) => {
    if (!canManage) return;
    try {
      await supabase.from('merchants').update({ is_open: !current }).eq('id', id);
      setMerchants(prev => prev.map(m => m.id === id ? { ...m, is_open: !current } : m));
    } catch { notify('err', 'Khalad.'); }
  };

  const saveCommission = async (id: string) => {
    if (!editCommission || !canManage) return;
    const val = parseFloat(editCommission.value);
    if (isNaN(val) || val < 0 || val > 50) { notify('err', 'Commission 0–50% dhexdeeda ah'); return; }
    try {
      await supabase.from('merchants').update({ commission_rate: val }).eq('id', id);
      setMerchants(prev => prev.map(m => m.id === id ? { ...m, commission_rate: val } : m));
      setEditCommission(null);
      notify('ok', `Commission ${val}% waa la keydiey`);
    } catch { notify('err', 'Khalad.'); }
  };

  const addProduct = () => {
    const price = parseFloat(newProdPrice);
    if (!newProdName.trim() || isNaN(price) || price <= 0) { notify('err', 'Magac iyo qiimo sax ah geli'); return; }
    setProducts(prev => [...prev, { id: 'p-' + Date.now(), name: newProdName.trim(), price, category: newProdCategory, in_stock: true, image_emoji: newProdEmoji }]);
    setNewProdName(''); setNewProdPrice('');
    notify('ok', `'${newProdName}' waa lagu daray dukaanka`);
  };

  const toggleStock = (pid: string) => setProducts(prev => prev.map(p => p.id === pid ? { ...p, in_stock: !p.in_stock } : p));
  const removeProduct = (pid: string, pname: string) => { if (confirm(`Tirtir '${pname}'?`)) setProducts(prev => prev.filter(p => p.id !== pid)); };

  const pendingMerchants = merchants.filter(m => m.verification_status === 'pending_approval' || m.verification_status === 'pending');
  const filteredMerchants = merchants
    .filter(m => subTab === 'pending' ? (m.verification_status === 'pending_approval' || m.verification_status === 'pending') : true)
    .filter(m => selectedCategory === 'all' || m.category === selectedCategory)
    .filter(m => !searchQuery || m.name.toLowerCase().includes(searchQuery.toLowerCase()) || m.district.toLowerCase().includes(searchQuery.toLowerCase()) || (m.owner_name ?? '').toLowerCase().includes(searchQuery.toLowerCase()));

  const totalRevenue = merchants.reduce((s, m) => s + (m.total_revenue ?? 0), 0);
  const totalCommission = merchants.reduce((s, m) => s + (m.total_revenue ?? 0) * (m.commission_rate / 100), 0);
  const activeMerchants = merchants.filter(m => m.verification_status === 'verified' && m.is_open).length;
  const monthlySubRevenue = merchants.filter(m => m.subscription_status === 'active').length * 3;

  return (
    <div className="space-y-6">

      {/* ── Header ── */}
      <div className="bg-gradient-to-r from-emerald-700 via-emerald-600 to-teal-700 text-white p-6 rounded-2xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2.5 py-1 bg-white/20 text-white text-[10px] font-black rounded-lg uppercase tracking-wider">🏪 SHIRI Business</span>
            <span className="px-2.5 py-1 bg-white/20 text-white text-[10px] font-black rounded-lg uppercase tracking-wider">🍽️ Eats & Market</span>
          </div>
          <h2 className="text-2xl font-black text-white">📦 SHIRI Business & Eats — Maamulka Ganacsiyada</h2>
          <p className="text-xs text-emerald-100 mt-1 leading-relaxed">
            Dukaamada oggolow/dii, commission beddel, alaabta maamul, dalabyada raac, dakhliga eeg.
          </p>
        </div>
        <button onClick={fetchData} disabled={isLoading}
          className="px-4 py-2 bg-white/20 hover:bg-white/30 text-white text-xs font-bold rounded-xl border border-white/30 transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shrink-0">
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Dib u soo cusbooneysii
        </button>
      </div>

      {/* ── Stats Row ── */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Dukaamada Online', value: activeMerchants, icon: '🏪', color: 'bg-green-50 border-green-200 text-green-800' },
          { label: 'Oggolaanshiis Sugaya', value: pendingMerchants.length, icon: '🔔', color: pendingMerchants.length > 0 ? 'bg-orange-50 border-orange-300 text-orange-800' : 'bg-neutral-50 border-neutral-200 text-neutral-600' },
          { label: 'Subscription /Bishii', value: `$${monthlySubRevenue.toFixed(0)}`, icon: '💳', color: 'bg-blue-50 border-blue-200 text-blue-800' },
          { label: "Commission Guud", value: `$${totalCommission.toFixed(0)}`, icon: '💰', color: 'bg-purple-50 border-purple-200 text-purple-800' },
        ].map(s => (
          <div key={s.label} className={`${s.color} border rounded-xl p-4 flex items-center gap-3`}>
            <span className="text-2xl">{s.icon}</span>
            <div>
              <div className="text-xl font-black">{s.value}</div>
              <div className="text-[11px] font-semibold">{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* ── Pending Alert ── */}
      {pendingMerchants.length > 0 && (
        <div className="p-4 bg-orange-50 border-2 border-orange-400 rounded-2xl flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-orange-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <div className="text-sm font-black text-orange-900">🔔 {pendingMerchants.length} Ganacsi oo Oggolaanshiis Sugaya!</div>
            <div className="text-xs text-orange-700 mt-0.5">
              {pendingMerchants.map(m => m.name).join(', ')} — oggolow ama dii si ay u bilaabaan.
            </div>
          </div>
          <button onClick={() => setSubTab('pending')} className="text-xs font-bold text-orange-700 underline whitespace-nowrap cursor-pointer">
            Eeg →
          </button>
        </div>
      )}

      {actionMsg && (
        <div className={`p-3 rounded-xl text-sm font-bold border ${actionMsg.type === 'ok' ? 'bg-emerald-50 border-emerald-300 text-emerald-900' : 'bg-red-50 border-red-300 text-red-900'}`}>
          {actionMsg.text}
        </div>
      )}

      {/* ── Sub-Tabs ── */}
      <div className="flex flex-wrap gap-2 bg-neutral-100 p-1 rounded-xl w-fit">
        {[
          { id: 'merchants', label: '🏪 Dukaamada', count: 0 },
          { id: 'orders',    label: '🍽️ Dalabyada Live', count: orders.filter(o => o.status === 'pending').length },
          { id: 'pending',   label: '🔔 Oggolaanshiis', count: pendingMerchants.length },
        ].map(tab => (
          <button key={tab.id}
            onClick={() => setSubTab(tab.id as any)}
            className={`px-4 py-2 rounded-lg text-xs font-black transition cursor-pointer flex items-center gap-1.5 ${subTab === tab.id ? 'bg-emerald-600 text-white shadow' : 'text-neutral-600 hover:bg-neutral-200'}`}>
            {tab.label}
            {tab.count > 0 && (
              <span className={`px-1.5 py-0.5 rounded-full text-[10px] font-black ${subTab === tab.id ? 'bg-white text-emerald-700' : 'bg-red-600 text-white'}`}>
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* ── MERCHANTS TAB ── */}
      {(subTab === 'merchants' || subTab === 'pending') && (
        <div className="space-y-4">
          {/* Search + Category Filter */}
          <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row gap-3 items-center">
            <div className="relative w-full md:w-80">
              <Search className="w-4 h-4 absolute left-3 top-3 text-neutral-400" />
              <input type="text" placeholder="Raadi dukaan, mulkiile, degmo..."
                value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs font-bold text-neutral-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500" />
            </div>
            {subTab === 'merchants' && (
              <div className="flex items-center gap-2 overflow-x-auto pb-1 md:pb-0 w-full md:w-auto">
                {CATEGORIES.map(cat => (
                  <button key={cat.id} onClick={() => setSelectedCategory(cat.id)}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition whitespace-nowrap cursor-pointer flex items-center gap-1 ${selectedCategory === cat.id ? 'bg-emerald-600 text-white' : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'}`}>
                    <span>{cat.emoji}</span> {cat.label}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Merchant Cards */}
          {isLoading ? (
            <div className="text-center py-16 text-neutral-400"><RefreshCw className="w-6 h-6 animate-spin mx-auto mb-2" /><p className="text-sm">Dukaamada soo xidanaya...</p></div>
          ) : filteredMerchants.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-2xl border border-neutral-200"><Store className="w-10 h-10 mx-auto text-neutral-300 mb-2" /><p className="text-sm text-neutral-500">Dukaan la ma helin.</p></div>
          ) : (
            <div className="space-y-3">
              {filteredMerchants.map(merchant => {
                const isPending = merchant.verification_status === 'pending_approval' || merchant.verification_status === 'pending';
                const isExpanded = expandedMerchant === merchant.id;
                return (
                  <div key={merchant.id}
                    className={`bg-white rounded-2xl border shadow-xs transition ${isPending ? 'border-orange-300 ring-1 ring-orange-100' : 'border-neutral-200'}`}>

                    {/* Card Header */}
                    <div className="p-4 flex items-start justify-between gap-3 flex-wrap">
                      <div className="flex items-start gap-3 flex-1 min-w-0">
                        {/* Emoji Icon */}
                        <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-2xl shrink-0 ${merchant.is_open ? 'bg-emerald-50' : 'bg-neutral-100'}`}>
                          {CATEGORIES.find(c => c.id === merchant.category)?.emoji ?? '🏪'}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="text-sm font-black text-neutral-900 truncate">{merchant.name}</span>
                            {merchant.verification_status === 'verified' && <ShieldCheck className="w-4 h-4 text-blue-500 shrink-0" />}
                            <StatusBadge status={merchant.verification_status} />
                          </div>
                          <div className="text-xs text-neutral-500 mt-0.5 flex items-center gap-3 flex-wrap">
                            {merchant.owner_name && <span>👤 {merchant.owner_name}</span>}
                            <span className="flex items-center gap-0.5"><MapPin className="w-3 h-3" />{merchant.district}</span>
                            <span className="flex items-center gap-0.5"><Phone className="w-3 h-3" />{merchant.phone}</span>
                            {merchant.rating > 0 && <span className="flex items-center gap-0.5"><Star className="w-3 h-3 text-amber-500 fill-amber-500" />{merchant.rating}</span>}
                          </div>
                        </div>
                      </div>

                      {/* Right Side Controls */}
                      <div className="flex items-center gap-2 flex-wrap shrink-0">
                        {/* Open/Closed Toggle */}
                        {merchant.verification_status === 'verified' && (
                          <button onClick={() => toggleOpen(merchant.id, merchant.is_open)} disabled={!canManage}
                            className={`px-2.5 py-1 rounded-full text-[10px] font-black cursor-pointer transition ${merchant.is_open ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200' : 'bg-neutral-100 text-neutral-500 hover:bg-neutral-200'}`}>
                            {merchant.is_open ? '🟢 Furan' : '⚫ Xiran'}
                          </button>
                        )}

                        {/* Commission Editor */}
                        {merchant.verification_status === 'verified' && (
                          editCommission?.id === merchant.id ? (
                            <div className="flex items-center gap-1">
                              <input type="number" value={editCommission.value}
                                onChange={e => setEditCommission({ id: merchant.id, value: e.target.value })}
                                className="w-14 px-2 py-1 border border-emerald-400 rounded-lg text-xs font-bold focus:outline-none"
                                min="0" max="50" step="0.5" />
                              <span className="text-xs text-neutral-500">%</span>
                              <button onClick={() => saveCommission(merchant.id)} className="px-2 py-1 bg-emerald-600 text-white text-xs font-bold rounded-lg cursor-pointer">✓</button>
                              <button onClick={() => setEditCommission(null)} className="px-2 py-1 bg-neutral-200 text-neutral-700 text-xs font-bold rounded-lg cursor-pointer">✕</button>
                            </div>
                          ) : (
                            <button onClick={() => canManage && setEditCommission({ id: merchant.id, value: merchant.commission_rate.toString() })}
                              className="flex items-center gap-1 px-2.5 py-1 bg-purple-50 text-purple-700 text-[11px] font-bold rounded-full border border-purple-200 cursor-pointer hover:bg-purple-100 transition">
                              <DollarSign className="w-3 h-3" />
                              {merchant.commission_rate}% Commission
                              {canManage && <Edit3 className="w-2.5 h-2.5 ml-0.5" />}
                            </button>
                          )
                        )}

                        {/* Approve / Reject for Pending */}
                        {isPending && canManage && (
                          <>
                            <button onClick={() => approveMerchant(merchant.id, merchant.name)}
                              className="flex items-center gap-1 px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white text-xs font-bold rounded-lg transition cursor-pointer">
                              <CheckCircle2 className="w-3.5 h-3.5" /> Oggolow
                            </button>
                            <button onClick={() => rejectMerchant(merchant.id, merchant.name)}
                              className="flex items-center gap-1 px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-lg transition cursor-pointer">
                              <XCircle className="w-3.5 h-3.5" /> Dii
                            </button>
                          </>
                        )}

                        {/* Products button */}
                        {merchant.verification_status === 'verified' && (
                          <button onClick={() => { setSelectedMerchantProducts(merchant); setProducts(MOCK_PRODUCTS); }}
                            className="flex items-center gap-1 px-3 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold rounded-lg border border-emerald-200 transition cursor-pointer">
                            <Package className="w-3.5 h-3.5" /> Alaabta
                          </button>
                        )}

                        {/* Expand toggle */}
                        <button onClick={() => setExpandedMerchant(isExpanded ? null : merchant.id)}
                          className="p-1.5 hover:bg-neutral-100 rounded-lg text-neutral-400 cursor-pointer transition">
                          {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
                        </button>
                      </div>
                    </div>

                    {/* Expanded Details */}
                    {isExpanded && (
                      <div className="px-4 pb-4 border-t border-neutral-100 pt-3">
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                          {[
                            { label: 'Dalabyada', value: merchant.total_orders ?? 0, icon: '📦' },
                            { label: 'Dakhliga Guud', value: `$${(merchant.total_revenue ?? 0).toFixed(2)}`, icon: '💰' },
                            { label: 'Waqtiga Gaarsiinta', value: `${merchant.delivery_time_minutes} min`, icon: '⏱️' },
                            { label: 'Subscription', value: `$${merchant.monthly_subscription_usd ?? 3}/Bishii`, icon: '📋' },
                          ].map(stat => (
                            <div key={stat.label} className="bg-neutral-50 rounded-xl p-3 text-center">
                              <div className="text-lg">{stat.icon}</div>
                              <div className="text-sm font-black text-neutral-900 mt-0.5">{stat.value}</div>
                              <div className="text-[10px] text-neutral-500 font-medium">{stat.label}</div>
                            </div>
                          ))}
                        </div>
                        {isPending && (
                          <div className="mt-3 p-3 bg-orange-50 border border-orange-200 rounded-xl text-xs text-orange-800">
                            ℹ️ <strong>{merchant.name}</strong> waxay u diiwaangeliyeen platform-ka.
                            Oggolaanshihiisa ka dib waxaa laga dalbanayaa <strong>$3.00/Bishii</strong> subscription.
                            Waxay ka shaqayn karaan app-ka marka aad oggolaato.
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ── ORDERS TAB ── */}
      {subTab === 'orders' && (
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-2xl border border-neutral-200 flex items-center justify-between">
            <h3 className="text-sm font-black text-neutral-900 flex items-center gap-2">
              <ShoppingBag className="w-4 h-4 text-emerald-600" /> Dalabyada Cuntada & Alaabta (Live)
            </h3>
            <span className="text-xs text-neutral-500">{orders.length} dalab</span>
          </div>

          {orders.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-2xl border border-neutral-200">
              <ShoppingBag className="w-10 h-10 mx-auto text-neutral-300 mb-2" />
              <p className="text-sm text-neutral-500">Dalab ma jiro.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {orders.map(order => (
                <div key={order.id} className="bg-white rounded-2xl border border-neutral-200 p-4 shadow-xs flex items-start justify-between gap-3 flex-wrap">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1">
                      <span className="text-sm font-black text-neutral-900">{order.order_number}</span>
                      <StatusBadge status={order.status} />
                    </div>
                    <div className="text-xs text-neutral-600 space-y-0.5">
                      <div>🏪 <strong>{order.merchant_name}</strong></div>
                      <div>👤 {order.customer_name}</div>
                      <div className="text-neutral-400">📋 {order.items_summary}</div>
                    </div>
                  </div>
                  <div className="text-right shrink-0">
                    <div className="text-sm font-black text-neutral-900">${order.total_amount.toFixed(2)}</div>
                    <div className="text-[11px] text-purple-600 font-bold">+${order.commission_amount.toFixed(2)} Commission</div>
                    <div className="text-[10px] text-neutral-400 mt-1">
                      {new Date(order.created_at).toLocaleTimeString('so-SO', { hour: '2-digit', minute: '2-digit' })}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Revenue Summary */}
          <div className="bg-gradient-to-r from-emerald-900 to-teal-900 text-white p-5 rounded-2xl border border-emerald-800">
            <div className="text-xs font-black uppercase text-emerald-400 mb-3 flex items-center gap-1.5">
              <BarChart3 className="w-3.5 h-3.5" /> Dakhliga & Commission-ka Guud
            </div>
            <div className="grid grid-cols-3 gap-4 text-center">
              {[
                { label: "Wadarta Dalabyada", val: `$${orders.reduce((s, o) => s + o.total_amount, 0).toFixed(2)}` },
                { label: "Commission SHIRI", val: `$${orders.reduce((s, o) => s + o.commission_amount, 0).toFixed(2)}` },
                { label: "Dukaamada Active", val: activeMerchants.toString() },
              ].map(s => (
                <div key={s.label}>
                  <div className="text-lg font-black text-white">{s.val}</div>
                  <div className="text-[11px] text-emerald-300 mt-0.5">{s.label}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ── PRODUCTS MODAL ── */}
      {selectedMerchantProducts && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-neutral-100 pb-4">
              <div>
                <h3 className="text-base font-black text-neutral-900 flex items-center gap-2">
                  <Package className="w-5 h-5 text-emerald-600" />
                  Alaabta: {selectedMerchantProducts.name}
                </h3>
                <p className="text-xs text-neutral-500 mt-0.5">
                  {selectedMerchantProducts.district} • {selectedMerchantProducts.phone}
                </p>
              </div>
              <button onClick={() => setSelectedMerchantProducts(null)}
                className="p-1.5 hover:bg-neutral-100 rounded-lg text-neutral-400 cursor-pointer transition">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Add Product */}
            {canManage && (
              <div className="bg-emerald-50 p-4 rounded-xl border border-emerald-200 space-y-3">
                <h4 className="text-xs font-black text-emerald-800 uppercase tracking-wide">+ Kudar Alaab Cusub</h4>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                  <input placeholder="Emoji 🍕" value={newProdEmoji} onChange={e => setNewProdEmoji(e.target.value)}
                    className="px-3 py-2 bg-white border border-emerald-200 rounded-lg text-sm font-bold focus:ring-2 focus:ring-emerald-500 outline-none text-center" maxLength={2} />
                  <input placeholder="Magaca Alaabta" value={newProdName} onChange={e => setNewProdName(e.target.value)}
                    className="px-3 py-2 bg-white border border-emerald-200 rounded-lg text-xs font-medium focus:ring-2 focus:ring-emerald-500 outline-none sm:col-span-2" />
                  <input type="number" step="0.50" placeholder="Qiimaha ($)" value={newProdPrice} onChange={e => setNewProdPrice(e.target.value)}
                    className="px-3 py-2 bg-white border border-emerald-200 rounded-lg text-xs font-medium focus:ring-2 focus:ring-emerald-500 outline-none" />
                </div>
                <button onClick={addProduct}
                  className="w-full px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition cursor-pointer flex items-center justify-center gap-1.5">
                  <Plus className="w-3.5 h-3.5" /> Kudar Alaabta
                </button>
              </div>
            )}

            {/* Products List */}
            <div className="space-y-2">
              <h4 className="text-xs font-black text-neutral-800 uppercase tracking-wide">
                Alaabta Jirta ({products.length})
              </h4>
              <div className="divide-y divide-neutral-100 border border-neutral-200 rounded-xl overflow-hidden">
                {products.map(p => (
                  <div key={p.id} className="p-3.5 flex items-center justify-between hover:bg-neutral-50/60 transition bg-white">
                    <div className="flex items-center gap-2.5">
                      <span className="text-xl">{p.image_emoji ?? '📦'}</span>
                      <div>
                        <div className="font-bold text-xs text-neutral-900">{p.name}</div>
                        <div className="text-[11px] text-neutral-500">${p.price.toFixed(2)} • {p.category}</div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button onClick={() => toggleStock(p.id)}
                        className={`px-2 py-1 rounded-md text-[10px] font-bold cursor-pointer transition ${p.in_stock ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'}`}>
                        {p.in_stock ? '✓ Jira' : '✕ Dhammaatay'}
                      </button>
                      <button onClick={() => removeProduct(p.id, p.name)}
                        className="p-1 hover:bg-red-50 text-neutral-400 hover:text-red-600 rounded transition cursor-pointer">
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button onClick={() => setSelectedMerchantProducts(null)}
                className="px-4 py-2 bg-neutral-900 hover:bg-black text-white rounded-xl text-xs font-bold transition cursor-pointer">
                Done / Xir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

