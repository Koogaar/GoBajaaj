import React, { useState } from 'react';
import { ShiriAddress, PaymentMethod, Order, VoiceNote } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { buildGoogleMapsViewUrl } from '../../utils/coordinateParser';
import { VoiceNoteRecorder } from '../common/VoiceNoteRecorder';
import {
  X,
  MapPin,
  CheckCircle2,
  ChevronRight,
  ShieldCheck,
  ArrowRight,
  Loader2,
  ExternalLink,
  Phone,
  MessageCircle,
  Copy,
  Check,
  Smartphone,
  Info,
} from 'lucide-react';

interface CheckoutModalProps {
  isOpen: boolean;
  onClose: () => void;
  merchantId: string;
  onOrderPlaced: (order: Order) => void;
  onOpenAddressManager: () => void;
}

export const CheckoutModal: React.FC<CheckoutModalProps> = ({
  isOpen,
  onClose,
  merchantId,
  onOrderPlaced,
  onOpenAddressManager,
}) => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  const addresses = platformStore.getAddresses();
  const cart = platformStore.getCart();
  const merchant = platformStore.getMerchants().find((m) => m.id === merchantId);

  const [selectedAddressId, setSelectedAddressId] = useState<string>(addresses[0]?.id || '');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('evc_plus');
  const [payerPhone, setPayerPhone] = useState<string>('0618890011');
  const [paymentReference, setPaymentReference] = useState<string>('');
  const [manualPaymentNote, setManualPaymentNote] = useState<string>('');
  const [instructions, setInstructions] = useState<string>('Agta masaajidka, wac markaad soo gaarto');
  const [customerWhatsapp, setCustomerWhatsapp] = useState<string>('252618890011');
  const [customerCallPhone, setCustomerCallPhone] = useState<string>('+252 61 889 0011');
  const [voiceNote, setVoiceNote] = useState<VoiceNote | null>(null);
  const [copiedUssd, setCopiedUssd] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  if (!isOpen || !merchant) return null;

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId) || addresses[0];

  const subtotal = cart.reduce((sum, item) => {
    const optMod = item.selectedOption?.priceModifier || 0;
    return sum + (item.product.price + optMod) * item.quantity;
  }, 0);

  const deliveryFee = merchant.deliveryFee || 2.0;
  const serviceFee = 0.5;
  const total = subtotal + deliveryFee + serviceFee;

  // Determine USSD shortcode string for Somali telecom providers
  const merchantPhoneClean = (merchant.paymentPhone || merchant.phone || '0615112233').replace(/\D/g, '');
  let ussdString = '';
  if (paymentMethod === 'evc_plus') {
    ussdString = `*712*${merchantPhoneClean}*${total.toFixed(2)}#`;
  } else if (paymentMethod === 'edahab') {
    ussdString = `*789*${merchantPhoneClean}*${total.toFixed(2)}#`;
  } else if (paymentMethod === 'zaad') {
    ussdString = `*880*${merchantPhoneClean}*${total.toFixed(2)}#`;
  } else if (paymentMethod === 'sahal') {
    ussdString = `*550*${merchantPhoneClean}*${total.toFixed(2)}#`;
  }

  const handleCopyUssd = () => {
    if (!ussdString) return;
    navigator.clipboard.writeText(ussdString);
    setCopiedUssd(true);
    setTimeout(() => setCopiedUssd(false), 2000);
  };

  const handlePlaceOrder = async () => {
    if (!selectedAddress) {
      alert('Fadlan dooro ama sameyso Shiri Address');
      return;
    }

    if (paymentMethod !== 'cash_on_delivery' && !payerPhone.trim()) {
      alert('Fadlan geli numberka aad lacagta ka dirtay');
      return;
    }

    setIsSubmitting(true);
    try {
      const order = await platformStore.placeOrder({
        merchantId: merchant.id,
        deliveryAddress: selectedAddress,
        paymentMethod,
        payerPhone: payerPhone.trim() || undefined,
        paymentReference: paymentReference.trim() || undefined,
        manualPaymentProofNote: manualPaymentNote.trim() || undefined,
        deliveryInstructions: instructions,
        customerWhatsapp,
        customerCallPhone,
        voiceNote: voiceNote || undefined,
      });

      setIsSubmitting(false);
      onOrderPlaced(order);
      onClose();
    } catch (err: any) {
      setIsSubmitting(false);
      alert(err.message || 'Khalad ayaa dhacay');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-2.5 sm:p-4 bg-black/70 backdrop-blur-xs animate-in fade-in">
      <div className="bg-white rounded-3xl w-full max-w-md max-h-[94vh] overflow-y-auto shadow-2xl border border-slate-100 flex flex-col">
        {/* Header */}
        <div className="sticky top-0 bg-white/95 backdrop-blur-md z-20 px-4 py-3 border-b border-slate-100 flex items-center justify-between">
          <div className="min-w-0 flex-1">
            <h2 className="font-bold text-sm sm:text-base text-slate-900 truncate">
              Checkout & Dalbo (1-Step)
            </h2>
            <p className="text-[11px] text-slate-500 truncate">
              {merchant.name} · {cart.length} alaab
            </p>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center transition-colors shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 space-y-4 flex-1">
          {/* Section 1: Delivery Address (Shiri Address) */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-red-600" />
                <span>{t.deliveryAddress}</span>
              </label>
              <button
                type="button"
                onClick={onOpenAddressManager}
                className="text-xs font-bold text-red-600 hover:text-red-700 bg-red-50 hover:bg-red-100 px-2 py-0.5 rounded-lg transition-colors"
              >
                + Ciwaan Cusub
              </button>
            </div>

            {/* Address Selector list */}
            <div className="space-y-1.5">
              {addresses.map((addr) => {
                const isSelected = addr.id === selectedAddressId;
                return (
                  <div
                    key={addr.id}
                    onClick={() => setSelectedAddressId(addr.id)}
                    className={`p-3 rounded-2xl border cursor-pointer transition-all flex items-start gap-2.5 ${
                      isSelected
                        ? 'border-red-600 bg-red-50/40 ring-1 ring-red-600 shadow-2xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded-full mt-0.5 border flex items-center justify-center shrink-0 ${
                        isSelected
                          ? 'border-red-600 bg-red-600 text-white'
                          : 'border-slate-300 bg-white'
                      }`}
                    >
                      {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1">
                        <span className="text-xs font-bold text-slate-900 truncate">
                          {addr.label} · <span className="font-mono text-red-600">{addr.addressCode}</span>
                        </span>
                        <span className="text-[10px] bg-slate-100 text-slate-700 px-1.5 py-0.5 rounded font-bold shrink-0">
                          {addr.district}
                        </span>
                      </div>
                      <p className="text-[11px] text-slate-600 truncate mt-0.5">
                        {addr.street || 'Waddo'}, {addr.buildingOrFloor} · {addr.landmark}
                      </p>
                      <div className="flex items-center justify-between text-[10px] text-slate-400 font-mono mt-1 pt-1 border-t border-slate-100">
                        <span>GPS: {addr.coordinates.lat.toFixed(5)}, {addr.coordinates.lng.toFixed(5)}</span>
                        {isSelected && (
                          <a
                            href={buildGoogleMapsViewUrl(addr.coordinates, addr.addressCode)}
                            target="_blank"
                            rel="noopener noreferrer"
                            onClick={(e) => e.stopPropagation()}
                            className="text-emerald-600 hover:underline flex items-center gap-0.5 font-semibold shrink-0"
                          >
                            <ExternalLink className="w-2.5 h-2.5" />
                            <span>Verify Map</span>
                          </a>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 2: Voice Landmark Note Recorder */}
          <div>
            <VoiceNoteRecorder
              onVoiceNoteRecorded={(vn) => setVoiceNote(vn)}
              existingVoiceNote={voiceNote || undefined}
            />
          </div>

          {/* Section 3: Delivery Text Instructions */}
          <div>
            <label className="block text-xs font-bold text-slate-900 mb-1">
              {t.deliveryInstructions}
            </label>
            <input
              type="text"
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              placeholder={t.deliveryInstructionsPlaceholder}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 bg-white font-medium text-slate-800 outline-none focus:border-red-600 focus:ring-1 focus:ring-red-200"
            />
          </div>

          {/* Section 4: Payment Method (Telecom Providers) */}
          <div className="space-y-2">
            <label className="block text-xs font-bold text-slate-900 uppercase tracking-wider">
              {t.paymentMethod}
            </label>
            <div className="grid grid-cols-2 gap-2">
              {[
                { id: 'evc_plus', name: 'EVC Plus', provider: 'Hormuud', icon: '📱' },
                { id: 'edahab', name: 'eDahab', provider: 'Somtel', icon: '💳' },
                { id: 'zaad', name: 'ZAAD', provider: 'Telesom', icon: '📶' },
                { id: 'sahal', name: 'Sahal', provider: 'Golis', icon: '📲' },
                { id: 'cash_on_delivery', name: 'Cash on Delivery', provider: 'Caddaan', icon: '💵' },
              ].map((opt) => {
                const isSelected = paymentMethod === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() => setPaymentMethod(opt.id as PaymentMethod)}
                    className={`p-2.5 rounded-2xl border text-left transition-all flex items-center gap-2 ${
                      isSelected
                        ? 'border-red-600 bg-red-50/50 ring-1 ring-red-600 shadow-2xs'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <span className="text-lg shrink-0">{opt.icon}</span>
                    <div className="min-w-0 flex-1">
                      <div className="text-xs font-bold text-slate-900 truncate">
                        {opt.name}
                      </div>
                      <div className="text-[10px] text-slate-500 truncate">
                        {opt.provider}
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>

            {/* USSD Dial & Payment Instructions */}
            <div className="p-3 rounded-2xl bg-amber-50/90 border border-amber-200 space-y-2.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-bold text-amber-950 flex items-center gap-1.5">
                  <Smartphone className="w-3.5 h-3.5 text-amber-700" />
                  <span>Xaqiijinta Lacag-Bixinta</span>
                </span>
                <span className="text-[10px] bg-amber-200 text-amber-900 px-2 py-0.5 rounded-full font-bold">
                  Sugaya Oggolaansho
                </span>
              </div>

              {/* Merchant Receiver Details & USSD Code */}
              {paymentMethod !== 'cash_on_delivery' ? (
                <div className="p-2.5 rounded-xl bg-white border border-amber-200 space-y-1.5 font-sans">
                  <div className="flex justify-between items-center text-[11px]">
                    <span className="text-slate-600">Meheradda:</span>
                    <span className="font-bold text-slate-900 truncate">{merchant.name}</span>
                  </div>
                  <div className="flex justify-between items-center text-[11px]">
                    <span className="text-slate-600">Merchant Account:</span>
                    <span className="font-mono font-bold text-red-600">{merchant.paymentPhone || merchant.phone}</span>
                  </div>

                  {ussdString && (
                    <div className="pt-1.5 border-t border-slate-100 flex items-center justify-between gap-1.5">
                      <span className="text-[10px] font-semibold text-slate-500 shrink-0">USSD:</span>
                      <span className="font-mono font-bold text-[11px] bg-amber-100 text-amber-900 px-2 py-0.5 rounded-md truncate">
                        {ussdString}
                      </span>
                      <button
                        type="button"
                        onClick={handleCopyUssd}
                        className="px-2 py-1 rounded-md bg-amber-600 hover:bg-amber-700 text-white text-[10px] font-bold shrink-0 flex items-center gap-1 transition-colors"
                        title="Koobiye USSD"
                      >
                        {copiedUssd ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                        <span>{copiedUssd ? 'Koodhka la qaaday' : 'Koobiye'}</span>
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-2.5 rounded-xl bg-white border border-amber-200 text-[11px] text-slate-700">
                  💵 Waxaad lacagta caddaanka ah (${total.toFixed(2)}) siin doontaa darawalka markuu xirmada kuu keeno.
                </div>
              )}

              {/* Sender Phone (Numberka laga soo diray) */}
              {paymentMethod !== 'cash_on_delivery' && (
                <div className="space-y-1">
                  <label className="block text-xs font-black text-slate-900">
                    Geli numberka aad lacagta ka dirtay *
                  </label>
                  <input
                    type="tel"
                    required
                    value={payerPhone}
                    onChange={(e) => setPayerPhone(e.target.value)}
                    placeholder="e.g. 0618890011"
                    className="w-full px-3 py-2 text-xs rounded-xl border-2 border-red-500 bg-white font-mono font-bold text-slate-900 outline-none focus:ring-2 focus:ring-red-200"
                  />
                  <p className="text-[10px] text-amber-800 leading-tight">
                    * Meheraddu waxay fiirin doontaa lambarkan si ay lacagta u approve garayso.
                  </p>
                </div>
              )}

              {/* Contact numbers for WhatsApp & Call */}
              <div className="grid grid-cols-2 gap-2 pt-1 border-t border-amber-200/60">
                <div>
                  <label className="block text-[10px] font-bold text-slate-700 mb-0.5">
                    WhatsApp:
                  </label>
                  <input
                    type="text"
                    value={customerWhatsapp}
                    onChange={(e) => setCustomerWhatsapp(e.target.value)}
                    placeholder="25261XXXXXXX"
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-amber-300 bg-white font-mono outline-none focus:border-red-600"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-bold text-slate-700 mb-0.5">
                    Wacitaanka (Call):
                  </label>
                  <input
                    type="text"
                    value={customerCallPhone}
                    onChange={(e) => setCustomerCallPhone(e.target.value)}
                    placeholder="+252 61..."
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-amber-300 bg-white font-mono outline-none focus:border-red-600"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Pricing Breakdown */}
          <div className="bg-slate-50 p-3.5 rounded-2xl border border-slate-200/80 space-y-1.5 text-xs">
            <div className="flex justify-between text-slate-600">
              <span>{t.subtotal} ({cart.length} alaab)</span>
              <span className="font-semibold text-slate-900 font-mono">${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>{t.deliveryFee}</span>
              <span className="font-semibold text-slate-900 font-mono">${deliveryFee.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-600">
              <span>{t.serviceFee}</span>
              <span className="font-semibold text-slate-900 font-mono">${serviceFee.toFixed(2)}</span>
            </div>
            <div className="pt-2 border-t border-slate-200 flex justify-between items-center text-sm font-bold text-slate-900">
              <span>{t.total}</span>
              <span className="text-base text-red-600 font-mono">${total.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Footer Place Order Button */}
        <div className="p-3.5 border-t border-slate-100 bg-white">
          <button
            disabled={isSubmitting}
            onClick={handlePlaceOrder}
            className="w-full py-3.5 px-4 bg-red-600 hover:bg-red-700 active:scale-[0.98] text-white font-bold text-sm rounded-2xl shadow-lg shadow-red-600/25 transition-all flex items-center justify-center gap-2 disabled:opacity-60"
          >
            {isSubmitting ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Processing Payment...</span>
              </>
            ) : (
              <>
                <span>Xaqiiji & Dalbo Hadda · ${total.toFixed(2)}</span>
                <ArrowRight className="w-4 h-4" />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
