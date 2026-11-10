import React, { useState, useEffect } from 'react';
import { Merchant, Courier, Order, ShiriAddress, ServiceZone } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { TelecomGatewayService } from '../../services/telecomProviders';
import { MapCanvas } from '../common/MapCanvas';
import { buildGoogleMapsViewUrl } from '../../utils/coordinateParser';
import {
  Users,
  Store,
  Bike,
  ShoppingBag,
  DollarSign,
  TrendingUp,
  MapPin,
  ShieldCheck,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Search,
  Filter,
  RefreshCw,
  Sliders,
  Settings,
  Layers,
  FileText,
  CreditCard,
  ExternalLink,
  Phone,
  MessageCircle,
  Plus,
  PlusCircle,
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  // Admin section: 'overview' | 'map' | 'orders' | 'subscriptions' | 'merchants' | 'couriers' | 'addresses' | 'telecom' | 'audit'
  const [activeSection, setActiveSection] = useState<
    'overview' | 'map' | 'orders' | 'subscriptions' | 'merchants' | 'couriers' | 'addresses' | 'telecom' | 'audit'
  >('overview');

  const [merchants, setMerchants] = useState<Merchant[]>(platformStore.getMerchants());
  const [couriers, setCouriers] = useState<Courier[]>(platformStore.getCouriers());
  const [orders, setOrders] = useState<Order[]>(platformStore.getOrders());
  const [addresses, setAddresses] = useState<ShiriAddress[]>(platformStore.getAddresses());
  const [auditLogs, setAuditLogs] = useState(platformStore.getAuditLogs());
  const [serviceZones, setServiceZones] = useState<ServiceZone[]>(platformStore.getServiceZones());

  // Search & Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [pricingModel, setPricingModel] = useState<'subscription' | 'commission'>('subscription');

  // Registration modal states
  const [isAddCourierOpen, setIsAddCourierOpen] = useState(false);
  const [cName, setCName] = useState('');
  const [cPhone, setCPhone] = useState('');
  const [cWhatsapp, setCWhatsapp] = useState('');
  const [cPaymentPhone, setCPaymentPhone] = useState('');
  const [cVehicleType, setCVehicleType] = useState<'Motorcycle' | 'Bajaj' | 'Car'>('Bajaj');
  const [cPlate, setCPlate] = useState('');

  const [isAddMerchantOpen, setIsAddMerchantOpen] = useState(false);
  const [mName, setMName] = useState('');
  const [mDistrict, setMDistrict] = useState('Hodan');
  const [mAddressCode, setMAddressCode] = useState('SHR-BO-9000');
  const [mPhone, setMPhone] = useState('');
  const [mWhatsapp, setMWhatsapp] = useState('');
  const [mPaymentPhone, setMPaymentPhone] = useState('');
  const [mOwner, setMOwner] = useState('');

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      setMerchants([...platformStore.getMerchants()]);
      setCouriers([...platformStore.getCouriers()]);
      setOrders([...platformStore.getOrders()]);
      setAddresses([...platformStore.getAddresses()]);
      setAuditLogs([...platformStore.getAuditLogs()]);
    });
    return unsub;
  }, []);

  // Financial calculations
  const totalGMV = orders.reduce((sum, o) => sum + o.total, 0) + 34100;
  const platformCommissionRevenue = orders.reduce((sum, o) => sum + (o.subtotal * 0.15) + o.serviceFee, 0) + 5115;
  const activeCouriersCount = couriers.filter((c) => c.isOnline).length + 840;
  const activeMerchantsCount = merchants.length + 180;

  // Monthly Subscription Model (0% Commission - Somalia SaaS Model)
  const merchantMonthlyMRR = activeMerchantsCount * 30; // $30/month per merchant
  const courierMonthlyMRR = activeCouriersCount * 10;   // $10/month per driver
  const totalMonthlyMRR = merchantMonthlyMRR + courierMonthlyMRR;

  // Pending subscription payments from drivers and merchants
  const pendingDriverSubs = couriers.filter(
    (c) => c.subscription?.manualPaymentStatus === 'pending_verification'
  );
  const pendingMerchantSubs = merchants.filter(
    (m) => m.subscription?.manualPaymentStatus === 'pending_verification'
  );
  const totalPendingSubs = pendingDriverSubs.length + pendingMerchantSubs.length;

  const filteredOrders = orders.filter((o) => {
    const matchSearch =
      o.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      o.merchantName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      o.customerName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchStatus = statusFilter === 'all' || o.status === statusFilter;
    return matchSearch && matchStatus;
  });

  const handleRegisterCourier = (e: React.FormEvent) => {
    e.preventDefault();
    if (!cName.trim() || !cPhone.trim()) return;
    platformStore.registerCourier({
      name: cName,
      phone: cPhone,
      whatsappNumber: cWhatsapp || cPhone.replace(/\D/g, ''),
      paymentPhone: cPaymentPhone || cPhone,
      vehicleType: cVehicleType,
      plateNumber: cPlate || 'MG-0000',
    });
    setIsAddCourierOpen(false);
    setCName('');
    setCPhone('');
    setCWhatsapp('');
    setCPaymentPhone('');
    setCPlate('');
  };

  const handleRegisterMerchant = (e: React.FormEvent) => {
    e.preventDefault();
    if (!mName.trim() || !mPhone.trim()) return;
    platformStore.registerMerchant({
      name: mName,
      district: mDistrict,
      addressCode: mAddressCode,
      phone: mPhone,
      whatsappNumber: mWhatsapp || mPhone.replace(/\D/g, ''),
      paymentPhone: mPaymentPhone || mPhone,
      ownerName: mOwner || 'Owner',
    });
    setIsAddMerchantOpen(false);
    setMName('');
    setMPhone('');
    setMWhatsapp('');
    setMPaymentPhone('');
    setMOwner('');
  };

  const handleApproveSubscription = (type: 'merchant' | 'courier', id: string) => {
    platformStore.verifySubscriptionPayment(type, id);
    alert('Khidmadda bisha si guul leh ayaa loo xaqiijiyay! (Subscription marked Active & Verified)');
  };

  const handleRejectSubscription = (type: 'merchant' | 'courier', id: string) => {
    const reason = prompt('Sababta diidmada (Reason for rejection):', 'Lacagta lama helin account-ka maamulka');
    if (reason) {
      platformStore.rejectSubscriptionPayment(type, id, reason);
      alert('Lacag-bixinta waa la diiday.');
    }
  };

  const handleSimulateDispatchOrder = () => {
    platformStore.simulateIncomingOrder();
    alert('Dalab tijaabo ah ayaa loo diray nidaamka! Darawalladu waxay hadda ka arkayaan "Dalabyada Soo Dhacaya".');
  };

  return (
    <div className="min-h-screen bg-slate-100 flex flex-col font-sans">
      {/* Top Admin Navigation Bar */}
      <header className="bg-slate-900 text-white border-b border-slate-800 px-6 py-3 flex items-center justify-between sticky top-0 z-40">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-red-600 flex items-center justify-center font-black text-sm">
            S
          </div>
          <div>
            <h1 className="text-base font-bold tracking-tight text-white leading-tight">
              SHIRI DELIVERY <span className="text-red-500 font-mono text-xs">· Operations Console</span>
            </h1>
            <p className="text-[10px] text-slate-400">
              Mogadishu Central Logistics & Fleet Dispatch Command
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs">
          <div className="hidden md:flex items-center gap-2 bg-slate-800 px-3 py-1.5 rounded-lg text-slate-300">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            <span>Mogadishu Gateway: Online</span>
          </div>
          <button
            onClick={() => window.location.reload()}
            className="flex items-center gap-1.5 bg-slate-800 hover:bg-slate-700 px-3 py-1.5 rounded-lg text-slate-200 transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5 text-red-400" />
            <span>Cusbooneysii</span>
          </button>
        </div>
      </header>

      {/* Mobile Horizontal Navigation Tab Bar (visible on screens < 768px) */}
      <div className="flex md:hidden bg-slate-900 border-b border-slate-800 px-3 py-2 overflow-x-auto gap-1.5 no-scrollbar shrink-0">
        {[
          { id: 'overview', label: 'Overview', icon: TrendingUp },
          { id: 'subscriptions', label: 'Approvals', icon: DollarSign, badge: totalPendingSubs },
          { id: 'map', label: 'Live Map', icon: MapPin },
          { id: 'orders', label: 'Orders', icon: ShoppingBag, badge: orders.length },
          { id: 'merchants', label: 'Merchants', icon: Store, badge: merchants.length },
          { id: 'couriers', label: 'Couriers', icon: Bike, badge: couriers.length },
          { id: 'addresses', label: 'Addresses', icon: Layers, badge: addresses.length },
          { id: 'telecom', label: 'Telecom', icon: CreditCard },
          { id: 'audit', label: 'Audit', icon: FileText },
        ].map((item) => {
          const isActive = activeSection === item.id;
          const IconComp = item.icon;
          return (
            <button
              key={item.id}
              onClick={() => setActiveSection(item.id as any)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
                isActive
                  ? 'bg-red-600 text-white shadow-xs'
                  : 'bg-slate-800 text-slate-400 hover:text-white'
              }`}
            >
              <IconComp className="w-3.5 h-3.5" />
              <span>{item.label}</span>
              {item.badge !== undefined && item.badge > 0 && (
                <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono ${
                  isActive ? 'bg-red-950 text-red-200' : 'bg-slate-700 text-white'
                }`}>
                  {item.badge}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Main Admin Layout: Sidebar + Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar */}
        <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col justify-between shrink-0 border-r border-slate-800 hidden md:flex">
          <div className="p-4 space-y-1">
            <div className="text-[10px] uppercase font-bold text-slate-500 px-3 py-1.5 tracking-wider">
              Management
            </div>

            {[
              { id: 'overview', label: 'Dashboard Overview', icon: TrendingUp },
              { id: 'subscriptions', label: 'Subscription Approvals', icon: DollarSign, badge: totalPendingSubs },
              { id: 'map', label: 'Live Operations Map', icon: MapPin },
              { id: 'orders', label: 'Order Dispatch Center', icon: ShoppingBag, badge: orders.length },
              { id: 'merchants', label: 'Merchants & Stores', icon: Store, badge: merchants.length },
              { id: 'couriers', label: 'Courier Fleet', icon: Bike, badge: couriers.length },
              { id: 'addresses', label: 'Shiri Address Registry', icon: Layers, badge: addresses.length },
              { id: 'telecom', label: 'Telecom & Payments', icon: CreditCard },
              { id: 'audit', label: 'System Audit Logs', icon: FileText },
            ].map((item) => {
              const isActive = activeSection === item.id;
              const IconComp = item.icon;
              return (
                <button
                  key={item.id}
                  onClick={() => setActiveSection(item.id as any)}
                  className={`w-full flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-colors ${
                    isActive
                      ? 'bg-red-600 text-white shadow-xs'
                      : 'hover:bg-slate-800/80 text-slate-400 hover:text-white'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    <IconComp className="w-4 h-4" />
                    <span>{item.label}</span>
                  </div>
                  {item.badge !== undefined && (
                    <span
                      className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${
                        isActive ? 'bg-red-800 text-white' : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>

          <div className="p-4 border-t border-slate-800 text-xs text-slate-500 space-y-1">
            <div className="font-semibold text-slate-400">Shiri Core v2.4.0</div>
            <div>PostgreSQL & RLS Enforced</div>
            <div>Telecom Adapters: EVC+ / eDahab</div>
          </div>
        </aside>

        {/* Content Area */}
        <main className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* SECTION 1: OVERVIEW & KPIS */}
          {activeSection === 'overview' && (
            <div className="space-y-6">
              {/* Urgent Pending Subscription Alert Banner */}
              {totalPendingSubs > 0 && (
                <div className="bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 text-slate-950 p-4 rounded-3xl shadow-lg shadow-amber-500/20 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-black/15 flex items-center justify-center text-xl shrink-0">
                      💰
                    </div>
                    <div>
                      <h4 className="text-sm font-black leading-tight">
                        Oggolaanshaha Lacagaha Bisha ({totalPendingSubs} sugaya)
                      </h4>
                      <p className="text-xs text-amber-950 font-medium mt-0.5">
                        {pendingMerchantSubs.length} ganacsato ($30/mo) iyo {pendingDriverSubs.length} darawal ($15/mo) ayaa soo diray lacagta bisha, waxayna sugayaan oggolaanshahaaga.
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setActiveSection('subscriptions')}
                    className="px-4 py-2 bg-slate-950 hover:bg-slate-900 text-amber-300 rounded-xl text-xs font-black shadow-sm transition-all shrink-0 self-start sm:self-auto"
                  >
                    Oggolow Hadda →
                  </button>
                </div>
              )}

              {/* Monetization Engine Switcher (SaaS Subscriptions vs Commission) */}
              <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="w-9 h-9 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center text-lg">
                    💎
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-slate-900">
                      Qaabka Dakhliga ee Shiri (Revenue Engine)
                    </h4>
                    <p className="text-[11px] text-slate-500">
                      {pricingModel === 'subscription'
                        ? 'Bixinta Bisha (SaaS Subscription): 0% Commission (Maqaayadaha iyo Darawallada waxba lagama gooyo)'
                        : 'Per-Order Commission Active: 15% laga jarayo dalab kasta oo cunto ah'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-2xl self-start md:self-auto">
                  <button
                    onClick={() => setPricingModel('subscription')}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                      pricingModel === 'subscription'
                        ? 'bg-slate-900 text-emerald-300 shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Bixinta Bisha ($30 / $10 - 0% Comm)
                  </button>
                  <button
                    onClick={() => setPricingModel('commission')}
                    className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all ${
                      pricingModel === 'commission'
                        ? 'bg-slate-900 text-white shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    Commission (15%)
                  </button>
                </div>
              </div>

              {/* Top Operational Metrics matching design reference */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    {t.kpiTotalCustomers}
                  </span>
                  <div className="text-2xl font-black font-mono text-slate-900 mt-1">12,482</div>
                  <div className="text-[11px] text-emerald-600 font-semibold mt-1">
                    +142 registered today
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    {t.kpiActiveDrivers}
                  </span>
                  <div className="text-2xl font-black font-mono text-slate-900 mt-1">
                    {activeCouriersCount}
                  </div>
                  <div className="text-[11px] text-emerald-600 font-semibold mt-1">
                    Mogadishu on-duty fleet
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    {t.kpiGrossVolume}
                  </span>
                  <div className="text-2xl font-black font-mono text-slate-900 mt-1">
                    ${totalGMV.toLocaleString()}
                  </div>
                  <div className="text-[11px] text-emerald-600 font-semibold mt-1">
                    Gross merchandise value
                  </div>
                </div>

                <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    {pricingModel === 'subscription' ? 'Monthly Subscriptions (MRR)' : t.kpiPlatformRevenue}
                  </span>
                  <div className="text-2xl font-black font-mono text-red-600 mt-1">
                    ${pricingModel === 'subscription' ? `${totalMonthlyMRR.toLocaleString()}/mo` : platformCommissionRevenue.toLocaleString()}
                  </div>
                  <div className="text-[11px] text-slate-500 font-semibold mt-1">
                    {pricingModel === 'subscription'
                      ? `${activeMerchantsCount} Dukaamo ($30) + ${activeCouriersCount} Mooto ($10)`
                      : '15% Commission + service fees'}
                  </div>
                </div>
              </div>

              {/* Mogadishu Live Operations Preview */}
              <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                      {t.liveOperationsMap}
                    </h3>
                    <p className="text-xs text-slate-500">
                      Real-time positions of dispatched couriers, restaurants, and dropoff destinations in Mogadishu
                    </p>
                  </div>
                  <button
                    onClick={() => setActiveSection('map')}
                    className="px-3 py-1.5 bg-red-600 text-white rounded-xl text-xs font-bold hover:bg-red-700"
                  >
                    Balaari Khariidadda (Full Map)
                  </button>
                </div>

                <div className="rounded-2xl overflow-hidden border border-slate-200">
                  <MapCanvas
                    mode="route"
                    height="h-72"
                    pickupCoords={merchants[0]?.coordinates}
                    dropoffCoords={orders[0]?.deliveryAddress?.coordinates}
                    courierCoords={couriers[0]?.currentCoordinates}
                    courierLabel={couriers[0]?.name}
                    pickupLabel={merchants[0]?.name}
                    dropoffLabel={orders[0]?.deliveryAddress?.addressCode}
                  />
                </div>
              </div>

              {/* Service Zones Configuration strip */}
              <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                  Mogadishu Delivery Zones & Pricing
                </h3>
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {serviceZones.map((z) => (
                    <div
                      key={z.id}
                      className="p-3.5 rounded-2xl bg-slate-50 border border-slate-200 text-xs flex justify-between items-center"
                    >
                      <div>
                        <div className="font-bold text-slate-900">{z.name}</div>
                        <div className="text-[11px] text-slate-500">Degmada: {z.district}</div>
                      </div>
                      <div className="text-right font-mono">
                        <div className="font-bold text-slate-900">${z.baseDeliveryFee.toFixed(2)} base</div>
                        <div className="text-[10px] text-slate-500">+${z.perKmFee.toFixed(2)}/km</div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* SECTION 2: LIVE MAP */}
          {activeSection === 'map' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Mogadishu Real-Time Fleet & Dispatch Map
                  </h3>
                  <p className="text-xs text-slate-500">
                    Hodan, Waberi, Hamarweyne, Wadajir, Dayniile & Yaqshid operational zones
                  </p>
                </div>
                <div className="flex gap-2 text-xs">
                  <span className="flex items-center gap-1 bg-emerald-50 text-emerald-700 px-2 py-1 rounded-lg">
                    🛵 {couriers.filter(c => c.isOnline).length} Couriers Online
                  </span>
                  <span className="flex items-center gap-1 bg-red-50 text-red-700 px-2 py-1 rounded-lg">
                    📍 {orders.filter(o => o.status === 'in_transit').length} In Transit
                  </span>
                </div>
              </div>

              <div className="rounded-2xl overflow-hidden border border-slate-200">
                <MapCanvas
                  mode="fleet"
                  height="h-[520px]"
                  pickupCoords={merchants[0]?.coordinates}
                  dropoffCoords={orders[0]?.deliveryAddress?.coordinates}
                  courierCoords={couriers[0]?.currentCoordinates}
                  courierLabel={couriers[0]?.name}
                  pickupLabel={merchants[0]?.name}
                  dropoffLabel={orders[0]?.deliveryAddress?.addressCode}
                />
              </div>
            </div>
          )}

          {/* SECTION 3: ORDERS MANAGEMENT */}
          {activeSection === 'orders' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Order Dispatch & Audit Management
                  </h3>
                  <p className="text-xs text-slate-500">
                    Live orders, dispatch state transitions, customer pins, and telecom settlements
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search order #, customer, store..."
                    className="px-3 py-1.5 text-xs rounded-xl border border-slate-300 outline-none w-56"
                  />
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="px-3 py-1.5 text-xs rounded-xl border border-slate-300 outline-none"
                  >
                    <option value="all">All Statuses</option>
                    <option value="pending">Pending</option>
                    <option value="preparing">Preparing</option>
                    <option value="ready_for_pickup">Ready for Pickup</option>
                    <option value="in_transit">In Transit</option>
                    <option value="delivered">Delivered</option>
                  </select>
                  <button
                    onClick={() => window.location.reload()}
                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition-colors shrink-0"
                    title="Cusbooneysii dalabyada"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>Cusbooneysii</span>
                  </button>
                </div>
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b border-slate-200 bg-slate-50 text-slate-600 font-bold">
                      <th className="p-3">Order #</th>
                      <th className="p-3">Customer</th>
                      <th className="p-3">Merchant</th>
                      <th className="p-3">Shiri Address</th>
                      <th className="p-3">Courier</th>
                      <th className="p-3">PIN</th>
                      <th className="p-3">Payment</th>
                      <th className="p-3">Total</th>
                      <th className="p-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {filteredOrders.map((ord) => (
                      <tr key={ord.id} className="hover:bg-slate-50">
                        <td className="p-3 font-mono font-bold text-slate-900">{ord.orderNumber}</td>
                        <td className="p-3">
                          <div className="font-semibold text-slate-800">{ord.customerName}</div>
                          <div className="text-[10px] text-slate-400 font-mono">{ord.customerPhone}</div>
                        </td>
                        <td className="p-3 font-medium text-slate-800">{ord.merchantName}</td>
                        <td className="p-3">
                          <div className="flex items-center gap-1.5">
                            <span className="font-mono text-red-600 font-bold">{ord.deliveryAddress.addressCode}</span>
                            <a
                              href={buildGoogleMapsViewUrl(ord.deliveryAddress.coordinates, ord.deliveryAddress.addressCode)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-slate-400 hover:text-emerald-600 transition-colors"
                              title="Open in Google Maps"
                            >
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          </div>
                          <div className="text-[10px] text-slate-500">{ord.deliveryAddress.district}</div>
                          <div className="text-[9px] text-slate-400 font-mono">
                            {ord.deliveryAddress.coordinates.lat.toFixed(5)}, {ord.deliveryAddress.coordinates.lng.toFixed(5)}
                          </div>
                        </td>
                        <td className="p-3">
                          {ord.courierName ? (
                            <span className="text-emerald-700 font-semibold">{ord.courierName}</span>
                          ) : (
                            <span className="text-amber-600 font-semibold">Unassigned</span>
                          )}
                        </td>
                        <td className="p-3 font-mono font-bold text-slate-700 bg-slate-50">{ord.handoverPin}</td>
                        <td className="p-3">
                          <span className="font-bold text-slate-700 uppercase">{ord.paymentMethod}</span>
                          <div className="text-[10px] text-emerald-600 font-semibold">{ord.paymentStatus}</div>
                        </td>
                        <td className="p-3 font-mono font-bold text-slate-900">${ord.total.toFixed(2)}</td>
                        <td className="p-3">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              ord.status === 'delivered'
                                ? 'bg-emerald-100 text-emerald-800'
                                : ord.status === 'in_transit'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {ord.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* SECTION: SUBSCRIPTION APPROVALS CONSOLE */}
          {activeSection === 'subscriptions' && (
            <div className="space-y-6">
              {/* Top Banner & Stats */}
              <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="w-8 h-8 rounded-xl bg-red-100 text-red-600 flex items-center justify-center font-bold text-sm">
                        💳
                      </span>
                      <h3 className="text-base font-bold text-slate-900">
                        Oggolaanshaha Lacagaha Bisha (Subscription Approvals Console)
                      </h3>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">
                      Kormeerka lacagaha ay ganacsatadu ($30/mo) iyo darawalladu ($15/mo) u soo direen maamulka si loogu ansixiyo barnaamijka (0% Commission Model).
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold bg-amber-100 text-amber-900 px-3 py-1.5 rounded-xl border border-amber-300">
                      {totalPendingSubs} Lacag-bixinood oo Sugaya
                    </span>
                  </div>
                </div>

                {/* 3 Metric Summary Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-slate-100">
                  <div className="p-3.5 bg-amber-50 rounded-2xl border border-amber-200 space-y-1">
                    <span className="text-[11px] font-bold text-amber-800 uppercase tracking-wider block">
                      Ganacsatada Sugaya ($30/mo)
                    </span>
                    <div className="text-2xl font-black font-mono text-slate-900">
                      {pendingMerchantSubs.length}
                    </div>
                    <p className="text-[10px] text-amber-700">
                      Wadarta: ${pendingMerchantSubs.length * 30}.00 sugaya xaqiijin
                    </p>
                  </div>

                  <div className="p-3.5 bg-blue-50 rounded-2xl border border-blue-200 space-y-1">
                    <span className="text-[11px] font-bold text-blue-800 uppercase tracking-wider block">
                      Darawallada Sugaya ($15/mo)
                    </span>
                    <div className="text-2xl font-black font-mono text-slate-900">
                      {pendingDriverSubs.length}
                    </div>
                    <p className="text-[10px] text-blue-700">
                      Wadarta: ${pendingDriverSubs.length * 15}.00 sugaya xaqiijin
                    </p>
                  </div>

                  <div className="p-3.5 bg-emerald-50 rounded-2xl border border-emerald-200 space-y-1">
                    <span className="text-[11px] font-bold text-emerald-800 uppercase tracking-wider block">
                      Dakhliga Bisha (Total MRR)
                    </span>
                    <div className="text-2xl font-black font-mono text-emerald-700">
                      ${totalMonthlyMRR.toLocaleString()}
                    </div>
                    <p className="text-[10px] text-emerald-700">
                      {merchants.length} dukaamo + {couriers.length} darawallo
                    </p>
                  </div>
                </div>
              </div>

              {/* PART 1: PENDING MERCHANTS */}
              <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Store className="w-5 h-5 text-red-600" />
                    <h4 className="text-sm font-bold text-slate-900">
                      Ganacsatada Soo Dirtay Lacagta Bisha ($30.00)
                    </h4>
                  </div>
                  <span className="text-xs font-bold text-slate-500">
                    {pendingMerchantSubs.length} sugaya
                  </span>
                </div>

                {pendingMerchantSubs.length === 0 ? (
                  <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-100 text-xs text-slate-500">
                    ✅ Dhammaan lacagaha ganacsatada hadda waa la ansixiyay. Ma jiraan kuwa sugaya!
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {pendingMerchantSubs.map((m) => (
                      <div
                        key={m.id}
                        className="p-4 rounded-2xl border-2 border-amber-300 bg-amber-50/30 space-y-3 shadow-xs"
                      >
                        <div className="flex justify-between items-start">
                          <div className="flex items-center gap-2.5">
                            <span className="text-2xl">{m.logo}</span>
                            <div>
                              <h4 className="text-sm font-bold text-slate-900">{m.name}</h4>
                              <p className="text-xs text-slate-500">
                                Mulkiilaha: <b>{m.ownerName}</b> · {m.district}
                              </p>
                            </div>
                          </div>
                          <span className="bg-amber-100 text-amber-900 font-bold text-[10px] px-2.5 py-1 rounded-full border border-amber-300 animate-pulse">
                            Sugaya $30
                          </span>
                        </div>

                        {/* Payment Details Box */}
                        <div className="p-3 bg-white rounded-xl border border-amber-200 space-y-1.5 text-xs font-sans">
                          <div className="flex justify-between items-center">
                            <span className="text-slate-600">Numberka laga soo diray:</span>
                            <span className="font-mono font-black text-red-600 text-sm">
                              {m.subscription?.payerPhone || m.paymentPhone}
                            </span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-slate-600">Habka / Khidmadda:</span>
                            <span className="font-mono font-bold text-slate-900">
                              {m.subscription?.paymentMethod.toUpperCase()} · $30.00 / bishii
                            </span>
                          </div>
                          {m.subscription?.submittedAt && (
                            <div className="flex justify-between items-center text-[10px] text-slate-500">
                              <span>Waqtiga la soo diray:</span>
                              <span className="font-mono">{new Date(m.subscription.submittedAt).toLocaleTimeString()}</span>
                            </div>
                          )}
                          {m.subscription?.manualNotes && (
                            <div className="pt-1 border-t border-slate-100 text-[10px] text-amber-800 italic">
                              Xusuusin: "{m.subscription.manualNotes}"
                            </div>
                          )}
                        </div>

                        {/* Action Buttons: Approve, WhatsApp, Call, Reject */}
                        <div className="space-y-2 pt-1">
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleApproveSubscription('merchant', m.id)}
                              className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Oggolow & Fur Dukaanka ($30)</span>
                            </button>
                            <button
                              onClick={() => handleRejectSubscription('merchant', m.id)}
                              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition-colors"
                              title="Diid lacag-bixinta haddii aadan helin"
                            >
                              Diid
                            </button>
                          </div>

                          <div className="flex gap-2">
                            <a
                              href={`https://wa.me/${(m.whatsappNumber || m.phone).replace(/\D/g, '')}?text=${encodeURIComponent(
                                `Asc ${m.name}, waxaan nahay maamulka Shiri Delivery. Waxaan helnay codsiga bixinta bisha ($30). Mahadsanid!`
                              )}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex-1 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 transition-colors"
                            >
                              <MessageCircle className="w-3 h-3 text-emerald-600" />
                              <span>WhatsApp</span>
                            </a>
                            <a
                              href={`tel:${m.callPhone || m.phone}`}
                              className="flex-1 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 transition-colors"
                            >
                              <Phone className="w-3 h-3 text-slate-600" />
                              <span>Wac</span>
                            </a>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* PART 2: PENDING DRIVERS */}
              <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Bike className="w-5 h-5 text-red-600" />
                    <h4 className="text-sm font-bold text-slate-900">
                      Darawallada Soo Dirtay Lacagta Bisha ($15.00)
                    </h4>
                  </div>
                  <span className="text-xs font-bold text-slate-500">
                    {pendingDriverSubs.length} sugaya
                  </span>
                </div>

                {pendingDriverSubs.length === 0 ? (
                  <div className="p-8 text-center bg-slate-50 rounded-2xl border border-slate-100 text-xs text-slate-500">
                    ✅ Dhammaan lacagaha darawallada hadda waa la ansixiyay. Ma jiraan kuwa sugaya!
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {pendingDriverSubs.map((c) => (
                      <div
                        key={c.id}
                        className="p-4 rounded-2xl border-2 border-blue-300 bg-blue-50/30 space-y-3 shadow-xs"
                      >
                        <div className="flex justify-between items-start">
                          <div className="flex items-center gap-2.5">
                            <span className="text-2xl">{c.avatar}</span>
                            <div>
                              <h4 className="text-sm font-bold text-slate-900">{c.name}</h4>
                              <p className="text-xs text-slate-500">
                                {c.vehicleType} · Taargada: <b>{c.plateNumber}</b>
                              </p>
                            </div>
                          </div>
                          <span className="bg-blue-100 text-blue-900 font-bold text-[10px] px-2.5 py-1 rounded-full border border-blue-300 animate-pulse">
                            Sugaya $15
                          </span>
                        </div>

                        {/* Payment Details Box */}
                        <div className="p-3 bg-white rounded-xl border border-blue-200 space-y-1.5 text-xs font-sans">
                          <div className="flex justify-between items-center">
                            <span className="text-slate-600">Numberka laga soo diray:</span>
                            <span className="font-mono font-black text-red-600 text-sm">
                              {c.subscription?.payerPhone || c.paymentPhone}
                            </span>
                          </div>
                          <div className="flex justify-between items-center">
                            <span className="text-slate-600">Habka / Khidmadda:</span>
                            <span className="font-mono font-bold text-slate-900">
                              {c.subscription?.paymentMethod.toUpperCase()} · $15.00 / bishii
                            </span>
                          </div>
                          {c.subscription?.submittedAt && (
                            <div className="flex justify-between items-center text-[10px] text-slate-500">
                              <span>Waqtiga la soo diray:</span>
                              <span className="font-mono">{new Date(c.subscription.submittedAt).toLocaleTimeString()}</span>
                            </div>
                          )}
                          {c.subscription?.manualNotes && (
                            <div className="pt-1 border-t border-slate-100 text-[10px] text-blue-800 italic">
                              Xusuusin: "{c.subscription.manualNotes}"
                            </div>
                          )}
                        </div>

                        {/* Action Buttons: Approve, WhatsApp, Call, Reject */}
                        <div className="space-y-2 pt-1">
                          <div className="flex gap-2">
                            <button
                              onClick={() => handleApproveSubscription('courier', c.id)}
                              className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>Oggolow & Fur Darawalka ($15)</span>
                            </button>
                            <button
                              onClick={() => handleRejectSubscription('courier', c.id)}
                              className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-xl transition-colors"
                              title="Diid lacag-bixinta haddii aadan helin"
                            >
                              Diid
                            </button>
                          </div>

                          <div className="flex gap-2">
                            <a
                              href={`https://wa.me/${(c.whatsappNumber || c.phone).replace(/\D/g, '')}?text=${encodeURIComponent(
                                `Asc ${c.name}, waxaan nahay maamulka Shiri Delivery. Waxaan helnay codsiga bixinta bisha darawalka ($15). Mahadsanid!`
                              )}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex-1 py-1.5 bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-200 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 transition-colors"
                            >
                              <MessageCircle className="w-3 h-3 text-emerald-600" />
                              <span>WhatsApp</span>
                            </a>
                            <a
                              href={`tel:${c.callPhone || c.phone}`}
                              className="flex-1 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-800 rounded-xl text-[11px] font-bold flex items-center justify-center gap-1 transition-colors"
                            >
                              <Phone className="w-3 h-3 text-slate-600" />
                              <span>Wac</span>
                            </a>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* SECTION 4: MERCHANTS MANAGEMENT */}
          {activeSection === 'merchants' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Registered Merchants & Subscriptions
                  </h3>
                  <p className="text-xs text-slate-500">
                    0% Commission SaaS Model · $30/month manual subscription per merchant
                  </p>
                </div>
                <button
                  onClick={() => setIsAddMerchantOpen(true)}
                  className="px-3.5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>+ Diiwaangeli Meherad Cusub</span>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {merchants.map((m) => (
                  <div key={m.id} className="p-4 rounded-2xl border border-slate-200 bg-white space-y-3">
                    <div className="flex justify-between items-start">
                      <div className="flex items-center gap-2.5">
                        <span className="text-2xl">{m.logo}</span>
                        <div>
                          <h4 className="text-sm font-bold text-slate-900">{m.name}</h4>
                          <p className="text-xs text-slate-500 font-mono">
                            Address: {m.addressCode} · {m.district}
                          </p>
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        <span className="bg-emerald-100 text-emerald-800 font-bold text-[10px] px-2 py-0.5 rounded-full">
                          {m.verificationStatus}
                        </span>
                        {m.subscription?.manualPaymentStatus === 'pending_verification' ? (
                          <span className="bg-amber-100 text-amber-900 font-mono text-[9px] font-bold px-2 py-0.5 rounded-full border border-amber-300 animate-pulse">
                            Sub: Sugaya $30
                          </span>
                        ) : (
                          <span className="bg-blue-50 text-blue-700 font-mono text-[9px] font-bold px-2 py-0.5 rounded-full border border-blue-200">
                            {m.subscription?.status === 'active' ? 'Sub: Active ($30)' : 'Sub: Pending'}
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Contact & Payment Info */}
                    <div className="text-xs text-slate-600 space-y-1.5 p-2.5 bg-slate-50 rounded-xl">
                      <div className="flex justify-between items-center">
                        <span>Owner: <b>{m.ownerName}</b></span>
                        <span>Today Sales: <b className="font-mono text-emerald-600">${m.salesToday.toFixed(2)}</b></span>
                      </div>
                      <div className="flex justify-between items-center">
                        <span>Payment Till / Account:</span>
                        <b className="font-mono text-slate-900">{m.paymentPhone || m.phone}</b>
                      </div>
                      {m.subscription?.payerPhone && (
                        <div className="flex justify-between items-center text-amber-900 bg-amber-50 p-1.5 rounded-lg border border-amber-200 text-[11px]">
                          <span>Laga soo diray:</span>
                          <b className="font-mono text-red-600 font-bold">{m.subscription.payerPhone}</b>
                        </div>
                      )}
                      <div className="flex justify-between items-center pt-1 border-t border-slate-200">
                        <span>Direct Contacts:</span>
                        <div className="flex items-center gap-2">
                          <a
                            href={`tel:${m.callPhone || m.phone}`}
                            className="px-2 py-0.5 bg-white hover:bg-slate-200 border border-slate-200 rounded text-[11px] font-semibold text-slate-700 flex items-center gap-1"
                          >
                            <Phone className="w-3 h-3 text-slate-500" />
                            <span>Call</span>
                          </a>
                          <a
                            href={`https://wa.me/${(m.whatsappNumber || m.phone).replace(/\D/g, '')}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-2 py-0.5 bg-emerald-100 hover:bg-emerald-200 text-emerald-800 rounded text-[11px] font-bold flex items-center gap-1"
                          >
                            <MessageCircle className="w-3 h-3 text-emerald-600" />
                            <span>WhatsApp</span>
                          </a>
                        </div>
                      </div>
                    </div>

                    <div className="flex gap-2 pt-1">
                      <button
                        onClick={() => handleApproveSubscription('merchant', m.id)}
                        className="flex-1 py-1.5 bg-emerald-600 text-white font-bold text-xs rounded-xl hover:bg-emerald-700"
                        title="Xaqiiji lacagta bisha ee meheradda ($30)"
                      >
                        Xaqiiji Lacagta Bisha ($30)
                      </button>
                      <button
                        onClick={() => platformStore.toggleMerchantOpen(m.id)}
                        className="px-3 py-1.5 bg-slate-100 text-slate-700 font-bold text-xs rounded-xl hover:bg-slate-200"
                      >
                        {m.isOpen ? 'Pause' : 'Resume'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SECTION 5: COURIER FLEET MANAGEMENT */}
          {activeSection === 'couriers' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Courier Fleet & Driver Subscriptions
                  </h3>
                  <p className="text-xs text-slate-500">
                    0% Commission SaaS Model · $15/month manual subscription per driver
                  </p>
                </div>
                <button
                  onClick={() => setIsAddCourierOpen(true)}
                  className="px-3.5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition-colors"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>+ Diiwaangeli Darawal Cusub</span>
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {couriers.map((c) => (
                  <div key={c.id} className="p-4 rounded-2xl border border-slate-200 bg-white space-y-3">
                    <div className="flex justify-between items-start">
                      <div className="flex items-center gap-2">
                        <span className="text-xl">{c.avatar}</span>
                        <div>
                          <h4 className="text-sm font-bold text-slate-900">{c.name}</h4>
                          <p className="text-[11px] font-mono text-slate-500">
                            {c.vehicleType} · {c.plateNumber}
                          </p>
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-1">
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${c.isOnline ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>
                          {c.isOnline ? 'ONLINE' : 'OFFLINE'}
                        </span>
                        <span className="bg-blue-50 text-blue-700 font-mono text-[9px] font-bold px-1.5 py-0.5 rounded-full border border-blue-200">
                          {c.subscription?.status === 'active' ? 'Sub: Active ($15)' : 'Sub: Pending'}
                        </span>
                      </div>
                    </div>

                    <div className="text-xs text-slate-600 space-y-1.5 p-2.5 bg-slate-50 rounded-xl">
                      <div className="flex justify-between items-center">
                        <span>Payment (EVC):</span>
                        <b className="font-mono text-slate-900">{c.paymentPhone || c.phone}</b>
                      </div>
                      <div className="flex justify-between items-center">
                        <span>Today's Trips:</span>
                        <b>{c.completedDeliveriesToday} safar</b>
                      </div>
                      <div className="flex justify-between items-center">
                        <span>Earnings (100%):</span>
                        <b className="font-mono text-emerald-600">${c.todayEarnings.toFixed(2)}</b>
                      </div>
                      <div className="flex justify-between items-center pt-1 border-t border-slate-200">
                        <span>Direct Contacts:</span>
                        <div className="flex items-center gap-1.5">
                          <a
                            href={`tel:${c.callPhone || c.phone}`}
                            className="px-1.5 py-0.5 bg-white hover:bg-slate-200 border border-slate-200 rounded text-[10px] font-semibold text-slate-700 flex items-center gap-0.5"
                          >
                            <Phone className="w-2.5 h-2.5 text-slate-500" />
                            <span>Call</span>
                          </a>
                          <a
                            href={`https://wa.me/${(c.whatsappNumber || c.phone).replace(/\D/g, '')}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-1.5 py-0.5 bg-emerald-100 hover:bg-emerald-200 text-emerald-800 rounded text-[10px] font-bold flex items-center gap-0.5"
                          >
                            <MessageCircle className="w-2.5 h-2.5 text-emerald-600" />
                            <span>WhatsApp</span>
                          </a>
                        </div>
                      </div>
                    </div>

                    <div className="pt-1">
                      <button
                        onClick={() => handleApproveSubscription('courier', c.id)}
                        className="w-full py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl transition-colors"
                        title="Xaqiiji lacagta bisha ee darawalka ($15)"
                      >
                        Xaqiiji Lacagta Bisha ($15)
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SECTION 6: SHIRI ADDRESS REGISTRY */}
          {activeSection === 'addresses' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <h3 className="text-base font-bold text-slate-900">
                Shiri Address Registry (Diiwaanka Guri Number Digital ah)
              </h3>
              <p className="text-xs text-slate-500">
                Central authoritative database of Mogadishu digital addresses with GPS coordinates and QR payloads
              </p>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="border-b bg-slate-50 text-slate-600 font-bold">
                      <th className="p-3">Address Code</th>
                      <th className="p-3">Owner</th>
                      <th className="p-3">Label</th>
                      <th className="p-3">District</th>
                      <th className="p-3">Street / Building</th>
                      <th className="p-3">Landmark</th>
                      <th className="p-3">GPS Coordinates</th>
                      <th className="p-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {addresses.map((addr) => (
                      <tr key={addr.id} className="hover:bg-slate-50">
                        <td className="p-3 font-mono font-bold text-red-600">{addr.addressCode}</td>
                        <td className="p-3 font-medium text-slate-800">{addr.ownerName}</td>
                        <td className="p-3">{addr.label}</td>
                        <td className="p-3 font-semibold text-slate-800">{addr.district} ({addr.neighborhood})</td>
                        <td className="p-3">{addr.street || 'Waddo'} · {addr.buildingOrFloor}</td>
                        <td className="p-3 text-slate-600 max-w-xs truncate">{addr.landmark}</td>
                        <td className="p-3 font-mono text-[11px] text-slate-500">
                          <div className="flex items-center gap-1.5">
                            <span>{addr.coordinates.lat.toFixed(5)}, {addr.coordinates.lng.toFixed(5)}</span>
                            <a
                              href={buildGoogleMapsViewUrl(addr.coordinates, addr.addressCode)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-slate-400 hover:text-emerald-600 transition-colors"
                              title="Open in Google Maps"
                            >
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          </div>
                        </td>
                        <td className="p-3">
                          <span className="bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded text-[10px] font-bold">
                            Verified
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* SECTION 7: TELECOM & PAYMENTS GATEWAY */}
          {activeSection === 'telecom' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <h3 className="text-base font-bold text-slate-900">
                Somalia Telecom Payment Gateway Integration
              </h3>
              <p className="text-xs text-slate-500">
                Operational status of Hormuud EVC Plus, Somtel eDahab, and Telesom ZAAD providers
              </p>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 space-y-2">
                  <div className="flex justify-between items-center font-bold text-xs text-emerald-900">
                    <span>Hormuud EVC Plus</span>
                    <span className="w-2 h-2 rounded-full bg-emerald-600" />
                  </div>
                  <p className="text-[11px] text-emerald-700">USSD: *712*...# · REST API Active</p>
                  <div className="text-[10px] font-mono text-emerald-800">Success Rate: 99.8%</div>
                </div>

                <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 space-y-2">
                  <div className="flex justify-between items-center font-bold text-xs text-amber-900">
                    <span>Somtel eDahab</span>
                    <span className="w-2 h-2 rounded-full bg-amber-600" />
                  </div>
                  <p className="text-[11px] text-amber-700">USSD: *789*...# · Webhook Active</p>
                  <div className="text-[10px] font-mono text-amber-800">Success Rate: 99.4%</div>
                </div>

                <div className="p-4 rounded-2xl bg-blue-50 border border-blue-200 space-y-2">
                  <div className="flex justify-between items-center font-bold text-xs text-blue-900">
                    <span>Telesom ZAAD</span>
                    <span className="w-2 h-2 rounded-full bg-blue-600" />
                  </div>
                  <p className="text-[11px] text-blue-700">USSD: *880*...# · Active</p>
                  <div className="text-[10px] font-mono text-blue-800">Success Rate: 99.6%</div>
                </div>
              </div>
            </div>
          )}

          {/* SECTION 8: SYSTEM AUDIT LOGS */}
          {activeSection === 'audit' && (
            <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
              <h3 className="text-base font-bold text-slate-900">
                System Security & Financial Audit Logs
              </h3>
              <div className="space-y-2">
                {auditLogs.map((log) => (
                  <div key={log.id} className="p-3 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs">
                    <div>
                      <span className="font-bold text-slate-900 font-mono">{log.action}</span>
                      <span className="text-slate-500 ml-2">by {log.actor}</span>
                      <p className="text-[11px] text-slate-600 mt-0.5">{log.details}</p>
                    </div>
                    <span className="text-[10px] text-slate-400 font-mono shrink-0">
                      {new Date(log.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Modal: Register New Merchant */}
      {isAddMerchantOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl border border-slate-100 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Diiwaangeli Meherad Cusub (Register Merchant)
                </h3>
                <p className="text-[11px] text-slate-500">
                  Qorshaha Bisha: $30/mo (0% Commission Model)
                </p>
              </div>
              <button
                onClick={() => setIsAddMerchantOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleRegisterMerchant} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Magaca Meheradda *</label>
                <input
                  type="text"
                  required
                  value={mName}
                  onChange={(e) => setMName(e.target.value)}
                  placeholder="e.g. Al-Najax Fast Food"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Degmada *</label>
                  <select
                    value={mDistrict}
                    onChange={(e) => setMDistrict(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none"
                  >
                    <option value="Hodan">Hodan</option>
                    <option value="Waberi">Waberi</option>
                    <option value="Hamarweyne">Hamarweyne</option>
                    <option value="Wadajir">Wadajir</option>
                    <option value="Dayniile">Dayniile</option>
                    <option value="Yaqshid">Yaqshid</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Shiri Address Code *</label>
                  <input
                    type="text"
                    required
                    value={mAddressCode}
                    onChange={(e) => setMAddressCode(e.target.value)}
                    placeholder="SHR-BO-..."
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka Lacag-Bixinta / Till Number (EVC Plus) *
                </label>
                <input
                  type="text"
                  required
                  value={mPaymentPhone}
                  onChange={(e) => setMPaymentPhone(e.target.value)}
                  placeholder="061XXXXXXX ama Till Number"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">WhatsApp Meheradda *</label>
                  <input
                    type="text"
                    required
                    value={mWhatsapp}
                    onChange={(e) => setMWhatsapp(e.target.value)}
                    placeholder="25261XXXXXXX"
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Taleefanka Wacitaanka *</label>
                  <input
                    type="text"
                    required
                    value={mPhone}
                    onChange={(e) => setMPhone(e.target.value)}
                    placeholder="+252 61 XXXXXXX"
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">Magaca Milkiilaha (Owner)</label>
                <input
                  type="text"
                  value={mOwner}
                  onChange={(e) => setMOwner(e.target.value)}
                  placeholder="e.g. Cabdi Cali"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all"
              >
                Diiwaangeli Meheradda ($30 Subscription)
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Register New Courier */}
      {isAddCourierOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl border border-slate-100 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Diiwaangeli Darawal Cusub (Register Courier)
                </h3>
                <p className="text-[11px] text-slate-500">
                  Qorshaha Bisha: $15/mo (100% Darawalkaa iska leh lacagta safarka)
                </p>
              </div>
              <button
                onClick={() => setIsAddCourierOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleRegisterCourier} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">Magaca Darawalka *</label>
                <input
                  type="text"
                  required
                  value={cName}
                  onChange={(e) => setCName(e.target.value)}
                  placeholder="e.g. Liibaan Maxamed"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Nooca Gaadiidka *</label>
                  <select
                    value={cVehicleType}
                    onChange={(e) => setCVehicleType(e.target.value as any)}
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 outline-none"
                  >
                    <option value="Bajaj">Bajaj (Mooto 3-Lugood)</option>
                    <option value="Motorcycle">Motorcycle (2-Lugood)</option>
                    <option value="Car">Gaari (Car)</option>
                  </select>
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Taargada (Plate) *</label>
                  <input
                    type="text"
                    required
                    value={cPlate}
                    onChange={(e) => setCPlate(e.target.value)}
                    placeholder="MG-7788-B"
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka Lacag-Bixinta (EVC Plus / eDahab) *
                </label>
                <input
                  type="text"
                  required
                  value={cPaymentPhone}
                  onChange={(e) => setCPaymentPhone(e.target.value)}
                  placeholder="061XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">WhatsApp Darawalka *</label>
                  <input
                    type="text"
                    required
                    value={cWhatsapp}
                    onChange={(e) => setCWhatsapp(e.target.value)}
                    placeholder="25261XXXXXXX"
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">Taleefanka Wacitaanka *</label>
                  <input
                    type="text"
                    required
                    value={cPhone}
                    onChange={(e) => setCPhone(e.target.value)}
                    placeholder="+252 61 XXXXXXX"
                    className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all"
              >
                Diiwaangeli Darawalka ($15 Subscription)
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
