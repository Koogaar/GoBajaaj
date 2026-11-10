import React, { useState } from 'react';
import { Order } from '../../types';
import { platformStore } from '../../services/store';
import { translations } from '../../i18n/translations';
import { MapCanvas } from '../common/MapCanvas';
import { VoiceNotePlayer } from '../common/VoiceNotePlayer';
import { buildGoogleMapsViewUrl } from '../../utils/coordinateParser';
import { buildCustomerToDriverWhatsappUrl, generateSimulatedSms } from '../../utils/whatsappHelper';
import {
  Phone,
  MessageCircle,
  Clock,
  MapPin,
  CheckCircle2,
  ChevronLeft,
  ShieldCheck,
  KeyRound,
  ExternalLink,
  Layers,
  FileText,
  Compass,
  Smartphone,
  Share2,
} from 'lucide-react';

interface LiveTrackingViewProps {
  order: Order;
  onBack: () => void;
}

export const LiveTrackingView: React.FC<LiveTrackingViewProps> = ({ order, onBack }) => {
  const lang = platformStore.getLanguage();
  const t = translations[lang];

  // Map view mode: 'map' or 'text_low_data'
  const [mapMode, setMapMode] = useState<'map' | 'text_low_data'>('map');
  const [showSmsModal, setShowSmsModal] = useState<boolean>(false);
  const [chatOpen, setChatOpen] = useState<boolean>(false);
  const [chatMessages, setChatMessages] = useState<string[]>([
    'Salaamu calaykum! Waxaan soo qaaday dalabkaagii.',
    'Waddada Maka Al-Mukarama ayaan marayaa, 8 daqiiqo ayaan kugu joogaa.',
  ]);
  const [newMsg, setNewMsg] = useState('');

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMsg.trim()) return;
    setChatMessages((prev) => [...prev, `Customer: ${newMsg}`]);
    setNewMsg('');
  };

  // Status progression stages
  const stages = [
    { key: 'pending', label: 'La Diray (Placed)' },
    { key: 'accepted', label: 'Meherad Aqbashay' },
    { key: 'preparing', label: 'Diyaarinta Cuntada' },
    { key: 'ready_for_pickup', label: 'Waa Diyaar (Pickup)' },
    { key: 'courier_assigned', label: 'Darawal loo qoondeeyay' },
    { key: 'picked_up', label: 'Darawalku wuu qaaday' },
    { key: 'in_transit', label: 'Waddada ayuu kusoo jiraa' },
    { key: 'delivered', label: 'Gacanta loo geliyay' },
  ];

  const currentStageIndex = stages.findIndex((s) => s.key === order.status);
  const activeIndex = currentStageIndex === -1 ? 0 : currentStageIndex;

  const smsData = generateSimulatedSms(order);
  const whatsappUrl = buildCustomerToDriverWhatsappUrl(order);

  return (
    <div className="flex flex-col h-full bg-slate-50 relative">
      {/* Top Header */}
      <div className="sticky top-0 z-30 bg-white/95 backdrop-blur-md px-4 py-3 border-b border-slate-100 flex items-center justify-between shadow-2xs">
        <button
          onClick={onBack}
          className="p-1.5 rounded-full hover:bg-slate-100 text-slate-700 transition-colors"
          title="Dib u noqo"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <div className="text-center">
          <h2 className="text-xs sm:text-sm font-bold text-slate-900">{t.liveTracking}</h2>
          <span className="text-[11px] font-mono text-red-600 font-bold">{order.orderNumber}</span>
        </div>
        <button
          onClick={() => setShowSmsModal(true)}
          className="p-1.5 rounded-full hover:bg-slate-100 text-slate-600 transition-colors"
          title="Arag SMS Xaqiijinta"
        >
          <Smartphone className="w-4 h-4 text-slate-700" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-3.5 space-y-3.5">
        {/* Map / Low-Data Text Toggle Header */}
        <div className="flex items-center justify-between bg-white p-1 rounded-2xl border border-slate-200">
          <button
            onClick={() => setMapMode('map')}
            className={`flex-1 py-1.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              mapMode === 'map'
                ? 'bg-red-600 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            <Compass className="w-3.5 h-3.5" />
            <span>Khariidadda GPS</span>
          </button>
          <button
            onClick={() => setMapMode('text_low_data')}
            className={`flex-1 py-1.5 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 ${
              mapMode === 'text_low_data'
                ? 'bg-slate-900 text-emerald-300 shadow-2xs'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
            title="Aad ugu habboon internet-ka gaabiska ah"
          >
            <FileText className="w-3.5 h-3.5 text-emerald-400" />
            <span>Qoraalka Goobta (Low Data)</span>
          </button>
        </div>

        {/* View Content: Either Interactive Map OR Text Fallback Guide */}
        {mapMode === 'map' ? (
          <div className="rounded-3xl overflow-hidden shadow-md border border-slate-200">
            <MapCanvas
              mode="route"
              height="h-56"
              pickupCoords={order.merchantCoordinates}
              dropoffCoords={order.deliveryAddress.coordinates}
              courierCoords={order.courierCoordinates}
              pickupLabel={order.merchantName}
              dropoffLabel={order.deliveryAddress.addressCode}
              courierLabel={order.courierName || 'Assigned Courier'}
            />
          </div>
        ) : (
          /* Low-Bandwidth Text Turn-by-Turn Card */
          <div className="bg-slate-900 text-white rounded-3xl p-4 border border-slate-800 shadow-md space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-1.5 text-emerald-400 text-xs font-bold">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
                <span>Tilmaamaha Khariidadda ee Qoraalka ah (Text-Only)</span>
              </div>
              <span className="text-[10px] font-mono text-slate-400 bg-slate-800 px-2 py-0.5 rounded-full">
                Zero Data Mode
              </span>
            </div>

            <div className="space-y-2.5 text-xs">
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-red-600/80 text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                  1
                </span>
                <div>
                  <span className="font-bold text-slate-200">Ka soo ambabaxay:</span>
                  <p className="text-slate-400 text-[11px]">{order.merchantName} ({order.merchantDistrict})</p>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-blue-600/80 text-white flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                  2
                </span>
                <div>
                  <span className="font-bold text-slate-200">Waddada ugu dhow:</span>
                  <p className="text-slate-400 text-[11px]">Waddo Maka Al-Mukarama → U leexo {order.deliveryAddress.district}</p>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-amber-500/80 text-slate-950 flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                  3
                </span>
                <div>
                  <span className="font-bold text-amber-300">Calaamadda Shiri:</span>
                  <p className="text-slate-300 text-[11px]">{order.deliveryAddress.landmark}</p>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-emerald-500 text-slate-950 flex items-center justify-center text-[10px] font-bold shrink-0 mt-0.5">
                  4
                </span>
                <div>
                  <span className="font-bold text-emerald-400">Guri Number:</span>
                  <p className="text-slate-200 font-mono font-bold text-[11px]">{order.deliveryAddress.addressCode} · {order.deliveryAddress.street || 'Waddo'}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Courier Contact Card */}
        {order.courierName ? (
          <div className="bg-white p-3.5 rounded-3xl shadow-xs border border-slate-100 flex items-center justify-between gap-2.5">
            <div className="flex items-center gap-2.5 min-w-0 flex-1">
              <div className="w-11 h-11 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center text-xl shrink-0">
                🛵
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-xs sm:text-sm font-bold text-slate-900 leading-tight truncate">
                  {order.courierName}
                </h3>
                <p className="text-[11px] text-slate-500 truncate mt-0.5">
                  {order.courierVehicle} · ⭐ {order.courierRating || 4.8}
                </p>
                <div className="flex items-center gap-1 text-[11px] font-bold text-emerald-600 mt-0.5 truncate">
                  <Clock className="w-3 h-3 shrink-0" />
                  <span className="truncate">ETA ~ {order.estimatedArrivalMinutes || 8} min</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-1.5 shrink-0">
              <a
                href={`tel:${order.courierPhone}`}
                className="w-8 h-8 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center hover:bg-emerald-100 transition-colors shrink-0"
                title={t.callDriver}
              >
                <Phone className="w-3.5 h-3.5" />
              </a>

              <a
                href={whatsappUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center hover:bg-emerald-700 transition-colors shrink-0"
                title="WhatsApp Darawalka"
              >
                <MessageCircle className="w-3.5 h-3.5" />
              </a>

              <button
                onClick={() => setChatOpen(!chatOpen)}
                className="w-8 h-8 rounded-full bg-red-50 text-red-600 flex items-center justify-center hover:bg-red-100 transition-colors shrink-0"
                title="In-App Chat"
              >
                <Layers className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        ) : (
          <div className="bg-white p-3.5 rounded-3xl shadow-xs border border-slate-100 flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center text-sm animate-spin shrink-0">
              ⏳
            </div>
            <div className="min-w-0 flex-1">
              <h4 className="text-xs font-bold text-slate-900">Waiting for Courier Assignment</h4>
              <p className="text-[11px] text-slate-500 truncate">System is dispatching to nearest Mogadishu courier</p>
            </div>
          </div>
        )}

        {/* Customer Voice Landmark Note Player if recorded */}
        {order.voiceNote && (
          <VoiceNotePlayer voiceNote={order.voiceNote} />
        )}

        {/* Handover PIN Card */}
        <div className="bg-gradient-to-r from-slate-900 to-slate-800 text-white p-3.5 rounded-3xl shadow-md flex items-center justify-between gap-3">
          <div className="space-y-0.5 min-w-0 flex-1">
            <div className="flex items-center gap-1.5 text-[11px] text-amber-300 font-bold">
              <KeyRound className="w-3.5 h-3.5 text-amber-400 shrink-0" />
              <span>{t.handoverPin}</span>
            </div>
            <p className="text-[10px] text-slate-300 leading-tight">
              Sii PIN-kaan darawalka markuu xirmada kuu keeno.
            </p>
          </div>
          <div className="bg-amber-400 text-slate-950 font-mono font-black text-lg px-3 py-1 rounded-xl tracking-widest shadow-inner shrink-0">
            {order.handoverPin}
          </div>
        </div>

        {/* Payment Status Card */}
        <div className={`p-3.5 rounded-3xl border shadow-2xs space-y-2 ${
          order.paymentStatus === 'paid'
            ? 'bg-emerald-50/70 border-emerald-200 text-emerald-950'
            : 'bg-amber-50/80 border-amber-200 text-amber-950'
        }`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold flex items-center gap-1.5">
              <span>{order.paymentStatus === 'paid' ? '✅' : '⏳'}</span>
              <span>{order.paymentStatus === 'paid' ? 'Lacagta Waa La Oggolaaday' : 'Sugaya Oggolaanshaha Meheradda'}</span>
            </span>
            <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
              order.paymentStatus === 'paid'
                ? 'bg-emerald-200 text-emerald-900'
                : 'bg-amber-200 text-amber-900'
            }`}>
              {order.paymentStatus === 'paid' ? 'Approved' : 'Pending Approval'}
            </span>
          </div>

          <div className="text-[11px] space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-600">Numberka aad ka dirtay:</span>
              <span className="font-mono font-bold text-slate-900">{order.payerPhone || order.customerPhone}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-600">Habka / Wadarta:</span>
              <span className="font-mono font-bold text-slate-900">{order.paymentMethod.toUpperCase()} · ${order.total.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Progress Tracker Stepper */}
        <div className="bg-white p-4 rounded-3xl shadow-xs border border-slate-100">
          <h4 className="text-xs font-bold text-slate-900 mb-3">Xaaladda Dalabka (Progress)</h4>
          <div className="space-y-2.5">
            {stages.map((st, i) => {
              const isPastOrCurrent = i <= activeIndex;
              const isCurrent = i === activeIndex;

              return (
                <div key={st.key} className="flex items-center gap-3 text-xs">
                  <div
                    className={`w-5 h-5 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0 transition-colors ${
                      isCurrent
                        ? 'bg-red-600 text-white ring-4 ring-red-100'
                        : isPastOrCurrent
                        ? 'bg-emerald-600 text-white'
                        : 'bg-slate-100 text-slate-400'
                    }`}
                  >
                    {isPastOrCurrent ? '✓' : i + 1}
                  </div>
                  <span
                    className={`font-medium ${
                      isCurrent
                        ? 'text-red-600 font-bold'
                        : isPastOrCurrent
                        ? 'text-slate-800'
                        : 'text-slate-400'
                    }`}
                  >
                    {st.label}
                  </span>
                  {isCurrent && (
                    <span className="text-[10px] bg-red-100 text-red-700 px-2 py-0.5 rounded-full font-semibold ml-auto">
                      Hadda Socda
                    </span>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Confirmed Delivery Location & Google Maps Link */}
        <div className="bg-white p-3.5 rounded-3xl shadow-xs border border-slate-100 space-y-2 text-xs">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-1.5">
                <MapPin className="w-3.5 h-3.5 text-red-600 shrink-0" />
                <span className="font-bold text-slate-900 truncate">
                  {order.deliveryAddress.addressCode} · {order.deliveryAddress.district}
                </span>
              </div>
              <p className="text-[11px] text-slate-600 mt-0.5 truncate">
                {order.deliveryAddress.street || 'Waddo'}, {order.deliveryAddress.buildingOrFloor} · {order.deliveryAddress.landmark}
              </p>
            </div>

            <a
              href={buildGoogleMapsViewUrl(order.deliveryAddress.coordinates, order.deliveryAddress.addressCode)}
              target="_blank"
              rel="noopener noreferrer"
              className="px-2.5 py-1 bg-slate-900 hover:bg-slate-800 text-emerald-300 border border-slate-700 rounded-xl text-xs font-bold flex items-center gap-1 shrink-0 transition-colors"
            >
              <ExternalLink className="w-3 h-3 text-emerald-400" />
              <span>Maps</span>
            </a>
          </div>
        </div>

        {/* In-app Chat Drawer if open */}
        {chatOpen && (
          <div className="bg-white p-3.5 rounded-3xl shadow-sm border border-slate-100 space-y-3">
            <div className="flex items-center justify-between border-b pb-2">
              <span className="text-xs font-bold text-slate-900">Chat with {order.courierName}</span>
              <button onClick={() => setChatOpen(false)} className="text-slate-400 text-xs font-bold">Xir</button>
            </div>
            <div className="max-h-36 overflow-y-auto space-y-1.5 text-xs">
              {chatMessages.map((msg, idx) => (
                <div key={idx} className="p-2 rounded-xl bg-slate-100 text-slate-800">
                  {msg}
                </div>
              ))}
            </div>
            <form onSubmit={handleSendChat} className="flex gap-2">
              <input
                type="text"
                value={newMsg}
                onChange={(e) => setNewMsg(e.target.value)}
                placeholder="Qor farriintaada..."
                className="flex-1 px-3 py-1.5 text-xs rounded-xl border border-slate-200 outline-none"
              />
              <button type="submit" className="px-3 py-1.5 bg-red-600 text-white text-xs font-bold rounded-xl">
                Dir
              </button>
            </form>
          </div>
        )}
      </div>

      {/* Simulated SMS Dialog Modal */}
      {showSmsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/60 backdrop-blur-xs animate-in fade-in">
          <div className="bg-slate-900 text-white rounded-3xl w-full max-w-sm p-4 space-y-3 border border-slate-800 shadow-2xl">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <div className="flex items-center gap-1.5">
                <Smartphone className="w-4 h-4 text-emerald-400" />
                <h3 className="text-xs font-bold text-slate-100">Simulated Telecom & Shiri SMS</h3>
              </div>
              <button
                onClick={() => setShowSmsModal(false)}
                className="text-slate-400 hover:text-white text-xs font-bold"
              >
                ✕
              </button>
            </div>

            {/* EVC Plus Message */}
            <div className="bg-slate-950 p-3 rounded-2xl border border-emerald-900/60 space-y-1">
              <div className="flex justify-between items-center text-[10px] text-emerald-400 font-bold">
                <span>{smsData.telecomReceipt.sender}</span>
                <span>{smsData.telecomReceipt.time}</span>
              </div>
              <p className="text-[11px] text-slate-200 font-mono leading-relaxed">
                {smsData.telecomReceipt.body}
              </p>
            </div>

            {/* Shiri Delivery Message */}
            <div className="bg-slate-950 p-3 rounded-2xl border border-red-900/60 space-y-1">
              <div className="flex justify-between items-center text-[10px] text-red-400 font-bold">
                <span>{smsData.shiriSms.sender}</span>
                <span>{smsData.shiriSms.time}</span>
              </div>
              <p className="text-[11px] text-slate-200 leading-relaxed font-sans">
                {smsData.shiriSms.body}
              </p>
            </div>

            <button
              onClick={() => setShowSmsModal(false)}
              className="w-full py-2 bg-slate-800 hover:bg-slate-700 text-white text-xs font-bold rounded-xl transition-colors"
            >
              Faham (Close)
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
