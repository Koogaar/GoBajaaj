import React, { useState, useEffect } from 'react';
import { Merchant, Product, Order, ShiriAddress } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { ShiriLogo } from '../common/ShiriLogo';
import { PhoneInput } from '../common/PhoneInput';
import { ShiriAddressModal } from './ShiriAddressModal';
import { CheckoutModal } from './CheckoutModal';
import { LiveTrackingView } from './LiveTrackingView';
import {
  Search,
  MapPin,
  ShoppingBag,
  Clock,
  Star,
  ChevronRight,
  Plus,
  Minus,
  Check,
  User,
  History,
  Home,
  QrCode,
  ArrowRight,
  Bike,
  Sparkles,
  Phone,
  LogOut,
  Globe,
} from 'lucide-react';

export const CustomerApp: React.FC = () => {
  const [lang, setLang] = useState(platformStore.getLanguage());
  const t = translations[lang];

  // Auth / Onboarding state
  const [isOnboarding, setIsOnboarding] = useState<boolean>(false);
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(true);

  // App navigation tabs: 'home' | 'categories' | 'orders' | 'profile'
  const [currentTab, setCurrentTab] = useState<'home' | 'categories' | 'orders' | 'profile'>('home');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Selected merchant & product modals
  const [selectedMerchant, setSelectedMerchant] = useState<Merchant | null>(null);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [selectedVariantId, setSelectedVariantId] = useState<string | undefined>();

  // Cart & Checkout
  const [isCartOpen, setIsCartOpen] = useState<boolean>(false);
  const [isCheckoutOpen, setIsCheckoutOpen] = useState<boolean>(false);
  const [isAddressModalOpen, setIsAddressModalOpen] = useState<boolean>(false);

  // Live order tracking view
  const [trackingOrder, setTrackingOrder] = useState<Order | null>(null);

  // Store data subscriptions
  const [merchants, setMerchants] = useState<Merchant[]>(platformStore.getMerchants());
  const [cart, setCart] = useState(platformStore.getCart());
  const [orders, setOrders] = useState<Order[]>(platformStore.getOrders());
  const [addresses, setAddresses] = useState<ShiriAddress[]>(platformStore.getAddresses());

  useEffect(() => {
    const unsubscribe = platformStore.subscribe(() => {
      setMerchants([...platformStore.getMerchants()]);
      setCart([...platformStore.getCart()]);
      setOrders([...platformStore.getOrders()]);
      setAddresses([...platformStore.getAddresses()]);
      setLang(platformStore.getLanguage());
    });
    return unsubscribe;
  }, []);

  const activeOrder = orders.find((o) => ['pending', 'accepted', 'preparing', 'ready_for_pickup', 'courier_assigned', 'picked_up', 'in_transit'].includes(o.status));
  const defaultAddress = addresses[0];

  const cartTotalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
  const cartSubtotal = cart.reduce((sum, item) => {
    const optPrice = item.selectedOption?.priceModifier || 0;
    return sum + (item.product.price + optPrice) * item.quantity;
  }, 0);

  // Categories list matching design references
  const categories = [
    { id: 'All', name: 'All', nameSo: 'Dhammaan', icon: '✨' },
    { id: 'Food & Restaurant', name: 'Food', nameSo: 'Cunto', icon: '🍔' },
    { id: 'Grocery & Supermarket', name: 'Grocery', nameSo: 'Supermarket', icon: '🛒' },
    { id: 'Pharmacy & Health', name: 'Pharmacy', nameSo: 'Daawo', icon: '💊' },
    { id: 'Fashion & Lifestyle', name: 'Fashion', nameSo: 'Dharka', icon: '👕' },
    { id: 'Electronics & Phones', name: 'Tech', nameSo: 'Electronics', icon: '📱' },
    { id: 'Documents & Parcels', name: 'Parcels', nameSo: 'Waraaqo', icon: '📦' },
  ];

  const filteredMerchants = merchants.filter((m) => {
    const matchesCat = selectedCategory === 'All' || m.category === selectedCategory;
    const matchesSearch =
      m.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.district.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCat && matchesSearch;
  });

  // If viewing active live tracking full view
  if (trackingOrder) {
    return (
      <LiveTrackingView
        order={trackingOrder}
        onBack={() => setTrackingOrder(null)}
      />
    );
  }

  return (
    <div className="flex flex-col h-full bg-slate-50 relative">
      {/* Top Header Bar */}
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-md px-4 py-2.5 border-b border-slate-100 flex items-center justify-between shadow-2xs">
        {/* Shiri Address Selector */}
        <button
          onClick={() => setIsAddressModalOpen(true)}
          className="flex items-center gap-2 text-left group min-w-0 flex-1 mr-2"
        >
          <div className="w-8 h-8 rounded-full bg-red-50 text-red-600 flex items-center justify-center shrink-0">
            <MapPin className="w-4 h-4" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-1.5 text-[10px] text-slate-500 font-semibold uppercase tracking-wider truncate">
              <span>{t.currentLocation}</span>
              <span className="text-red-600 font-mono font-bold shrink-0">
                {defaultAddress?.addressCode || 'SHR-MG-48291'}
              </span>
            </div>
            <div className="text-xs font-bold text-slate-900 truncate flex items-center gap-1">
              <span className="truncate">{defaultAddress ? `${defaultAddress.district}, Mogadishu` : 'Hodan, Mogadishu'}</span>
              <ChevronRight className="w-3.5 h-3.5 text-slate-400 group-hover:translate-x-0.5 transition-transform shrink-0" />
            </div>
          </div>
        </button>

        {/* QR & Language buttons */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setIsAddressModalOpen(true)}
            className="p-2 rounded-xl text-slate-600 hover:bg-slate-100 transition-colors"
            title="Guri Number QR Code"
          >
            <QrCode className="w-4 h-4 text-slate-700" />
          </button>
          <button
            onClick={() => {
              const next = lang === 'so' ? 'en' : 'so';
              platformStore.setLanguage(next);
            }}
            className="px-2 py-1 rounded-lg bg-slate-100 text-slate-700 hover:bg-slate-200 text-[11px] font-bold transition-colors"
          >
            {lang === 'so' ? 'EN' : 'SO'}
          </button>
        </div>
      </header>

      {/* Main Content Area based on currentTab */}
      <div className="flex-1 overflow-y-auto pb-24">
        {currentTab === 'home' && (
          <div className="space-y-4 p-4">
            {/* Search Input */}
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t.searchPlaceholder}
                className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white border border-slate-200 text-xs font-medium text-slate-900 placeholder:text-slate-400 shadow-2xs outline-none focus:border-red-600 focus:ring-1 focus:ring-red-600"
              />
            </div>

            {/* Active Order Banner if an order is in flight */}
            {activeOrder && (
              <div
                onClick={() => setTrackingOrder(activeOrder)}
                className="group relative overflow-hidden rounded-2xl bg-gradient-to-r from-red-600 via-rose-600 to-red-700 text-white p-3.5 shadow-md shadow-red-600/20 cursor-pointer transition-all hover:shadow-lg active:scale-[0.99] border border-white/15 space-y-2.5"
              >
                {/* Top Row: Header Badge & Uncramped Action Button */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5 min-w-0">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0 animate-ping" />
                    <span className="text-[11px] font-bold text-red-100 uppercase tracking-wider shrink-0">
                      Dalab Hadda Socda
                    </span>
                    <span className="text-[10px] font-mono font-bold px-1.5 py-0.5 rounded-md bg-black/30 text-white/95 shrink-0 border border-white/10">
                      {activeOrder.orderNumber}
                    </span>
                  </div>

                  {/* Track Order CTA: Guaranteed single-line button */}
                  <span className="inline-flex items-center gap-1 bg-white text-red-600 group-hover:bg-red-50 text-xs font-bold px-2.5 py-1 rounded-xl shadow-xs whitespace-nowrap shrink-0 transition-colors">
                    <span>{t.trackOrder}</span>
                    <ChevronRight className="w-3.5 h-3.5 shrink-0" />
                  </span>
                </div>

                {/* Bottom Row: Courier / Progression Details */}
                <div className="flex items-center justify-between gap-2 pt-1 border-t border-white/15">
                  <div className="flex items-center gap-2 min-w-0">
                    <div className="w-7 h-7 rounded-lg bg-white/20 backdrop-blur-xs flex items-center justify-center text-sm shrink-0">
                      🛵
                    </div>
                    <span className="text-xs font-bold text-white truncate">
                      {activeOrder.status === 'in_transit'
                        ? 'Waddada ayuu kusoo jiraa'
                        : activeOrder.status === 'preparing'
                        ? 'Jikada ayaa diyaarinaysa'
                        : activeOrder.status === 'ready_for_pickup'
                        ? 'Waa diyaar, darawal sugaya'
                        : activeOrder.status === 'courier_assigned'
                        ? 'Darawal ayaa loo qoondeeyay'
                        : 'Dalabka waa la aqbalay'}
                    </span>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    {activeOrder.voiceNote && (
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-emerald-950/60 text-emerald-300 border border-emerald-500/40 flex items-center gap-1">
                        <span>🎙️</span>
                        <span>Cod</span>
                      </span>
                    )}
                    <span className="text-[11px] font-bold bg-white/20 px-2 py-0.5 rounded-lg text-white font-mono shrink-0">
                      ~{activeOrder.estimatedArrivalMinutes || 8} min
                    </span>
                  </div>
                </div>
              </div>
            )}

            {/* Promotional Banner Hero matching design references */}
            <div className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-red-600 to-rose-700 text-white p-5 shadow-md">
              <div className="max-w-[210px] space-y-1">
                <span className="text-[10px] font-bold uppercase tracking-wider text-red-200 bg-red-800/40 px-2 py-0.5 rounded-md">
                  Ku dhowaa, Ku gaara
                </span>
                <h3 className="text-lg font-black leading-tight pt-1">
                  {t.promotions}
                </h3>
                <p className="text-[11px] text-red-100">
                  {t.promotionsSub}
                </p>
                <button
                  onClick={() => setSelectedCategory('Food & Restaurant')}
                  className="mt-3 px-3.5 py-1.5 bg-white text-red-600 rounded-xl text-xs font-bold shadow-xs hover:bg-red-50 transition-colors"
                >
                  Dalbo Hadda
                </button>
              </div>

              {/* Delivery Motorbike Illustration */}
              <div className="absolute -right-3 bottom-1 w-32 h-32 flex items-center justify-center text-6xl opacity-90 select-none">
                🛵
              </div>
            </div>

            {/* Categories Carousel */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  {t.categoriesTitle}
                </h4>
                <button
                  onClick={() => setCurrentTab('categories')}
                  className="text-[11px] font-semibold text-red-600 hover:text-red-700"
                >
                  {t.viewAll}
                </button>
              </div>
              <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
                {categories.map((cat) => {
                  const isSelected = selectedCategory === cat.id;
                  return (
                    <button
                      key={cat.id}
                      onClick={() => setSelectedCategory(cat.id)}
                      className={`flex flex-col items-center justify-center min-w-[72px] p-2.5 rounded-2xl border transition-all shrink-0 ${
                        isSelected
                          ? 'bg-red-600 text-white border-red-600 shadow-sm'
                          : 'bg-white text-slate-700 border-slate-200 hover:border-slate-300'
                      }`}
                    >
                      <span className="text-xl mb-1">{cat.icon}</span>
                      <span className="text-[11px] font-bold whitespace-nowrap">
                        {lang === 'so' ? cat.nameSo : cat.name}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Popular Merchants in Mogadishu */}
            <div>
              <div className="flex items-center justify-between mb-2.5">
                <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  {t.popularMerchants} ({filteredMerchants.length})
                </h4>
                <span className="text-[11px] text-slate-500 font-medium">Mogadishu</span>
              </div>

              <div className="space-y-3">
                {filteredMerchants.map((merchant) => (
                  <div
                    key={merchant.id}
                    onClick={() => setSelectedMerchant(merchant)}
                    className="bg-white rounded-3xl p-3 border border-slate-200/80 hover:border-red-200 transition-all shadow-2xs hover:shadow-sm cursor-pointer flex gap-3 group"
                  >
                    <div className="w-20 h-20 rounded-2xl bg-slate-100 overflow-hidden shrink-0 relative">
                      <img
                        src={merchant.banner}
                        alt={merchant.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                      />
                      <div className="absolute top-1 left-1 w-6 h-6 rounded-lg bg-white/90 backdrop-blur-xs flex items-center justify-center text-xs shadow-xs">
                        {merchant.logo}
                      </div>
                    </div>

                    <div className="flex-1 min-w-0 flex flex-col justify-between py-0.5">
                      <div>
                        <div className="flex items-center justify-between">
                          <h5 className="text-sm font-bold text-slate-900 truncate">
                            {merchant.name}
                          </h5>
                          <span className="flex items-center gap-0.5 text-xs font-bold text-slate-800 shrink-0">
                            <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                            {merchant.rating}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-500 truncate mt-0.5">
                          {lang === 'so' ? merchant.categorySomali : merchant.category} · {merchant.district}
                        </p>
                      </div>

                      <div className="flex items-center gap-3 text-[11px] text-slate-600 font-medium pt-1">
                        <span className="flex items-center gap-1 text-slate-700">
                          <Clock className="w-3 h-3 text-red-600" />
                          {merchant.deliveryTimeMinutes} min
                        </span>
                        <span>•</span>
                        <span>Gaarsiin ${merchant.deliveryFee.toFixed(2)}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Tab: Categories */}
        {currentTab === 'categories' && (
          <div className="p-4 space-y-4">
            <h3 className="text-base font-bold text-slate-900">{t.categoriesTitle}</h3>
            <div className="grid grid-cols-2 gap-3">
              {categories.filter(c => c.id !== 'All').map((cat) => (
                <div
                  key={cat.id}
                  onClick={() => {
                    setSelectedCategory(cat.id);
                    setCurrentTab('home');
                  }}
                  className="bg-white p-4 rounded-3xl border border-slate-200/80 hover:border-red-600 shadow-2xs hover:shadow-md cursor-pointer transition-all flex flex-col items-center text-center group"
                >
                  <span className="text-3xl mb-2 group-hover:scale-110 transition-transform">{cat.icon}</span>
                  <span className="text-xs font-bold text-slate-900">{lang === 'so' ? cat.nameSo : cat.name}</span>
                  <span className="text-[10px] text-slate-500 mt-0.5">Mogadishu Stores</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab: Order History */}
        {currentTab === 'orders' && (
          <div className="p-4 space-y-3">
            <h3 className="text-base font-bold text-slate-900 mb-2">Taariikhda Dalabyada (Orders)</h3>
            {orders.length === 0 ? (
              <div className="text-center py-12 text-slate-400 text-xs">Ma jiraan dalabyo hore</div>
            ) : (
              orders.map((ord) => (
                <div
                  key={ord.id}
                  onClick={() => setTrackingOrder(ord)}
                  className="bg-white p-4 rounded-3xl border border-slate-200/80 shadow-2xs hover:shadow-sm cursor-pointer space-y-2 transition-all"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="min-w-0 flex-1">
                      <span className="text-xs font-bold text-slate-900 truncate block">{ord.merchantName}</span>
                      <div className="text-[10px] font-mono text-red-600 font-semibold truncate">{ord.orderNumber}</div>
                    </div>
                    <span
                      className={`text-[10px] font-bold px-2.5 py-1 rounded-full shrink-0 whitespace-nowrap ${
                        ord.status === 'delivered'
                          ? 'bg-emerald-100 text-emerald-800'
                          : ord.status === 'cancelled'
                          ? 'bg-rose-100 text-rose-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {ord.status}
                    </span>
                  </div>
                  <div className="text-xs text-slate-600">
                    {ord.items.map((i) => `${i.quantity}x ${i.name}`).join(', ')}
                  </div>
                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                    <span className="text-slate-500">{new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    <span className="font-bold font-mono text-slate-900">${ord.total.toFixed(2)}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* Tab: Profile */}
        {currentTab === 'profile' && (
          <div className="p-4 space-y-4">
            <div className="bg-white p-5 rounded-3xl border border-slate-200/80 shadow-2xs flex items-center gap-3">
              <div className="w-14 h-14 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xl font-bold">
                AM
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-900">Ahmed Mohamed</h3>
                <p className="text-xs font-mono text-slate-500">+252 61 234 5678</p>
                <span className="text-[10px] bg-emerald-100 text-emerald-800 font-bold px-2 py-0.5 rounded-full mt-1 inline-block">
                  Verified Customer
                </span>
              </div>
            </div>

            {/* Menu Links */}
            <div className="bg-white rounded-3xl border border-slate-200/80 shadow-2xs divide-y divide-slate-100 overflow-hidden text-xs">
              <button
                onClick={() => setIsAddressModalOpen(true)}
                className="w-full p-4 flex items-center justify-between text-slate-800 hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <MapPin className="w-4 h-4 text-red-600" />
                  <span className="font-semibold">{t.myAddresses}</span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => setCurrentTab('orders')}
                className="w-full p-4 flex items-center justify-between text-slate-800 hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <History className="w-4 h-4 text-slate-600" />
                  <span className="font-semibold">Dalabaadkaagii hore (Orders)</span>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-400" />
              </button>

              <button
                onClick={() => {
                  const next = lang === 'so' ? 'en' : 'so';
                  platformStore.setLanguage(next);
                }}
                className="w-full p-4 flex items-center justify-between text-slate-800 hover:bg-slate-50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <Globe className="w-4 h-4 text-blue-600" />
                  <span className="font-semibold">Luqadda (Language): {lang === 'so' ? 'Af-Soomaali' : 'English'}</span>
                </div>
                <span className="text-[10px] bg-slate-100 font-bold px-2 py-0.5 rounded">Switch</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Floating Cart Button if Cart Has Items */}
      {cart.length > 0 && !isCartOpen && (
        <div className="fixed bottom-20 left-4 right-4 z-40 max-w-[368px] mx-auto">
          <button
            onClick={() => setIsCartOpen(true)}
            className="w-full py-3.5 px-5 bg-red-600 hover:bg-red-700 active:scale-[0.98] text-white font-bold text-xs rounded-2xl shadow-xl shadow-red-600/30 flex items-center justify-between transition-all"
          >
            <div className="flex items-center gap-2">
              <span className="w-6 h-6 rounded-full bg-white text-red-600 flex items-center justify-center font-bold text-xs">
                {cartTotalCount}
              </span>
              <span>{t.yourCart}</span>
            </div>
            <span className="font-mono text-sm">${cartSubtotal.toFixed(2)}</span>
          </button>
        </div>
      )}

      {/* Bottom Navigation Tab Bar (Pattern 1 from Mobile Design Spec) */}
      <nav className="fixed bottom-0 left-0 right-0 z-30 bg-white/95 backdrop-blur-md border-t border-slate-200 h-16 grid grid-cols-4 items-center max-w-[400px] mx-auto">
        {[
          { id: 'home', label: 'Home', icon: Home },
          { id: 'categories', label: 'Explore', icon: Search },
          { id: 'orders', label: 'Orders', icon: History, badge: activeOrder ? '1' : undefined },
          { id: 'profile', label: 'Profile', icon: User },
        ].map((tab) => {
          const isActive = currentTab === tab.id;
          const IconComponent = tab.icon;
          return (
            <button
              key={tab.id}
              onClick={() => setCurrentTab(tab.id as any)}
              className={`flex flex-col items-center justify-center h-full relative transition-colors ${
                isActive ? 'text-red-600' : 'text-slate-500 hover:text-slate-800'
              }`}
            >
              <div className="relative">
                <IconComponent className="w-5 h-5" />
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

      {/* Merchant Detail Modal */}
      {selectedMerchant && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-2 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-md max-h-[92vh] overflow-y-auto shadow-2xl flex flex-col">
            {/* Banner & Close */}
            <div className="relative h-44 bg-slate-900 shrink-0">
              <img
                src={selectedMerchant.banner}
                alt={selectedMerchant.name}
                className="w-full h-full object-cover opacity-85"
              />
              <button
                onClick={() => setSelectedMerchant(null)}
                className="absolute top-3 right-3 w-8 h-8 rounded-full bg-black/50 text-white flex items-center justify-center hover:bg-black/70 transition-colors"
              >
                ✕
              </button>
              <div className="absolute bottom-3 left-4 right-4 text-white">
                <span className="text-2xl mr-2">{selectedMerchant.logo}</span>
                <h3 className="text-lg font-bold leading-tight">{selectedMerchant.name}</h3>
                <p className="text-[11px] text-slate-200 mt-0.5">{selectedMerchant.slogan}</p>
              </div>
            </div>

            {/* Menu Products list */}
            <div className="p-4 space-y-3 flex-1">
              <div className="flex items-center justify-between text-xs text-slate-600 border-b pb-2">
                <span>⭐ {selectedMerchant.rating} ({selectedMerchant.reviewCount} reviews)</span>
                <span>⏱️ {selectedMerchant.deliveryTimeMinutes} min</span>
                <span>🛵 ${selectedMerchant.deliveryFee.toFixed(2)} delivery</span>
              </div>

              <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider pt-1">
                Liiska Cuntada / Alaabta ({selectedMerchant.products.length})
              </h4>

              <div className="space-y-2.5">
                {selectedMerchant.products.map((prod) => (
                  <div
                    key={prod.id}
                    className="p-3 rounded-2xl border border-slate-200 hover:border-red-200 flex gap-3 transition-colors"
                  >
                    <img
                      src={prod.imageUrl}
                      alt={prod.name}
                      className="w-16 h-16 rounded-xl object-cover shrink-0"
                    />
                    <div className="flex-1 min-w-0 flex flex-col justify-between">
                      <div>
                        <div className="flex justify-between items-start">
                          <h5 className="text-xs font-bold text-slate-900 truncate">
                            {lang === 'so' ? prod.nameSomali : prod.name}
                          </h5>
                          <span className="text-xs font-mono font-bold text-slate-900 ml-1">
                            ${prod.price.toFixed(2)}
                          </span>
                        </div>
                        <p className="text-[10px] text-slate-500 line-clamp-2 mt-0.5">
                          {lang === 'so' ? prod.descriptionSomali : prod.description}
                        </p>
                      </div>

                      <div className="flex justify-end pt-1">
                        <button
                          onClick={() => {
                            platformStore.addToCart(prod, 1);
                          }}
                          className="px-3 py-1 bg-red-600 hover:bg-red-700 text-white text-[11px] font-bold rounded-xl flex items-center gap-1 shadow-xs"
                        >
                          <Plus className="w-3 h-3" />
                          <span>Gali Gaariga</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="p-3 border-t bg-slate-50 flex justify-between items-center">
              <span className="text-xs text-slate-600">Cart Total: <b>${cartSubtotal.toFixed(2)}</b></span>
              <button
                onClick={() => {
                  setSelectedMerchant(null);
                  setIsCartOpen(true);
                }}
                className="px-4 py-2 bg-red-600 text-white text-xs font-bold rounded-xl shadow-xs"
              >
                Arag Gaariga ({cartTotalCount})
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cart Drawer */}
      {isCartOpen && (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-t-3xl sm:rounded-3xl w-full max-w-md max-h-[85vh] overflow-y-auto p-5 shadow-2xl flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b">
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                <ShoppingBag className="w-5 h-5 text-red-600" />
                <span>{t.yourCart} ({cartTotalCount})</span>
              </h3>
              <button onClick={() => setIsCartOpen(false)} className="text-slate-400 hover:text-slate-600 text-sm font-bold">
                ✕
              </button>
            </div>

            {cart.length === 0 ? (
              <div className="py-12 text-center text-xs text-slate-400">
                {t.emptyCartDesc}
              </div>
            ) : (
              <div className="py-3 space-y-3 flex-1">
                {cart.map((item, idx) => (
                  <div key={idx} className="flex items-center justify-between text-xs py-2 border-b border-slate-100">
                    <div className="min-w-0 pr-2">
                      <div className="font-bold text-slate-900">{item.product.name}</div>
                      <div className="text-[11px] text-slate-500 font-mono">${item.product.price.toFixed(2)} each</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => platformStore.updateCartQuantity(idx, item.quantity - 1)}
                        className="w-6 h-6 rounded-lg bg-slate-100 flex items-center justify-center text-slate-700 font-bold hover:bg-slate-200"
                      >
                        -
                      </button>
                      <span className="font-bold text-xs">{item.quantity}</span>
                      <button
                        onClick={() => platformStore.updateCartQuantity(idx, item.quantity + 1)}
                        className="w-6 h-6 rounded-lg bg-slate-100 flex items-center justify-center text-slate-700 font-bold hover:bg-slate-200"
                      >
                        +
                      </button>
                    </div>
                  </div>
                ))}

                <div className="pt-2 text-xs space-y-1.5">
                  <div className="flex justify-between text-slate-600">
                    <span>{t.subtotal}</span>
                    <span className="font-mono font-semibold">${cartSubtotal.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>{t.deliveryFee}</span>
                    <span className="font-mono font-semibold">$2.00</span>
                  </div>
                  <div className="flex justify-between text-slate-600">
                    <span>{t.serviceFee}</span>
                    <span className="font-mono font-semibold">$0.50</span>
                  </div>
                  <div className="flex justify-between text-sm font-bold text-slate-900 pt-2 border-t">
                    <span>{t.total}</span>
                    <span className="text-red-600 font-mono">${(cartSubtotal + 2.5).toFixed(2)}</span>
                  </div>
                </div>

                <button
                  onClick={() => {
                    setIsCartOpen(false);
                    setIsCheckoutOpen(true);
                  }}
                  className="w-full mt-3 py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-xl shadow-lg shadow-red-600/25 flex items-center justify-center gap-1.5"
                >
                  <span>Gudub Checkout-ka</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Checkout Modal */}
      {isCheckoutOpen && (
        <CheckoutModal
          isOpen={isCheckoutOpen}
          merchantId={cart[0]?.product.merchantId || merchants[0]?.id}
          onClose={() => setIsCheckoutOpen(false)}
          onOrderPlaced={(order) => {
            setTrackingOrder(order);
          }}
          onOpenAddressManager={() => setIsAddressModalOpen(true)}
        />
      )}

      {/* Shiri Address Modal */}
      <ShiriAddressModal
        isOpen={isAddressModalOpen}
        onClose={() => setIsAddressModalOpen(false)}
      />
    </div>
  );
};
