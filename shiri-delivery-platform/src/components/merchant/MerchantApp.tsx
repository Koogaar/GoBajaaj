import React, { useState, useEffect } from 'react';
import { Merchant, Order, Product, OrderStatus, PaymentMethod } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { buildGoogleMapsViewUrl } from '../../utils/coordinateParser';
import {
  Store,
  ShoppingBag,
  Package,
  Plus,
  DollarSign,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  TrendingUp,
  Settings,
  Bell,
  ChevronRight,
  Power,
  MapPin,
  Check,
  Bike,
  Sparkles,
  ExternalLink,
  Navigation,
  Phone,
  MessageCircle,
  Edit3,
  Volume2,
  LogOut,
} from 'lucide-react';
import { VoiceNotePlayer } from '../common/VoiceNotePlayer';
import { soundEffects } from '../../utils/soundEffects';

export const MerchantApp: React.FC = () => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  // Active Merchant initialized from platform auth
  const [selectedMerchantId, setSelectedMerchantId] = useState<string>(
    platformStore.getActiveMerchantId() || 'merch-1'
  );
  const [activeTab, setActiveTab] = useState<'dashboard' | 'orders' | 'products' | 'finance' | 'settings'>('dashboard');

  // Filter tabs for orders
  const [orderFilter, setOrderFilter] = useState<'all' | 'pending' | 'preparing' | 'ready' | 'completed'>('all');

  // Add Product Modal state
  const [isAddProductOpen, setIsAddProductOpen] = useState<boolean>(false);
  const [newProdName, setNewProdName] = useState<string>('');
  const [newProdNameSo, setNewProdNameSo] = useState<string>('');
  const [newProdPrice, setNewProdPrice] = useState<string>('7.50');
  const [newProdCategory, setNewProdCategory] = useState<string>('Main Dish');
  const [newProdDesc, setNewProdDesc] = useState<string>('Fresh hot delivery in Mogadishu');

  // Store subscriptions
  const [merchants, setMerchants] = useState<Merchant[]>(platformStore.getMerchants());
  const [orders, setOrders] = useState<Order[]>(platformStore.getOrders());

  // Store contact & manual payment edit modal state
  const [isEditStoreOpen, setIsEditStoreOpen] = useState<boolean>(false);
  const [editStorePaymentPhone, setEditStorePaymentPhone] = useState<string>('');
  const [editStoreWhatsapp, setEditStoreWhatsapp] = useState<string>('');
  const [editStoreCallPhone, setEditStoreCallPhone] = useState<string>('');

  // Business Subscription Payment Modal state (paying $30 to admin)
  const [isPaySubscriptionOpen, setIsPaySubscriptionOpen] = useState<boolean>(false);
  const [subPayerPhone, setSubPayerPhone] = useState<string>('0615112233');
  const [subPaymentMethod, setSubPaymentMethod] = useState<PaymentMethod>('evc_plus');
  const [subNotes, setSubNotes] = useState<string>('');

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      setMerchants([...platformStore.getMerchants()]);
      setOrders([...platformStore.getOrders()]);
    });
    return unsub;
  }, []);

  const merchant = merchants.find((m) => m.id === selectedMerchantId) || merchants[0];
  const merchantOrders = orders.filter((o) => o.merchantId === merchant.id);

  const pendingOrders = merchantOrders.filter((o) => o.status === 'pending');
  const pendingPaymentOrders = merchantOrders.filter((o) => o.paymentStatus !== 'paid' && o.status !== 'cancelled');
  const preparingOrders = merchantOrders.filter((o) => o.status === 'accepted' || o.status === 'preparing');
  const readyOrders = merchantOrders.filter((o) => o.status === 'ready_for_pickup' || o.status === 'courier_assigned');
  const completedOrders = merchantOrders.filter((o) => o.status === 'delivered');
  const merchantEarnings = completedOrders.reduce((sum, o) => sum + o.total, 0) || merchant.salesToday;

  const filteredOrders = merchantOrders.filter((o) => {
    if ((orderFilter as string) === 'unpaid') return o.paymentStatus !== 'paid' && o.status !== 'cancelled';
    if (orderFilter === 'pending') return o.status === 'pending';
    if (orderFilter === 'preparing') return o.status === 'accepted' || o.status === 'preparing';
    if (orderFilter === 'ready') return o.status === 'ready_for_pickup' || o.status === 'courier_assigned';
    if (orderFilter === 'completed') return o.status === 'delivered';
    return true;
  });

  const handleToggleStoreStatus = () => {
    platformStore.toggleMerchantOpen(merchant.id);
  };

  const handleCreateProduct = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProdName.trim()) return;

    platformStore.addProduct(merchant.id, {
      name: newProdName,
      nameSomali: newProdNameSo || newProdName,
      description: newProdDesc,
      descriptionSomali: newProdDesc,
      price: parseFloat(newProdPrice) || 5.0,
      imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=80',
      category: newProdCategory,
      categorySomali: newProdCategory,
      inStock: true,
      stockQuantity: 50,
      prepTimeMinutes: 15,
    });

    setNewProdName('');
    setNewProdNameSo('');
    setIsAddProductOpen(false);
  };

  const handleUpdateStatus = (orderId: string, status: OrderStatus, note?: string) => {
    platformStore.updateOrderStatus(orderId, status, merchant.name, note);
  };

  const handleOpenEditStore = () => {
    setEditStorePaymentPhone(merchant.paymentPhone || merchant.phone);
    setEditStoreWhatsapp(merchant.whatsappNumber || merchant.phone.replace(/\D/g, ''));
    setEditStoreCallPhone(merchant.callPhone || merchant.phone);
    setIsEditStoreOpen(true);
  };

  const handleSaveStore = (e: React.FormEvent) => {
    e.preventDefault();
    platformStore.updateMerchantProfile(merchant.id, {
      paymentPhone: editStorePaymentPhone,
      whatsappNumber: editStoreWhatsapp,
      callPhone: editStoreCallPhone,
    });
    setIsEditStoreOpen(false);
  };

  const handleSubmitSubscription = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subPayerPhone.trim()) {
      alert('Fadlan geli numberka aad lacagta ka dirtay');
      return;
    }
    const res = platformStore.submitSubscriptionPayment('merchant', merchant.id, {
      payerPhone: subPayerPhone.trim(),
      paymentMethod: subPaymentMethod,
      notes: subNotes.trim() || undefined,
    });
    alert(res.message);
    setIsPaySubscriptionOpen(false);
  };

  return (
    <div className="flex flex-col h-full bg-slate-50 relative">
      {/* Top Mobile App Bar */}
      <header className="sticky top-0 z-30 bg-white px-4 py-3 border-b border-slate-100 flex items-center justify-between shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-lg font-bold">
            {merchant.logo}
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <h2 className="text-sm font-bold text-slate-900 leading-tight">
                {merchant.name}
              </h2>
              <span className="w-2 h-2 rounded-full bg-emerald-500" title="Online" />
            </div>
            <p className="text-[10px] font-mono text-slate-500">
              Shiri Code: {merchant.addressCode} · {merchant.district}
            </p>
          </div>
        </div>

        {/* Store Active Badge & Logout Action */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => {
              platformStore.logoutRole('merchant');
              window.location.reload();
            }}
            className="p-1.5 px-2.5 rounded-xl bg-slate-100 hover:bg-rose-50 text-slate-600 hover:text-rose-600 transition-colors flex items-center gap-1 text-xs font-bold"
            title="Ka bax akoonka meheradda"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Ka Bax</span>
          </button>
        </div>
      </header>

      {/* Main Tab View */}
      <div className="flex-1 overflow-y-auto p-4 pb-24 space-y-4">
        {/* Urgent New Order Alert Banner */}
        {pendingOrders.length > 0 && (
          <div className="bg-red-600 text-white p-3.5 rounded-2xl shadow-lg shadow-red-600/30 flex items-center justify-between animate-pulse">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-white/20 flex items-center justify-center">
                <Bell className="w-4 h-4 text-white" />
              </div>
              <div>
                <h4 className="text-xs font-bold leading-tight">
                  {t.newOrderAlert} ({pendingOrders.length})
                </h4>
                <p className="text-[10px] text-red-100 mt-0.5">
                  Fadlan aqbal si aysan u daahin diyaarintu
                </p>
              </div>
            </div>
            <button
              onClick={() => {
                setOrderFilter('pending');
                setActiveTab('orders');
              }}
              className="px-3 py-1 bg-white text-red-600 rounded-xl text-xs font-bold shadow-xs hover:bg-red-50"
            >
              Arag Hadda
            </button>
          </div>
        )}

        {/* Urgent Customer Payment Approval Alert Banner */}
        {pendingPaymentOrders.length > 0 && (
          <div className="bg-amber-500 text-slate-950 p-3.5 rounded-2xl shadow-lg shadow-amber-500/25 flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-black/10 flex items-center justify-center text-base">
                💳
              </div>
              <div>
                <h4 className="text-xs font-black leading-tight">
                  Lacag-Bixin Sugaysa Oggolaansho ({pendingPaymentOrders.length})
                </h4>
                <p className="text-[10px] text-amber-950 font-medium mt-0.5">
                  Macmiilku wuxuu bixiyay lacagta, fadlan xaqiiji oo approve garee
                </p>
              </div>
            </div>
            <button
              onClick={() => {
                setOrderFilter('unpaid' as any);
                setActiveTab('orders');
              }}
              className="px-3 py-1 bg-slate-950 text-amber-300 rounded-xl text-xs font-bold shadow-xs hover:bg-slate-900"
            >
              Oggolow
            </button>
          </div>
        )}

        {/* TAB 1: DASHBOARD */}
        {activeTab === 'dashboard' && (
          <div className="space-y-4">
            {/* Store Status Toggle Card */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-2xl flex items-center justify-center ${
                    merchant.isOpen ? 'bg-emerald-100 text-emerald-600' : 'bg-slate-100 text-slate-400'
                  }`}
                >
                  <Power className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-slate-900">{t.storeStatus}</h4>
                  <p className="text-[11px] text-slate-500">
                    {merchant.isOpen ? t.storeOpen : t.storeClosed}
                  </p>
                </div>
              </div>

              <button
                onClick={handleToggleStoreStatus}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors ${
                  merchant.isOpen
                    ? 'bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100'
                    : 'bg-red-50 text-red-700 border border-red-200 hover:bg-red-100'
                }`}
              >
                {merchant.isOpen ? 'Furan (Open)' : 'Xiran (Closed)'}
              </button>
            </div>

            {/* KPI Metric Cards */}
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs">
                <div className="flex items-center justify-between text-slate-500 mb-1">
                  <span className="text-[11px] font-semibold">{t.todaysSales}</span>
                  <DollarSign className="w-4 h-4 text-emerald-600" />
                </div>
                <div className="text-xl font-black font-mono text-slate-900">
                  ${merchant.salesToday.toFixed(2)}
                </div>
                <div className="text-[10px] text-emerald-600 font-semibold mt-1 flex items-center gap-1">
                  <TrendingUp className="w-3 h-3" />
                  <span>+{merchant.ordersTodayCount} dalab maanta</span>
                </div>
              </div>

              <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs">
                <div className="flex items-center justify-between text-slate-500 mb-1">
                  <span className="text-[11px] font-semibold">Dalabyada Sugaya</span>
                  <Clock className="w-4 h-4 text-amber-500" />
                </div>
                <div className="text-xl font-black font-mono text-amber-600">
                  {pendingOrders.length + preparingOrders.length}
                </div>
                <div className="text-[10px] text-slate-500 mt-1">
                  {pendingOrders.length} cusub · {preparingOrders.length} jikada
                </div>
              </div>
            </div>

            {/* Quick Navigation Action Grid */}
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              <button
                onClick={() => {
                  setOrderFilter('all');
                  setActiveTab('orders');
                }}
                className="bg-white p-3 rounded-2xl border border-slate-200 hover:border-red-600 shadow-2xs flex flex-col items-center gap-1"
              >
                <ShoppingBag className="w-5 h-5 text-red-600" />
                <span className="font-bold text-slate-800">Dalabyada</span>
              </button>

              <button
                onClick={() => setActiveTab('products')}
                className="bg-white p-3 rounded-2xl border border-slate-200 hover:border-red-600 shadow-2xs flex flex-col items-center gap-1"
              >
                <Package className="w-5 h-5 text-blue-600" />
                <span className="font-bold text-slate-800">Alaabta</span>
              </button>

              <button
                onClick={() => setActiveTab('finance')}
                className="bg-white p-3 rounded-2xl border border-slate-200 hover:border-red-600 shadow-2xs flex flex-col items-center gap-1"
              >
                <TrendingUp className="w-5 h-5 text-emerald-600" />
                <span className="font-bold text-slate-800">Xisaabta</span>
              </button>
            </div>

            {/* Recent Orders Feed */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  Dalabyadii Ugu Dambeeyay
                </h4>
                <button
                  onClick={() => setActiveTab('orders')}
                  className="text-[11px] font-semibold text-red-600"
                >
                  Dhammaan
                </button>
              </div>

              {merchantOrders.slice(0, 3).map((ord) => (
                <div
                  key={ord.id}
                  className="p-3 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs"
                >
                  <div>
                    <span className="font-bold text-slate-900 font-mono">{ord.orderNumber}</span>
                    <p className="text-[11px] text-slate-500 mt-0.5">
                      {ord.items.map((i) => `${i.quantity}x ${i.name}`).join(', ')}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className="font-bold font-mono text-slate-900">${ord.total.toFixed(2)}</span>
                    <div className="text-[10px] font-semibold text-red-600 capitalize">{ord.status}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 2: ORDERS PROCESSOR (CRITICAL REQUIREMENT 8.3) */}
        {activeTab === 'orders' && (
          <div className="space-y-3">
            {/* Filter Pills */}
            <div className="flex gap-1.5 overflow-x-auto pb-1 no-scrollbar text-xs">
              {[
                { id: 'all', label: 'Dhammaan', count: merchantOrders.length },
                { id: 'unpaid', label: 'Sugaya Lacagta', count: pendingPaymentOrders.length },
                { id: 'pending', label: 'Cusub (Pending)', count: pendingOrders.length },
                { id: 'preparing', label: 'Diyaarinta', count: preparingOrders.length },
                { id: 'ready', label: 'Diyaar (Ready)', count: readyOrders.length },
                { id: 'completed', label: 'La Keenay', count: completedOrders.length },
              ].map((f) => {
                const isSelected = orderFilter === f.id;
                return (
                  <button
                    key={f.id}
                    onClick={() => setOrderFilter(f.id as any)}
                    className={`px-3 py-1.5 rounded-xl font-bold whitespace-nowrap transition-all flex items-center gap-1.5 ${
                      isSelected
                        ? f.id === 'unpaid'
                          ? 'bg-amber-500 text-slate-950 shadow-xs'
                          : 'bg-red-600 text-white shadow-xs'
                        : 'bg-white text-slate-600 border border-slate-200 hover:border-slate-300'
                    }`}
                  >
                    <span>{f.label}</span>
                    <span
                      className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                        isSelected
                          ? 'bg-black/20 text-white'
                          : f.id === 'unpaid' && f.count > 0
                          ? 'bg-amber-100 text-amber-900 font-bold'
                          : 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {f.count}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Orders Cards List */}
            {filteredOrders.length === 0 ? (
              <div className="text-center py-12 text-slate-400 text-xs bg-white rounded-3xl border border-slate-200">
                Ma jiraan dalabyo qaybtaan ah.
              </div>
            ) : (
              filteredOrders.map((ord) => (
                <div
                  key={ord.id}
                  className="bg-white rounded-3xl p-4 border border-slate-200/80 shadow-2xs space-y-3"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono font-bold text-sm text-slate-900">
                          {ord.orderNumber}
                        </span>
                        <span className="bg-slate-100 text-slate-600 text-[10px] font-semibold px-2 py-0.5 rounded-full">
                          {ord.paymentMethod.toUpperCase()}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 mt-1">
                        <p className="text-xs text-slate-500">
                          Macmiil: <span className="font-semibold text-slate-800">{ord.customerName}</span>
                        </p>
                        <a
                          href={`tel:${ord.customerCallPhone || ord.customerPhone}`}
                          className="px-2 py-0.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-semibold rounded-lg flex items-center gap-1 transition-colors"
                          title="Wac Macmiilka"
                        >
                          <Phone className="w-3 h-3 text-slate-500" />
                          <span>Wac</span>
                        </a>
                        <a
                          href={`https://wa.me/${(ord.customerWhatsapp || ord.customerPhone).replace(/\D/g, '')}?text=${encodeURIComponent(
                            `Asc ${ord.customerName}, waxaan nahay ${merchant.name}. Dalabkaaga (${ord.orderNumber}) waxaan ka wadnaa diyaarintiisa!`
                          )}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="px-2 py-0.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-[10px] font-bold rounded-lg flex items-center gap-1 transition-colors"
                          title="WhatsApp Macmiilka"
                        >
                          <MessageCircle className="w-3 h-3 text-emerald-600" />
                          <span>WhatsApp</span>
                        </a>
                      </div>
                    </div>

                    <div className="text-right">
                      <span className="text-sm font-black font-mono text-slate-900">
                        ${ord.total.toFixed(2)}
                      </span>
                      <div className="text-[10px] text-slate-500">
                        {new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </div>
                  </div>

                  {/* Items List */}
                  <div className="p-3 rounded-2xl bg-slate-50 border border-slate-100 space-y-1.5 text-xs">
                    {ord.items.map((item) => (
                      <div key={item.id} className="flex justify-between text-slate-700">
                        <span>
                          <b className="text-slate-900">{item.quantity}x</b> {item.name} {item.selectedOptionName && `(${item.selectedOptionName})`}
                        </span>
                        <span className="font-mono font-semibold">${item.totalPrice.toFixed(2)}</span>
                      </div>
                    ))}
                    {ord.deliveryInstructions && (
                      <div className="pt-1.5 border-t border-slate-200/60 text-[11px] text-amber-700 italic">
                        Tilmaamo: "{ord.deliveryInstructions}"
                      </div>
                    )}
                  </div>

                  {/* Customer Delivery Address & Confirmed Coordinates */}
                  <div className="p-3 rounded-2xl bg-red-50/40 border border-red-100 space-y-2 text-xs">
                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-2">
                      <div className="min-w-0">
                        <div className="flex items-center gap-1.5">
                          <MapPin className="w-3.5 h-3.5 text-red-600 shrink-0" />
                          <span className="font-bold text-slate-900 truncate">
                            {ord.deliveryAddress.addressCode} · {ord.deliveryAddress.district}
                          </span>
                        </div>
                        <div className="text-[11px] text-slate-600 mt-0.5 break-words">
                          {ord.deliveryAddress.street || 'Waddo'}, {ord.deliveryAddress.buildingOrFloor} · {ord.deliveryAddress.landmark}
                        </div>
                      </div>

                      <a
                        href={buildGoogleMapsViewUrl(
                          ord.deliveryAddress.coordinates,
                          `${ord.deliveryAddress.addressCode} (${ord.customerName})`
                        )}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-2.5 py-1.5 bg-white hover:bg-slate-50 text-slate-800 border border-slate-200 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 shrink-0 shadow-2xs transition-colors self-start sm:self-auto"
                        title="Open customer delivery coordinates in Google Maps"
                      >
                        <ExternalLink className="w-3 h-3 text-emerald-600 shrink-0" />
                        <span>Google Maps</span>
                      </a>
                    </div>

                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 text-[10px] pt-1 border-t border-red-100/80 text-slate-500 font-mono">
                      <span>
                        Coords: {ord.deliveryAddress.coordinates.lat.toFixed(5)}, {ord.deliveryAddress.coordinates.lng.toFixed(5)}
                      </span>
                      <span className="text-red-600 font-medium">Albaabka guriga</span>
                    </div>
                  </div>

                  {/* Customer Voice Landmark Note Player if attached */}
                  {ord.voiceNote && (
                    <VoiceNotePlayer voiceNote={ord.voiceNote} />
                  )}

                  {/* Courier info if assigned */}
                  {ord.courierName && (
                    <div className="flex items-center justify-between text-xs bg-emerald-50 text-emerald-900 p-2.5 rounded-xl border border-emerald-100">
                      <div className="flex items-center gap-2">
                        <Bike className="w-4 h-4 text-emerald-600" />
                        <span>Darawalka: <b>{ord.courierName}</b> ({ord.courierVehicle})</span>
                      </div>
                      <span className="text-[10px] font-bold bg-white px-2 py-0.5 rounded-full border border-emerald-200">
                        Assigned
                      </span>
                    </div>
                  )}

                  {/* Customer Payment Verification & Approval Box */}
                  <div
                    className={`p-3.5 rounded-2xl border text-xs space-y-2.5 transition-all ${
                      ord.paymentStatus === 'paid'
                        ? 'bg-emerald-50/70 border-emerald-200 text-slate-800'
                        : 'bg-amber-50/90 border-amber-300 ring-1 ring-amber-400/50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-1.5 font-bold">
                        <span className="text-base">{ord.paymentStatus === 'paid' ? '✅' : '💳'}</span>
                        <span className="text-slate-900">
                          {ord.paymentStatus === 'paid'
                            ? 'Lacagta Waa La Oggolaaday (Payment Verified)'
                            : 'Lacag Bixin Sugaysa Oggolaanshahaaga'}
                        </span>
                      </div>
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          ord.paymentStatus === 'paid'
                            ? 'bg-emerald-200 text-emerald-900'
                            : 'bg-amber-200 text-amber-900 animate-pulse'
                        }`}
                      >
                        {ord.paymentStatus === 'paid' ? 'APPROVED' : 'SUGAYA OGGOLAANSHO'}
                      </span>
                    </div>

                    <div className="grid grid-cols-2 gap-2 p-2.5 bg-white rounded-xl border border-slate-200/80">
                      <div>
                        <span className="text-[10px] text-slate-500 block">Numberka laga soo diray:</span>
                        <span className="font-mono font-black text-red-600 text-xs">
                          {ord.payerPhone || ord.customerPhone}
                        </span>
                      </div>
                      <div>
                        <span className="text-[10px] text-slate-500 block">Wadarta & Habka:</span>
                        <span className="font-mono font-bold text-slate-900 text-xs">
                          ${ord.total.toFixed(2)} · {ord.paymentMethod.toUpperCase()}
                        </span>
                      </div>
                      {ord.paymentReference && (
                        <div className="col-span-2 pt-1 border-t border-slate-100 text-[10px] text-slate-600 flex justify-between items-center">
                          <span>Tixraaca: <b className="font-mono text-slate-800">{ord.paymentReference}</b></span>
                          {ord.manualPaymentProofNote && (
                            <span className="italic truncate max-w-[200px]" title={ord.manualPaymentProofNote}>
                              "{ord.manualPaymentProofNote}"
                            </span>
                          )}
                        </div>
                      )}
                    </div>

                    {ord.paymentStatus !== 'paid' ? (
                      <div className="space-y-1.5 pt-1">
                        <p className="text-[10px] text-amber-800 font-medium">
                          * Fadlan hubi account-kaaga ({merchant.paymentPhone || merchant.phone}) ka dibna riix badhanka si aad u oggolaato.
                        </p>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              platformStore.approveCustomerPayment(ord.id);
                            }}
                            className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            <span>Oggolow Lacagta (${ord.total.toFixed(2)})</span>
                          </button>
                          <a
                            href={`https://wa.me/${(ord.customerWhatsapp || ord.customerPhone).replace(/\D/g, '')}?text=${encodeURIComponent(
                              `Asc ${ord.customerName}, waxaan nahay ${merchant.name}. Fadlan noo soo dir shaashadda (screenshot) lacagta aad ka soo dirtay ${ord.payerPhone || ord.customerPhone} ee dalabka ${ord.orderNumber}. Mahadsanid!`
                            )}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-2.5 py-2 bg-emerald-100 hover:bg-emerald-200 text-emerald-800 rounded-xl text-[11px] font-bold flex items-center gap-1 shrink-0"
                            title="Weydii Risiidhka WhatsApp"
                          >
                            <MessageCircle className="w-3.5 h-3.5 text-emerald-700" />
                            <span>Risiidh</span>
                          </a>
                        </div>
                      </div>
                    ) : (
                      <div className="text-[10px] text-emerald-700 font-medium flex items-center gap-1 pt-0.5">
                        <CheckCircle2 className="w-3 h-3 text-emerald-600 shrink-0" />
                        <span>Lacagta macmiilka waa la hubiyay oo la ansixiyay.</span>
                      </div>
                    )}
                  </div>

                  {/* Action Buttons based on status */}
                  <div className="pt-2 border-t border-slate-100 flex gap-2">
                    {ord.status === 'pending' && (
                      <>
                        <button
                          onClick={() => handleUpdateStatus(ord.id, 'accepted', 'Merchant accepted order')}
                          className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>{t.acceptOrder}</span>
                        </button>
                        <button
                          onClick={() => handleUpdateStatus(ord.id, 'cancelled', 'Rejected by merchant due to stock')}
                          className="px-3 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition-colors"
                        >
                          {t.rejectOrder}
                        </button>
                      </>
                    )}

                    {ord.status === 'accepted' && (
                      <button
                        onClick={() => handleUpdateStatus(ord.id, 'preparing', 'Food being cooked')}
                        className="w-full py-2.5 bg-amber-500 hover:bg-amber-600 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                      >
                        <Clock className="w-3.5 h-3.5" />
                        <span>{t.markPreparing}</span>
                      </button>
                    )}

                    {ord.status === 'preparing' && (
                      <button
                        onClick={() => handleUpdateStatus(ord.id, 'ready_for_pickup', 'Food packaged and waiting')}
                        className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                      >
                        <Package className="w-3.5 h-3.5" />
                        <span>{t.markReady}</span>
                      </button>
                    )}

                    {ord.status === 'ready_for_pickup' && (
                      <div className="w-full py-2.5 bg-slate-100 text-slate-700 font-semibold text-xs rounded-xl text-center">
                        Darawal ayaa kusoo wajahan soo qaadashada...
                      </div>
                    )}

                    {ord.status === 'courier_assigned' && (
                      <button
                        onClick={() => platformStore.courierConfirmPickup(ord.id)}
                        className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors"
                      >
                        {t.confirmHandover} ({ord.courierName})
                      </button>
                    )}

                    {ord.status === 'in_transit' && (
                      <div className="w-full py-2 bg-blue-50 text-blue-700 text-xs font-semibold rounded-xl text-center">
                        Waddada ayuu kusoo jiraa macmiilka
                      </div>
                    )}

                    {ord.status === 'delivered' && (
                      <div className="w-full py-2 bg-emerald-50 text-emerald-700 text-xs font-bold rounded-xl text-center flex items-center justify-center gap-1.5">
                        <Check className="w-3.5 h-3.5" />
                        <span>Si guul leh ayaa loo gaarsiiyay</span>
                      </div>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* TAB 3: PRODUCTS & INVENTORY MANAGEMENT */}
        {activeTab === 'products' && (
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900">{t.productsTitle}</h3>
                <p className="text-[11px] text-slate-500">
                  Isbeddel kasta wuxuu isla markiiba ka muuqanayaa Customer App
                </p>
              </div>
              <button
                onClick={() => setIsAddProductOpen(true)}
                className="px-3 py-2 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl flex items-center gap-1 shadow-xs"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>{t.addProduct}</span>
              </button>
            </div>

            <div className="space-y-2.5">
              {platformStore.getProducts().filter((p) => p.merchantId === merchant.id).map((prod) => (
                <div
                  key={prod.id}
                  className="bg-white p-3 rounded-2xl border border-slate-200/80 shadow-2xs flex gap-3 items-center justify-between"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={prod.imageUrl}
                      alt={prod.name}
                      className="w-12 h-12 rounded-xl object-cover shrink-0"
                    />
                    <div className="min-w-0">
                      <h5 className="text-xs font-bold text-slate-900 truncate">{prod.name}</h5>
                      <span className="text-[11px] font-mono font-bold text-red-600">${prod.price.toFixed(2)}</span>
                      <div className="text-[10px] text-slate-400 truncate">{prod.category}</div>
                    </div>
                  </div>

                  {/* In Stock toggle */}
                  <button
                    onClick={() => platformStore.toggleProductStock(prod.id)}
                    className={`px-2.5 py-1 rounded-xl text-[11px] font-bold border transition-colors shrink-0 ${
                      prod.inStock
                        ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                        : 'bg-slate-100 text-slate-400 border-slate-200'
                    }`}
                  >
                    {prod.inStock ? t.inStock : t.outOfStock}
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 4: FINANCE & SETTLEMENTS */}
        {activeTab === 'finance' && (
          <div className="space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Xisaabaadka & Dakhliga (Finance)</h3>

            {/* Monthly Subscription Status Card */}
            <div className="bg-gradient-to-r from-emerald-600 to-teal-700 text-white p-4 rounded-3xl shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase tracking-wider bg-white/20 px-2 py-0.5 rounded-full">
                  Qorshaha Bisha (Subscription)
                </span>
                <span className="text-xs font-mono font-bold text-emerald-200">
                  0% Commission Plan
                </span>
              </div>
              <div className="flex items-baseline justify-between">
                <div>
                  <h4 className="text-base font-bold">Shiri Business Partner</h4>
                  <p className="text-[11px] text-emerald-100">Bixinta Bisha: $30.00 / bishii</p>
                </div>
                <span className="px-2.5 py-1 bg-white text-emerald-800 text-xs font-bold rounded-xl shadow-xs">
                  Xubin Firfircoon (Active)
                </span>
              </div>
              <p className="text-[10px] text-emerald-100/90 pt-1 border-t border-white/10">
                ✅ Dalab kasta oo cuntadaada ah 100% adaa iska leh. Ma jiro boqolkiiba lagaa goynayo!
              </p>
            </div>

            {/* Today's Sales Earnings */}
            <div className="bg-gradient-to-br from-slate-900 to-slate-800 text-white p-5 rounded-3xl shadow-md space-y-4">
              <div>
                <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
                  Dakhligaaga Saafiga ah ee Maanta (Net Sales)
                </span>
                <div className="text-3xl font-black font-mono text-emerald-400 mt-0.5">
                  ${merchantEarnings.toFixed(2)}
                </div>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  100% Dakhliga iibka waa kaaga (0% Commission Model)
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3 pt-3 border-t border-slate-700 text-xs">
                <div>
                  <span className="text-slate-400">Wadarta Iibka (Gross):</span>
                  <div className="font-mono font-bold">${merchantEarnings.toFixed(2)}</div>
                </div>
                <div>
                  <span className="text-slate-400">Commission-ka Shiri:</span>
                  <div className="font-mono font-bold text-emerald-400">
                    $0.00 (0% - Free)
                  </div>
                </div>
              </div>
            </div>

            {/* Manual Payment Instructions for Business Subscription */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200 shadow-2xs space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  Habka Bixinta Bisha (Manual Payment)
                </h4>
                <span className="text-[10px] bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-bold">
                  Bisha Cusub: $30
                </span>
              </div>
              <p className="text-[11px] text-slate-600 leading-relaxed">
                U wareeji khidmadda bisha lambarrada rasmiga ah ee Shiri Delivery, kadibna u dir shaashadda (screenshot) WhatsApp maamulka si degdeg loogu furo dukaankaaga:
              </p>

              <div className="p-3 bg-slate-50 rounded-2xl space-y-2 text-xs font-mono">
                <div className="flex justify-between items-center text-emerald-800 font-bold">
                  <span>Hormuud EVC Plus:</span>
                  <span className="bg-emerald-100 px-2 py-0.5 rounded text-[11px]">*712*615000000*30#</span>
                </div>
                <div className="flex justify-between items-center text-amber-800 font-bold">
                  <span>Somtel eDahab:</span>
                  <span className="bg-amber-100 px-2 py-0.5 rounded text-[11px]">*789*650000000*30#</span>
                </div>
              </div>

              {merchant.subscription?.manualPaymentStatus === 'pending_verification' && (
                <div className="p-3 bg-amber-50 rounded-2xl border border-amber-200 text-xs space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-amber-900">
                    <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
                    <span>Lacagtaadu waxay sugaysaa oggolaanshaha Maamulka (Pending Admin Approval)</span>
                  </div>
                  <p className="text-[11px] text-amber-800">
                    Waxaad $30 ka soo dirtay: <b className="font-mono text-slate-900">{merchant.subscription.payerPhone}</b>. Admin-ka ayaa hadda eegaya si uu u ansixiyo.
                  </p>
                </div>
              )}

              {/* Action Button: Submit payment details to admin */}
              <button
                onClick={() => {
                  setSubPayerPhone(merchant.paymentPhone || merchant.phone);
                  setIsPaySubscriptionOpen(true);
                }}
                className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
              >
                <DollarSign className="w-3.5 h-3.5" />
                <span>Geli Numberka aad ka dirtay lacagta bisha ($30)</span>
              </button>

              {/* Action Buttons to Confirm via WhatsApp or Call */}
              <div className="grid grid-cols-2 gap-2 pt-1">
                <a
                  href={`https://wa.me/252615000000?text=${encodeURIComponent(
                    `Asc Shiri Delivery, waxaan ahay meheradda ${merchant.name} (${merchant.phone}). Waxaan bixiyay khidmadda bisha ($30) ee barnaamijka. Nambarka aan ka soo diray waa ${merchant.subscription?.payerPhone || merchant.paymentPhone}. Fadlan i xaqiiji!`
                  )}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-xs transition-colors"
                >
                  <MessageCircle className="w-3.5 h-3.5" />
                  <span>U dir Risiidhka WhatsApp</span>
                </a>
                <a
                  href="tel:+252615000000"
                  className="py-2.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-xs transition-colors"
                >
                  <Phone className="w-3.5 h-3.5" />
                  <span>Wac Maamulka</span>
                </a>
              </div>
            </div>

            {/* Store Contact & Payout Account Card */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                    Xisaabta & Xiriirka Meheradda
                  </h4>
                  <p className="text-[10px] text-slate-500">
                    Nambarada lacag-qaadashada EVC Plus & WhatsApp tooska ah
                  </p>
                </div>
                <button
                  onClick={handleOpenEditStore}
                  className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[11px] font-bold flex items-center gap-1 transition-colors"
                >
                  <Edit3 className="w-3 h-3 text-slate-600" />
                  <span>Wax ka beddel</span>
                </button>
              </div>

              <div className="p-3 rounded-2xl bg-slate-50 text-xs space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-500">Merchant Till / Account:</span>
                  <span className="font-mono font-bold text-emerald-600">{merchant.paymentPhone || merchant.phone}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">WhatsApp Meheradda:</span>
                  {merchant.whatsappNumber ? (
                    <a
                      href={`https://wa.me/${merchant.whatsappNumber.replace(/\D/g, '')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-bold text-emerald-700 flex items-center gap-1 hover:underline"
                    >
                      <MessageCircle className="w-3 h-3 text-emerald-600" />
                      <span>{merchant.whatsappNumber}</span>
                    </a>
                  ) : (
                    <span className="text-slate-400 italic">Lama gelin</span>
                  )}
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Taleefanka Caadiga ah (Call):</span>
                  <a
                    href={`tel:${merchant.callPhone || merchant.phone}`}
                    className="font-bold text-slate-900 flex items-center gap-1 hover:text-red-600"
                  >
                    <Phone className="w-3 h-3 text-slate-400" />
                    <span>{merchant.callPhone || merchant.phone}</span>
                  </a>
                </div>
                <div className="flex justify-between pt-1 border-t border-slate-200">
                  <span className="text-slate-500">Bixinta Xigta:</span>
                  <span className="font-semibold text-emerald-600 font-mono">1-da Bisha Soo Socota ($30)</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Merchant Bottom Tab Bar */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200 h-16 grid grid-cols-4 items-center max-w-[400px] mx-auto">
        {[
          { id: 'dashboard', label: 'Dashboard', icon: Store },
          { id: 'orders', label: 'Orders', icon: ShoppingBag, badge: pendingOrders.length > 0 ? `${pendingOrders.length}` : undefined },
          { id: 'products', label: 'Products', icon: Package },
          { id: 'finance', label: 'Finance', icon: DollarSign },
        ].map((tab) => {
          const isActive = activeTab === tab.id;
          const IconComp = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`flex flex-col items-center justify-center h-full relative transition-colors ${
                isActive ? 'text-red-600' : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <div className="relative">
                <IconComp className="w-5 h-5" />
                {tab.badge && (
                  <span className="absolute -top-1 -right-2 w-4 h-4 rounded-full bg-red-600 text-white text-[9px] font-bold flex items-center justify-center">
                    {tab.badge}
                  </span>
                )}
              </div>
              <span className="text-[10px] font-semibold mt-1">{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Add Product Modal */}
      {isAddProductOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-md p-5 shadow-2xl border border-slate-100 flex flex-col space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-sm font-bold text-slate-900">{t.addProduct}</h3>
              <button onClick={() => setIsAddProductOpen(false)} className="text-slate-400 font-bold">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  {t.productName} *
                </label>
                <input
                  type="text"
                  required
                  value={newProdName}
                  onChange={(e) => setNewProdName(e.target.value)}
                  placeholder="e.g. Chicken Shawarma Roll"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  {t.productNameSomali}
                </label>
                <input
                  type="text"
                  value={newProdNameSo}
                  onChange={(e) => setNewProdNameSo(e.target.value)}
                  placeholder="e.g. Shawarma Digaag oo Kulul"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    {t.productPrice} *
                  </label>
                  <input
                    type="number"
                    step="0.5"
                    required
                    value={newProdPrice}
                    onChange={(e) => setNewProdPrice(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none font-mono focus:border-red-600"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Category
                  </label>
                  <input
                    type="text"
                    value={newProdCategory}
                    onChange={(e) => setNewProdCategory(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Description
                </label>
                <textarea
                  rows={2}
                  value={newProdDesc}
                  onChange={(e) => setNewProdDesc(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none resize-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all"
              >
                Keydi Alaabta (Save Product)
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Edit Store Contact & Till Modal */}
      {isEditStoreOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-sm p-5 shadow-2xl border border-slate-100 flex flex-col space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-sm font-bold text-slate-900">
                Beddel Xiriirka & Till Number-ka Meheradda
              </h3>
              <button
                onClick={() => setIsEditStoreOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveStore} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka Lacag-Qaadashada (Merchant Till / EVC) *
                </label>
                <input
                  type="text"
                  required
                  value={editStorePaymentPhone}
                  onChange={(e) => setEditStorePaymentPhone(e.target.value)}
                  placeholder="061XXXXXXX ama Till Number"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
                <span className="text-[10px] text-slate-400 mt-0.5 block">
                  Lambarka ama Till-ka ay macaamiishu lacagta kugu soo wareejinayaan
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka WhatsApp ee Dalabyada (Orders WhatsApp) *
                </label>
                <input
                  type="text"
                  required
                  value={editStoreWhatsapp}
                  onChange={(e) => setEditStoreWhatsapp(e.target.value)}
                  placeholder="25261XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
                <span className="text-[10px] text-slate-400 mt-0.5 block">
                  Si macaamiisha iyo darawalladu si toos ah ugu soo diraan farriimaha WhatsApp
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Taleefanka Tooska ah ee Wacitaanka (Direct Call) *
                </label>
                <input
                  type="text"
                  required
                  value={editStoreCallPhone}
                  onChange={(e) => setEditStoreCallPhone(e.target.value)}
                  placeholder="+252 61 XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all"
              >
                Keydi Xiriirka Meheradda (Save)
              </button>
            </form>
          </div>
        </div>
      )}
      {/* Pay Subscription Modal ($30 to Admin) */}
      {isPaySubscriptionOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-sm p-5 shadow-2xl border border-slate-100 flex flex-col space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Bixi Khidmadda Bisha ($30.00)
                </h3>
                <p className="text-[10px] text-slate-500">
                  U wareeji maamulka Shiri si dukaankaagu u ahaado mid firfircoon
                </p>
              </div>
              <button
                onClick={() => setIsPaySubscriptionOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            {/* Admin Payment Accounts */}
            <div className="p-3 bg-amber-50 rounded-2xl border border-amber-200 text-xs space-y-1.5 font-mono">
              <span className="font-sans font-bold text-amber-900 text-[11px] block">
                Lambarrada Maamulka Shiri ee lacagta lagu shubayo:
              </span>
              <div className="flex justify-between items-center text-emerald-800 font-bold">
                <span>EVC Plus:</span>
                <span className="bg-emerald-100 px-1.5 py-0.5 rounded text-[11px]">*712*615000000*30#</span>
              </div>
              <div className="flex justify-between items-center text-amber-800 font-bold">
                <span>eDahab:</span>
                <span className="bg-amber-100 px-1.5 py-0.5 rounded text-[11px]">*789*650000000*30#</span>
              </div>
            </div>

            <form onSubmit={handleSubmitSubscription} className="space-y-3 text-xs">
              <div>
                <label className="block font-black text-slate-900 mb-1">
                  Geli numberka aad lacagta ka dirtay *
                </label>
                <input
                  type="tel"
                  required
                  value={subPayerPhone}
                  onChange={(e) => setSubPayerPhone(e.target.value)}
                  placeholder="e.g. 0615112233"
                  className="w-full px-3 py-2 rounded-xl border-2 border-red-500 bg-white font-mono font-bold text-slate-900 outline-none focus:ring-2 focus:ring-red-200 shadow-2xs"
                />
                <span className="text-[10px] text-slate-500 mt-0.5 block">
                  Maamulku lambarkan ayuu ku xaqiijin doonaa lacag-bixintaada
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Habka aad u dirtay *
                </label>
                <select
                  value={subPaymentMethod}
                  onChange={(e) => setSubPaymentMethod(e.target.value as PaymentMethod)}
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-medium outline-none"
                >
                  <option value="evc_plus">Hormuud EVC Plus (*712*)</option>
                  <option value="edahab">Somtel eDahab (*789*)</option>
                  <option value="zaad">Telesom ZAAD (*880*)</option>
                  <option value="sahal">Golis Sahal (*550*)</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Xusuusin / Tixraac (Reference ID ama Notes)
                </label>
                <input
                  type="text"
                  value={subNotes}
                  onChange={(e) => setSubNotes(e.target.value)}
                  placeholder="e.g. EVC-94821 ama Magacaaga"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all flex items-center justify-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Gudbi Lacagta Bisha ($30)</span>
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
