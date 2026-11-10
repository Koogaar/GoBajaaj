import React, { useState, useEffect } from 'react';
import { Courier, Order, PaymentMethod } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { MapCanvas } from '../common/MapCanvas';
import { LocationService } from '../../services/locationService';
import {
  buildGoogleMapsNavigateUrl,
  buildGoogleMapsViewUrl,
} from '../../utils/coordinateParser';
import {
  Bike,
  DollarSign,
  CheckCircle2,
  Navigation,
  Phone,
  Power,
  ShieldCheck,
  AlertTriangle,
  Clock,
  MapPin,
  KeyRound,
  ArrowRight,
  TrendingUp,
  User,
  History,
  ExternalLink,
  Compass,
  Inbox,
  MessageCircle,
  PlusCircle,
  Edit3,
  Volume2,
} from 'lucide-react';
import { VoiceNotePlayer } from '../common/VoiceNotePlayer';
import { soundEffects } from '../../utils/soundEffects';
import { buildDriverToCustomerWhatsappUrl } from '../../utils/whatsappHelper';

export const CourierApp: React.FC = () => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  // Active driver in simulator: Ahmed Hassan (Canab)
  const [selectedCourierId, setSelectedCourierId] = useState<string>('cour-1');
  const [couriers, setCouriers] = useState<Courier[]>(platformStore.getCouriers());
  const [orders, setOrders] = useState<Order[]>(platformStore.getOrders());

  // Active view tab: 'dashboard' | 'incoming_orders' | 'delivery' | 'earnings' | 'profile'
  const [activeTab, setActiveTab] = useState<'dashboard' | 'incoming_orders' | 'delivery' | 'earnings' | 'profile'>('dashboard');

  // Delivery Handover PIN input state (e.g. 4-digit keypad)
  const [enteredPin, setEnteredPin] = useState<string>('');
  const [pinError, setPinError] = useState<string | null>(null);
  const [deliverySuccess, setDeliverySuccess] = useState<boolean>(false);

  // Driver contact & manual payment profile modal state
  const [isEditProfileOpen, setIsEditProfileOpen] = useState<boolean>(false);
  const [editPaymentPhone, setEditPaymentPhone] = useState<string>('');
  const [editWhatsapp, setEditWhatsapp] = useState<string>('');
  const [editCallPhone, setEditCallPhone] = useState<string>('');

  // Driver Subscription Payment Modal state (paying $15 to admin)
  const [isPaySubscriptionOpen, setIsPaySubscriptionOpen] = useState<boolean>(false);
  const [subPayerPhone, setSubPayerPhone] = useState<string>('0613456789');
  const [subPaymentMethod, setSubPaymentMethod] = useState<PaymentMethod>('evc_plus');
  const [subNotes, setSubNotes] = useState<string>('');

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      setCouriers([...platformStore.getCouriers()]);
      setOrders([...platformStore.getOrders()]);
    });
    return unsub;
  }, []);

  const courier = couriers.find((c) => c.id === selectedCourierId) || couriers[0];
  const activeOrder = orders.find((o) => o.id === courier.activeOrderId);

  // Watch real device GPS position when courier is online
  useEffect(() => {
    if (!courier.isOnline) return;

    const watchId = LocationService.watchLocation(
      (coords, _accuracy) => {
        platformStore.updateCourierLocation(courier.id, coords);
      },
      (err: string) => {
        console.warn('Courier GPS watch error:', err);
      }
    );

    return () => {
      if (watchId !== null) LocationService.clearWatch(watchId);
    };
  }, [courier.isOnline, courier.id]);

  // Incoming unassigned orders ready for pickup
  const incomingDispatchOrder = orders.find(
    (o) => o.status === 'ready_for_pickup' && !o.courierId && courier.isOnline
  );

  const handleToggleOnline = () => {
    platformStore.toggleCourierOnline(courier.id);
  };

  const handleAcceptTrip = (orderId: string) => {
    platformStore.courierAcceptTrip(courier.id, orderId);
    setActiveTab('delivery');
  };

  const handleConfirmPickup = (orderId: string) => {
    platformStore.courierConfirmPickup(orderId);
  };

  const handleVerifyDeliveryPin = () => {
    if (!activeOrder) return;
    setPinError(null);

    const res = platformStore.courierCompleteDelivery(activeOrder.id, enteredPin);
    if (!res.success) {
      setPinError(res.message);
    } else {
      setDeliverySuccess(true);
      setEnteredPin('');
      setTimeout(() => {
        setDeliverySuccess(false);
        setActiveTab('dashboard');
      }, 2500);
    }
  };

  const unassignedOrders = orders.filter(
    (o) => !o.courierId && o.status !== 'delivered' && o.status !== 'cancelled'
  );

  const handleOpenEditProfile = () => {
    setEditPaymentPhone(courier.paymentPhone || courier.phone);
    setEditWhatsapp(courier.whatsappNumber || courier.phone.replace(/\D/g, ''));
    setEditCallPhone(courier.callPhone || courier.phone);
    setIsEditProfileOpen(true);
  };

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    platformStore.updateCourierProfile(courier.id, {
      paymentPhone: editPaymentPhone,
      whatsappNumber: editWhatsapp,
      callPhone: editCallPhone,
    });
    setIsEditProfileOpen(false);
  };

  const handleSubmitSubscription = (e: React.FormEvent) => {
    e.preventDefault();
    if (!subPayerPhone.trim()) {
      alert('Fadlan geli numberka aad lacagta ka dirtay');
      return;
    }
    const res = platformStore.submitSubscriptionPayment('courier', courier.id, {
      payerPhone: subPayerPhone.trim(),
      paymentMethod: subPaymentMethod,
      notes: subNotes.trim() || undefined,
    });
    alert(res.message);
    setIsPaySubscriptionOpen(false);
  };

  const handleSimulateNewOrder = () => {
    platformStore.simulateIncomingOrder();
    setActiveTab('incoming_orders');
  };

  return (
    <div className="flex flex-col h-full bg-slate-50 relative">
      {/* Top Mobile App Bar */}
      <header className="sticky top-0 z-30 bg-white px-4 py-3 border-b border-slate-100 flex items-center justify-between shadow-2xs">
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-2xl bg-slate-900 text-white flex items-center justify-center text-lg">
            {courier.avatar}
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <h2 className="text-sm font-bold text-slate-900 leading-tight">
                {courier.name}
              </h2>
              <span
                className={`w-2 h-2 rounded-full ${
                  courier.isOnline ? 'bg-emerald-500' : 'bg-slate-400'
                }`}
              />
            </div>
            <p className="text-[10px] font-mono text-slate-500">
              {courier.vehicleType} · {courier.plateNumber} · ⭐ {courier.rating}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            onClick={() => soundEffects.playDispatchAlert()}
            className="p-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 transition-colors flex items-center gap-1 text-[11px] font-bold"
            title="Tijaabi Digniinta Darawalka (Test Dispatch Siren)"
          >
            <Volume2 className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Alert</span>
          </button>

          {/* Online / Offline switch */}
          <button
            onClick={handleToggleOnline}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
              courier.isOnline
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            <Power className="w-3.5 h-3.5" />
            <span>{courier.isOnline ? 'Online' : 'Offline'}</span>
          </button>
        </div>
      </header>

      {/* Main Tab View */}
      <div className="flex-1 overflow-y-auto p-4 pb-24 space-y-4">
        {/* Incoming Dispatch Request Alert Modal / Banner */}
        {incomingDispatchOrder && !activeOrder && (
          <div className="bg-gradient-to-br from-red-600 to-rose-700 text-white p-4 rounded-3xl shadow-xl shadow-red-600/30 space-y-3 animate-bounce">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-7 h-7 rounded-xl bg-white/20 flex items-center justify-center text-sm font-bold">
                  🔔
                </span>
                <span className="text-xs font-black uppercase tracking-wider">
                  {t.newDeliveryRequest}
                </span>
              </div>
              <span className="text-sm font-mono font-black text-amber-300">
                +${(incomingDispatchOrder.deliveryFee + 4.5).toFixed(2)}
              </span>
            </div>

            <div className="bg-white/10 backdrop-blur-xs p-3 rounded-2xl space-y-1.5 text-xs">
              <div className="flex items-center gap-2 min-w-0">
                <span className="text-amber-300 shrink-0">🏪 {t.pickupFrom}</span>
                <span className="font-bold truncate">{incomingDispatchOrder.merchantName} ({incomingDispatchOrder.merchantDistrict})</span>
              </div>
              <div className="flex items-center gap-2 min-w-0">
                <span className="text-emerald-300 shrink-0">📍 {t.deliverTo}</span>
                <span className="font-bold truncate">{incomingDispatchOrder.deliveryAddress.addressCode} ({incomingDispatchOrder.deliveryAddress.district})</span>
              </div>
              <div className="text-[11px] text-red-100 pt-1 flex justify-between">
                <span>Distance: ~2.4 km · ETA 12 min</span>
                <span>Payment: {incomingDispatchOrder.paymentMethod.toUpperCase()}</span>
              </div>
              <div className="text-[10px] text-red-200/90 pt-1 border-t border-white/10 flex items-center justify-between">
                <span className="font-mono">
                  GPS: {incomingDispatchOrder.deliveryAddress.coordinates.lat.toFixed(5)}, {incomingDispatchOrder.deliveryAddress.coordinates.lng.toFixed(5)}
                </span>
                <a
                  href={buildGoogleMapsViewUrl(
                    incomingDispatchOrder.deliveryAddress.coordinates,
                    incomingDispatchOrder.deliveryAddress.addressCode
                  )}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline hover:text-white flex items-center gap-1 font-semibold"
                >
                  <ExternalLink className="w-3 h-3" />
                  <span>Google Maps</span>
                </a>
              </div>
            </div>

            <div className="flex gap-2 pt-1">
              <button
                onClick={() => handleAcceptTrip(incomingDispatchOrder.id)}
                className="flex-1 py-2.5 bg-white hover:bg-red-50 text-red-600 font-black text-xs rounded-xl shadow-md transition-all text-center"
              >
                {t.acceptTrip}
              </button>
              <button
                onClick={() => {}}
                className="px-4 py-2.5 bg-red-800/60 hover:bg-red-800 text-white text-xs font-bold rounded-xl transition-all"
              >
                {t.rejectTrip}
              </button>
            </div>
          </div>
        )}

        {/* TAB 1: COURIER DASHBOARD */}
        {activeTab === 'dashboard' && (
          <div className="space-y-4">
            {/* Incoming Orders Alert Banner */}
            <div className="bg-gradient-to-r from-red-600 to-rose-600 text-white p-4 rounded-3xl shadow-md space-y-2.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-amber-300 animate-ping" />
                  <span className="text-xs font-black uppercase tracking-wider">
                    Dalabyada Soo Dhacaya (Available Orders)
                  </span>
                </div>
                <span className="bg-white/20 text-white font-mono font-black text-xs px-2.5 py-0.5 rounded-full">
                  {unassignedOrders.length} Banaan
                </span>
              </div>
              <p className="text-[11px] text-red-100 leading-relaxed">
                Dalabyo cusub oo laga dalbaday maqaayadaha iyo dukaamada Mogadishu ayaa diyaar u ah qaadasho.
              </p>
              <div className="flex gap-2 pt-1">
                <button
                  onClick={() => setActiveTab('incoming_orders')}
                  className="flex-1 py-2.5 bg-white text-red-600 text-xs font-black rounded-xl shadow-xs hover:bg-red-50 transition-all flex items-center justify-center gap-1.5"
                >
                  <Inbox className="w-3.5 h-3.5 text-red-600" />
                  <span>Eeg Dalabyada Soo Dhacaya ({unassignedOrders.length})</span>
                </button>
                <button
                  onClick={handleSimulateNewOrder}
                  className="px-3 py-2.5 bg-red-800/60 hover:bg-red-800 text-white text-xs font-bold rounded-xl transition-all flex items-center gap-1"
                  title="Abuur dalab tijaabo ah oo cusub"
                >
                  <PlusCircle className="w-3.5 h-3.5" />
                  <span>Tijaabi Dalab</span>
                </button>
              </div>
            </div>

            {/* Today's Earnings Metric Card */}
            <div className="bg-gradient-to-br from-slate-900 to-slate-800 text-white p-5 rounded-3xl shadow-lg space-y-3">
              <div className="flex items-center justify-between text-slate-400">
                <span className="text-xs font-semibold uppercase tracking-wider">{t.todayEarnings}</span>
                <DollarSign className="w-4 h-4 text-emerald-400" />
              </div>
              <div className="text-3xl font-black font-mono text-emerald-400">
                ${courier.todayEarnings.toFixed(2)}
              </div>
              <div className="grid grid-cols-2 gap-3 pt-3 border-t border-slate-700 text-xs">
                <div>
                  <span className="text-slate-400">{t.completedTrips}:</span>
                  <div className="font-bold text-white font-mono mt-0.5">
                    {courier.completedDeliveriesToday} safar
                  </div>
                </div>
                <div>
                  <span className="text-slate-400">Cash on Delivery:</span>
                  <div className="font-bold text-amber-300 font-mono mt-0.5">
                    ${courier.cashCollectedToday.toFixed(2)}
                  </div>
                </div>
              </div>
            </div>

            {/* Driver Contact & Manual Payout Account Card */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold text-slate-900">
                    Xiriirka & Lacag-Bixinta Darawalka
                  </h4>
                  <p className="text-[10px] text-slate-500">
                    Nambarada lacag-bixinta EVC Plus & WhatsApp tooska ah
                  </p>
                </div>
                <button
                  onClick={handleOpenEditProfile}
                  className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[11px] font-bold flex items-center gap-1 transition-colors"
                >
                  <Edit3 className="w-3 h-3 text-slate-600" />
                  <span>Wax ka beddel</span>
                </button>
              </div>

              <div className="p-3 bg-slate-50 rounded-2xl space-y-2 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Lambarka Lacag-bixinta (EVC/eDahab):</span>
                  <span className="font-mono font-bold text-emerald-600">
                    {courier.paymentPhone || courier.phone}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Wacitaanka Caadiga ah (Direct Call):</span>
                  <a
                    href={`tel:${courier.callPhone || courier.phone}`}
                    className="font-bold text-slate-800 hover:text-red-600 flex items-center gap-1"
                  >
                    <Phone className="w-3 h-3 text-slate-400" />
                    <span>{courier.callPhone || courier.phone}</span>
                  </a>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">WhatsApp Toos ah:</span>
                  {courier.whatsappNumber ? (
                    <a
                      href={`https://wa.me/${courier.whatsappNumber.replace(/\D/g, '')}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-emerald-700 font-bold flex items-center gap-1 hover:underline"
                    >
                      <MessageCircle className="w-3 h-3 text-emerald-600" />
                      <span>{courier.whatsappNumber}</span>
                    </a>
                  ) : (
                    <span className="text-slate-400 italic">Lama gelin</span>
                  )}
                </div>
              </div>
            </div>

            {/* Active Delivery shortcut banner if driver is on active trip */}
            {activeOrder && (
              <div className="bg-white p-4 rounded-3xl border-2 border-red-500 shadow-md space-y-3">
                <div
                  onClick={() => setActiveTab('delivery')}
                  className="cursor-pointer flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-xl">
                      🛵
                    </div>
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-red-600">
                        Active Delivery in Progress
                      </span>
                      <h4 className="text-xs font-bold text-slate-900">
                        {activeOrder.orderNumber} · {activeOrder.deliveryAddress.addressCode} ({activeOrder.deliveryAddress.district})
                      </h4>
                    </div>
                  </div>
                  <ArrowRight className="w-4 h-4 text-red-600" />
                </div>

                <div className="pt-2 border-t border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                  <span className="font-mono text-[10px] text-slate-500">
                    GPS: {activeOrder.deliveryAddress.coordinates.lat.toFixed(5)}, {activeOrder.deliveryAddress.coordinates.lng.toFixed(5)}
                  </span>
                  <a
                    href={buildGoogleMapsNavigateUrl(activeOrder.deliveryAddress.coordinates)}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-xs transition-colors shrink-0 self-start sm:self-auto"
                    title="Open turn-by-turn navigation in Google Maps app"
                  >
                    <Navigation className="w-3 h-3 fill-white shrink-0" />
                    <span>Navigate with Google Maps</span>
                  </a>
                </div>
              </div>
            )}

            {/* Verification & Compliance Status */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-2.5">
              <div className="flex items-center justify-between text-xs font-bold text-slate-900">
                <span>Dukumiintiyada Darawalka (Documents)</span>
                <span className="text-emerald-600 flex items-center gap-1 text-[11px]">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Verified</span>
                </span>
              </div>
              <div className="space-y-1.5 text-xs text-slate-600">
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>National ID Card:</span>
                  <span className="font-semibold text-slate-900">Xaqiijisan (Verified)</span>
                </div>
                <div className="flex justify-between py-1 border-b border-slate-100">
                  <span>Driver's License:</span>
                  <span className="font-semibold text-slate-900">MG-DL-84920</span>
                </div>
                <div className="flex justify-between py-1">
                  <span>Vehicle Registration:</span>
                  <span className="font-semibold text-slate-900">{courier.plateNumber}</span>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB: INCOMING ORDERS FEED (DALABYADA SOO DHACAYA) */}
        {activeTab === 'incoming_orders' && (
          <div className="space-y-4">
            {/* Feed Header */}
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
                  <Inbox className="w-4 h-4 text-red-600" />
                  <span>Dalabyada Soo Dhacaya</span>
                </h3>
                <p className="text-[10px] text-slate-500">
                  Dhammaan dalabyada banaan ee diyaar u ah qaadasho
                </p>
              </div>
              <button
                onClick={handleSimulateNewOrder}
                className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-[11px] font-bold flex items-center gap-1 shadow-xs transition-colors"
              >
                <PlusCircle className="w-3.5 h-3.5" />
                <span>+ Tijaabi Dalab Cusub</span>
              </button>
            </div>

            {/* Active order notification if courier already has an active trip */}
            {activeOrder && (
              <div className="bg-amber-50 border border-amber-200 p-3 rounded-2xl text-xs space-y-1">
                <div className="flex items-center gap-1.5 text-amber-800 font-bold">
                  <AlertTriangle className="w-3.5 h-3.5 text-amber-600 shrink-0" />
                  <span>Waxaad haysataa safar socda ({activeOrder.orderNumber})</span>
                </div>
                <p className="text-[11px] text-amber-700">
                  Fadlan dhammee gaarsiintaada hadda ka hor inta aadan bilaabin safar cusub, ama aqbal kan xiga marka aad u dhowaato.
                </p>
              </div>
            )}

            {/* List of Incoming Unassigned Orders */}
            {unassignedOrders.length === 0 ? (
              <div className="bg-white p-8 rounded-3xl border border-slate-200 text-center space-y-3">
                <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-xl">
                  📭
                </div>
                <h4 className="text-sm font-bold text-slate-800">
                  Hadda ma jiraan dalabyo cusub oo sugaya
                </h4>
                <p className="text-xs text-slate-500 max-w-xs mx-auto">
                  Dalab kasta oo macmiilku ka dalbado makhaayadaha Muqdisho wuxuu si toos ah ugu soo dhacayaa halkan.
                </p>
                <button
                  onClick={handleSimulateNewOrder}
                  className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-xl transition-all shadow-xs inline-flex items-center gap-1.5"
                >
                  <PlusCircle className="w-3.5 h-3.5 text-red-400" />
                  <span>Tijaabi Soo Dhicidda Dalab Cusub</span>
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {unassignedOrders.map((ord) => {
                  const driverPay = ord.deliveryFee + 4.5;
                  return (
                    <div
                      key={ord.id}
                      className="bg-white p-4 rounded-3xl border border-slate-200 shadow-sm space-y-3 hover:border-red-300 transition-colors"
                    >
                      {/* Top Bar: Order number & Payout */}
                      <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                        <div>
                          <span className="font-mono font-bold text-xs text-slate-900">
                            {ord.orderNumber}
                          </span>
                          <div className="text-[10px] text-slate-400 flex items-center gap-1 mt-0.5">
                            <Clock className="w-3 h-3" />
                            <span>Diyaar u ah qaadasho</span>
                          </div>
                        </div>
                        <div className="text-right">
                          <span className="text-[10px] font-bold uppercase text-slate-400 block">
                            Dakhligaaga
                          </span>
                          <span className="text-base font-black font-mono text-emerald-600">
                            +${driverPay.toFixed(2)}
                          </span>
                        </div>
                      </div>

                      {/* Pickup & Delivery Steps */}
                      <div className="space-y-2 text-xs">
                        {/* Pickup Merchant */}
                        <div className="p-2.5 bg-slate-50 rounded-2xl space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] font-bold uppercase text-slate-400">
                              🏪 Ka soo qaad (Pickup)
                            </span>
                            <div className="flex items-center gap-2">
                              <a
                                href={`tel:${ord.merchantPhone}`}
                                className="text-[11px] text-slate-600 hover:text-red-600 flex items-center gap-0.5 font-semibold"
                                title="Wac Meheradda"
                              >
                                <Phone className="w-3 h-3" />
                                <span>Wac</span>
                              </a>
                              <a
                                href={`https://wa.me/${ord.merchantPhone.replace(/\D/g, '')}?text=${encodeURIComponent(`Asc waxaan ahay darawalka Shiri Delivery ee qaadaya dalabka ${ord.orderNumber}`)}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-[11px] text-emerald-600 hover:underline flex items-center gap-0.5 font-bold"
                                title="WhatsApp Meheradda"
                              >
                                <MessageCircle className="w-3 h-3" />
                                <span>WhatsApp</span>
                              </a>
                            </div>
                          </div>
                          <div className="font-bold text-slate-800">
                            {ord.merchantName} ({ord.merchantDistrict})
                          </div>
                        </div>

                        {/* Deliver to Customer Address */}
                        <div className="p-2.5 bg-red-50/50 border border-red-100 rounded-2xl space-y-1">
                          <div className="flex items-center justify-between">
                            <span className="text-[10px] font-bold uppercase text-red-600">
                              📍 U gee Macmiilka (Shiri Address)
                            </span>
                            <div className="flex items-center gap-2">
                              <a
                                href={`tel:${ord.customerPhone}`}
                                className="text-[11px] text-slate-600 hover:text-red-600 flex items-center gap-0.5 font-semibold"
                                title="Wac Macmiilka"
                              >
                                <Phone className="w-3 h-3" />
                                <span>Wac</span>
                              </a>
                              {ord.customerWhatsapp && (
                                <a
                                  href={`https://wa.me/${ord.customerWhatsapp.replace(/\D/g, '')}?text=${encodeURIComponent(`Asc waxaan ahay darawalka Shiri Delivery ee kuu sida dalabka ${ord.orderNumber}`)}`}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-[11px] text-emerald-600 hover:underline flex items-center gap-0.5 font-bold"
                                  title="WhatsApp Macmiilka"
                                >
                                  <MessageCircle className="w-3 h-3" />
                                  <span>WhatsApp</span>
                                </a>
                              )}
                            </div>
                          </div>
                          <div className="font-bold text-slate-900 font-mono">
                            {ord.deliveryAddress.addressCode} · {ord.deliveryAddress.district}
                          </div>
                          <div className="text-[11px] text-slate-600">
                            Calaamad: {ord.deliveryAddress.landmark}
                          </div>
                          {ord.deliveryInstructions && (
                            <div className="text-[10px] text-amber-800 bg-amber-100/60 px-2 py-1 rounded-lg mt-1 italic">
                              "{ord.deliveryInstructions}"
                            </div>
                          )}
                        </div>
                      </div>

                      {/* Distance & GPS link */}
                      <div className="flex items-center justify-between text-[11px] text-slate-500 pt-1">
                        <span>Payment: <strong className="text-slate-800">{ord.paymentMethod.toUpperCase()}</strong></span>
                        <a
                          href={buildGoogleMapsViewUrl(ord.deliveryAddress.coordinates, ord.deliveryAddress.addressCode)}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-red-600 hover:underline flex items-center gap-1 font-bold"
                        >
                          <ExternalLink className="w-3 h-3" />
                          <span>Google Maps Pin</span>
                        </a>
                      </div>

                      {/* Accept / Decline Action */}
                      <div className="flex gap-2 pt-1">
                        <button
                          onClick={() => handleAcceptTrip(ord.id)}
                          className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-black text-xs rounded-2xl shadow-md shadow-red-600/20 transition-all flex items-center justify-center gap-1.5"
                        >
                          <Bike className="w-4 h-4" />
                          <span>Aqbal Dalabka (+${driverPay.toFixed(2)})</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 2: ACTIVE DELIVERY TURN-BY-TURN & HANDOVER PIN VERIFICATION */}
        {activeTab === 'delivery' && (
          <div className="space-y-4">
            {!activeOrder ? (
              <div className="bg-white p-8 rounded-3xl border border-slate-200 text-center space-y-2">
                <Bike className="w-10 h-10 text-slate-300 mx-auto" />
                <h4 className="text-sm font-bold text-slate-800">Ma jiro dalab hadda kugu xiran</h4>
                <p className="text-xs text-slate-500">
                  Gal Online si laguu soo gaarsiiyo codsiyada cusub ee Mogadishu.
                </p>
                <button
                  onClick={() => setActiveTab('dashboard')}
                  className="mt-2 px-4 py-2 bg-slate-100 text-slate-700 text-xs font-bold rounded-xl"
                >
                  Ku noqo Dashboard-ka
                </button>
              </div>
            ) : (
              <div className="space-y-4">
                {/* Navigation Map */}
                <div className="rounded-3xl overflow-hidden shadow-md border border-slate-200">
                  <MapCanvas
                    mode="route"
                    height="h-56"
                    pickupCoords={activeOrder.merchantCoordinates}
                    dropoffCoords={activeOrder.deliveryAddress.coordinates}
                    courierCoords={courier.currentCoordinates}
                    pickupLabel={activeOrder.merchantName}
                    dropoffLabel={activeOrder.deliveryAddress.addressCode}
                    courierLabel={courier.name}
                  />
                </div>

                {/* Pickup & Destination Cards */}
                <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-3">
                  <div className="flex items-start justify-between border-b pb-2">
                    <div>
                      <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                        Order Details
                      </span>
                      <h4 className="text-sm font-bold text-slate-900 font-mono">
                        {activeOrder.orderNumber}
                      </h4>
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-bold text-emerald-600 font-mono">
                        +${(activeOrder.deliveryFee + 4.5).toFixed(2)}
                      </span>
                      <div className="text-[10px] text-slate-500">{activeOrder.paymentMethod.toUpperCase()}</div>
                    </div>
                  </div>

                  {/* Checkpoints */}
                  <div className="space-y-2 text-xs">
                    <div className="p-2.5 rounded-2xl bg-slate-50 border border-slate-100 flex items-start gap-2.5">
                      <span className="text-base">🏪</span>
                      <div className="flex-1 min-w-0">
                        <div className="font-bold text-slate-900">{activeOrder.merchantName}</div>
                        <div className="text-[11px] text-slate-500">{activeOrder.merchantDistrict} · {activeOrder.merchantPhone}</div>
                      </div>
                      {activeOrder.status === 'courier_assigned' && (
                        <button
                          onClick={() => handleConfirmPickup(activeOrder.id)}
                          className="px-2.5 py-1 bg-red-600 text-white text-[11px] font-bold rounded-lg shrink-0"
                        >
                          {t.confirmPickup}
                        </button>
                      )}
                    </div>

                    <div className="p-3 rounded-2xl bg-red-50/50 border border-red-100 space-y-2">
                      <div className="flex items-start gap-2.5">
                        <span className="text-base">📍</span>
                        <div className="flex-1 min-w-0">
                          <div className="font-bold text-slate-900">
                            {activeOrder.deliveryAddress.addressCode} ({activeOrder.deliveryAddress.district})
                          </div>
                          <div className="text-[11px] text-slate-600">
                            {activeOrder.deliveryAddress.street || 'Waddo'}, {activeOrder.deliveryAddress.buildingOrFloor} · {activeOrder.deliveryAddress.landmark}
                          </div>
                          <div className="text-[10px] font-semibold text-slate-700 mt-0.5">
                            Macmiil: {activeOrder.customerName} ({activeOrder.customerPhone})
                          </div>
                          <div className="flex items-center gap-1.5 shrink-0">
                            <a
                              href={`tel:${activeOrder.customerCallPhone || activeOrder.customerPhone}`}
                              className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center shrink-0 shadow-2xs hover:bg-emerald-200 transition-colors"
                              title="Wac Macmiilka"
                            >
                              <Phone className="w-3.5 h-3.5" />
                            </a>
                            <a
                              href={buildDriverToCustomerWhatsappUrl(activeOrder, courier.name)}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-2xs hover:bg-emerald-700 transition-colors"
                              title="WhatsApp Macmiilka"
                            >
                              <MessageCircle className="w-3.5 h-3.5" />
                            </a>
                          </div>
                        </div>
                      </div>

                      {/* Customer Voice Landmark Note Player */}
                      {activeOrder.voiceNote && (
                        <VoiceNotePlayer voiceNote={activeOrder.voiceNote} />
                      )}

                        {/* Google Maps Turn-by-Turn Navigation Action */}
                        <div className="pt-2 border-t border-red-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                          <div className="text-[10px] font-mono text-slate-500">
                            <span>GPS: {activeOrder.deliveryAddress.coordinates.lat.toFixed(5)}, {activeOrder.deliveryAddress.coordinates.lng.toFixed(5)}</span>
                          </div>
                          <a
                            href={buildGoogleMapsNavigateUrl(activeOrder.deliveryAddress.coordinates)}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 shadow-xs transition-colors shrink-0 self-start sm:self-auto"
                            title="Open turn-by-turn GPS navigation in Google Maps app"
                          >
                            <Navigation className="w-3 h-3 fill-white shrink-0" />
                            <span>Navigate with Google Maps</span>
                          </a>
                        </div>
                      </div>
                    </div>
                  </div>

                {/* Handover PIN Verification Keypad (Requirement 7 & 8) */}
                {(activeOrder.status === 'picked_up' || activeOrder.status === 'in_transit') && (
                  <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs space-y-4">
                    <div className="text-center space-y-1">
                      <div className="w-10 h-10 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center mx-auto">
                        <KeyRound className="w-5 h-5" />
                      </div>
                      <h4 className="text-sm font-bold text-slate-900">
                        {t.enterHandoverPin}
                      </h4>
                      <p className="text-[11px] text-slate-500">
                        Weydii macmiilka koodhka 4-ta god ah ee app-kiisa kaga qoran.
                      </p>
                    </div>

                    {/* PIN input box */}
                    <div>
                      <input
                        type="text"
                        maxLength={4}
                        value={enteredPin}
                        onChange={(e) => setEnteredPin(e.target.value.replace(/[^0-9]/g, ''))}
                        placeholder="••••"
                        className="w-44 mx-auto block text-center text-2xl font-black font-mono tracking-widest py-2 rounded-2xl border-2 border-slate-300 focus:border-red-600 outline-none"
                      />
                      {pinError && (
                        <p className="text-xs text-rose-600 text-center font-semibold mt-1">
                          {pinError}
                        </p>
                      )}
                    </div>

                    <button
                      onClick={handleVerifyDeliveryPin}
                      disabled={enteredPin.length < 4}
                      className="w-full py-3 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-lg shadow-red-600/25 transition-all flex items-center justify-center gap-2"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>{t.verifyAndComplete}</span>
                    </button>
                  </div>
                )}

                {deliverySuccess && (
                  <div className="bg-emerald-600 text-white p-4 rounded-3xl text-center space-y-1 animate-in zoom-in">
                    <CheckCircle2 className="w-8 h-8 mx-auto" />
                    <h4 className="text-sm font-bold">Gaarsiintu waa Guuleysatay!</h4>
                    <p className="text-xs text-emerald-100">
                      Dakhligaaga waxaa lagu daray +${(activeOrder.deliveryFee + 4.5).toFixed(2)}
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: EARNINGS & COD RECONCILIATION */}
        {activeTab === 'earnings' && (
          <div className="space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Dakhliga & Khidmadda Bisha (Earnings)</h3>

            {/* Courier Monthly Membership Plan Card */}
            <div className="bg-gradient-to-r from-slate-900 to-slate-800 text-white p-5 rounded-3xl shadow-md space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 px-2.5 py-0.5 rounded-full border border-emerald-500/30">
                  Qorshaha Bisha ($15/Bishii)
                </span>
                <span className="text-xs font-mono font-bold text-emerald-400">
                  0% Deductions (Commission La'aan)
                </span>
              </div>
              <div className="flex items-baseline justify-between">
                <div>
                  <h4 className="text-base font-bold text-white">Xubinnimada Darawalka</h4>
                  <p className="text-[11px] text-slate-300">Safarrada oo dhan 100% lacagtooda adaa qaata</p>
                </div>
                <span className="px-2.5 py-1 bg-emerald-500 text-slate-950 text-xs font-black rounded-xl">
                  {courier.subscription?.status === 'active' ? 'Active' : 'Pending'}
                </span>
              </div>
              <p className="text-[10px] text-slate-300 pt-2 border-t border-slate-700 leading-relaxed">
                ✅ <strong>0% Commission:</strong> Ma jiro boqolkiiba lagaa jarayo safar kasta. Waxaad bixinaysaa oo kaliya $15 bishii oo go'an!
              </p>
            </div>

            {/* Manual Payment Instructions for Driver Monthly Subscription */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200 shadow-2xs space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  Habka Bixinta Khidmadda Bisha (Manual Payment)
                </h4>
                <span className="text-[10px] bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-bold">
                  Bisha Cusub: $15
                </span>
              </div>
              <p className="text-[11px] text-slate-600 leading-relaxed">
                U wareeji lacagta bisha lambarrada rasmiga ah ee Shiri Delivery, kadibna u dir shaashadda (screenshot) WhatsApp maamulka si degdeg loogu furo barnaamijkaaga:
              </p>

              <div className="p-3 bg-slate-50 rounded-2xl space-y-2 text-xs font-mono">
                <div className="flex justify-between items-center text-emerald-800 font-bold">
                  <span>Hormuud EVC Plus:</span>
                  <span className="bg-emerald-100 px-2 py-0.5 rounded text-[11px]">*712*615000000*15#</span>
                </div>
                <div className="flex justify-between items-center text-amber-800 font-bold">
                  <span>Somtel eDahab:</span>
                  <span className="bg-amber-100 px-2 py-0.5 rounded text-[11px]">*789*650000000*15#</span>
                </div>
              </div>

              {courier.subscription?.manualPaymentStatus === 'pending_verification' && (
                <div className="p-3 bg-amber-50 rounded-2xl border border-amber-200 text-xs space-y-1">
                  <div className="flex items-center gap-1.5 font-bold text-amber-900">
                    <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />
                    <span>Lacagtaadu waxay sugaysaa oggolaanshaha Maamulka (Pending Admin Approval)</span>
                  </div>
                  <p className="text-[11px] text-amber-800">
                    Waxaad $15 ka soo dirtay lambarka: <b className="font-mono text-slate-900">{courier.subscription.payerPhone}</b>. Admin-ka ayaa hadda eegaya si uu u ansixiyo.
                  </p>
                </div>
              )}

              {/* Action Button: Submit payment details to admin */}
              <button
                onClick={() => {
                  setSubPayerPhone(courier.paymentPhone || courier.phone);
                  setIsPaySubscriptionOpen(true);
                }}
                className="w-full py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center justify-center gap-1.5"
              >
                <DollarSign className="w-3.5 h-3.5" />
                <span>Geli Numberka aad ka dirtay lacagta bisha ($15)</span>
              </button>

              {/* Action Buttons to Confirm via WhatsApp or Call */}
              <div className="grid grid-cols-2 gap-2 pt-1">
                <a
                  href={`https://wa.me/252615000000?text=${encodeURIComponent(
                    `Asc Shiri Delivery, waxaan ahay darawalka ${courier.name} (${courier.phone}). Waxaan bixiyay khidmadda bisha ($15) ee barnaamijka. Nambarka aan ka soo diray waa ${courier.subscription?.payerPhone || courier.paymentPhone}. Fadlan i xaqiiji!`
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

            {/* Daily stats */}
            <div className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs space-y-3">
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-500">Today's Deliveries:</span>
                <span className="font-bold text-slate-900">{courier.completedDeliveriesToday} safar</span>
              </div>
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-500">Delivery Earnings (100% Yours):</span>
                <span className="font-mono font-bold text-emerald-600">${courier.todayEarnings.toFixed(2)}</span>
              </div>
              <div className="flex justify-between items-center text-xs">
                <span className="text-slate-500">Cash on Delivery Held:</span>
                <span className="font-mono font-bold text-amber-600">${courier.cashCollectedToday.toFixed(2)}</span>
              </div>
            </div>

            <button
              onClick={() => alert(`Payout request of $${courier.todayEarnings.toFixed(2)} sent to Hormuud EVC Plus (${courier.paymentPhone || courier.phone})`)}
              className="w-full py-3 bg-slate-900 text-white text-xs font-bold rounded-2xl shadow-md hover:bg-slate-800 transition-colors"
            >
              U Wareeji EVC Plus / Sahal (${courier.todayEarnings.toFixed(2)})
            </button>
          </div>
        )}
      </div>

      {/* Courier Bottom Tab Bar with 4 Tabs */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200 h-16 grid grid-cols-4 items-center max-w-[400px] mx-auto">
        {[
          { id: 'dashboard', label: 'Dashboard', icon: Bike },
          { id: 'incoming_orders', label: 'Dalabyada', icon: Inbox, badge: unassignedOrders.length > 0 ? `${unassignedOrders.length}` : undefined },
          { id: 'delivery', label: 'Navigation', icon: Navigation, badge: activeOrder ? '1' : undefined },
          { id: 'earnings', label: 'Dakhliga', icon: DollarSign },
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
                  <span className="absolute -top-1 -right-2 px-1 min-w-4 h-4 rounded-full bg-red-600 text-white text-[9px] font-bold flex items-center justify-center animate-pulse">
                    {tab.badge}
                  </span>
                )}
              </div>
              <span className="text-[10px] font-semibold mt-1">{tab.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Driver Contact & Manual Payment Profile Modal */}
      {isEditProfileOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-sm p-5 shadow-2xl border border-slate-100 flex flex-col space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <h3 className="text-sm font-bold text-slate-900">
                Beddel Xiriirka & Lacag-Bixinta Darawalka
              </h3>
              <button
                onClick={() => setIsEditProfileOpen(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveProfile} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka Lacag-Bixinta (EVC Plus / eDahab) *
                </label>
                <input
                  type="text"
                  required
                  value={editPaymentPhone}
                  onChange={(e) => setEditPaymentPhone(e.target.value)}
                  placeholder="061XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
                <span className="text-[10px] text-slate-400 mt-0.5 block">
                  Lambarka laguugu soo dirayo lacagaha iyo dakhligaaga
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Lambarka WhatsApp (Tooska ah) *
                </label>
                <input
                  type="text"
                  required
                  value={editWhatsapp}
                  onChange={(e) => setEditWhatsapp(e.target.value)}
                  placeholder="25261XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
                <span className="text-[10px] text-slate-400 mt-0.5 block">
                  Si maamulka iyo macaamiishu kuula xiriiraan adigoon lambar kaydin
                </span>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Taleefanka Caadiga ah ee Wacitaanka (Call) *
                </label>
                <input
                  type="text"
                  required
                  value={editCallPhone}
                  onChange={(e) => setEditCallPhone(e.target.value)}
                  placeholder="+252 61 XXXXXXX"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all"
              >
                Keydi Xiriirka (Save Contacts)
              </button>
            </form>
          </div>
        </div>
      )}
      {/* Pay Subscription Modal ($15 to Admin) */}
      {isPaySubscriptionOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-sm p-5 shadow-2xl border border-slate-100 flex flex-col space-y-4">
            <div className="flex items-center justify-between pb-2 border-b">
              <div>
                <h3 className="text-sm font-bold text-slate-900">
                  Bixi Khidmadda Bisha ee Darawalka ($15.00)
                </h3>
                <p className="text-[10px] text-slate-500">
                  0% Commission - Safarrada oo dhan 100% lacagtooda adaa leh
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
                <span className="bg-emerald-100 px-1.5 py-0.5 rounded text-[11px]">*712*615000000*15#</span>
              </div>
              <div className="flex justify-between items-center text-amber-800 font-bold">
                <span>eDahab:</span>
                <span className="bg-amber-100 px-1.5 py-0.5 rounded text-[11px]">*789*650000000*15#</span>
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
                  placeholder="e.g. 0613456789"
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
                  placeholder="e.g. EVC-83210 ama Magacaaga"
                  className="w-full px-3 py-2 rounded-xl border border-slate-300 font-mono outline-none focus:border-red-600"
                />
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-lg shadow-red-600/25 transition-all flex items-center justify-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Gudbi Lacagta Bisha ($15)</span>
              </button>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
