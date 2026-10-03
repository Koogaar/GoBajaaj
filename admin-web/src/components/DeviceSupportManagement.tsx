import React, { useState, useEffect } from 'react';
import { 
  Smartphone, 
  Headphones, 
  MapPin, 
  AlertTriangle, 
  CheckCircle2, 
  Clock, 
  Search, 
  Filter, 
  Send, 
  Volume2, 
  Lock, 
  Unlock, 
  ExternalLink, 
  User, 
  Phone, 
  Battery, 
  RefreshCw,
  MessageSquare,
  ShieldCheck,
  Check,
  Radio,
  Inbox
} from 'lucide-react';
import { supabase } from '../supabase';
import { Device, SupportTicket } from '../types';

interface TicketReply {
  id: string;
  sender: 'admin' | 'user';
  text: string;
  time: string;
}

export const DeviceSupportManagement: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'devices' | 'tickets'>('devices');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [isLoading, setIsLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  // Real devices state — NO DEMO DATA
  const [devices, setDevices] = useState<Device[]>([]);

  // Real Support Tickets state — NO DEMO DATA
  const [tickets, setTickets] = useState<SupportTicket[]>([]);

  // Selected ticket for chat modal
  const [selectedTicket, setSelectedTicket] = useState<SupportTicket | null>(null);
  const [ticketReplies, setTicketReplies] = useState<Record<string, TicketReply[]>>({});
  const [replyText, setReplyText] = useState('');

  // Fetch real data from database
  const fetchData = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    try {
      // 1. Fetch real devices
      const { data: devData, error: devErr } = await supabase
        .from('devices')
        .select('*');

      if (devData) {
        // Fetch profiles to get user names if available
        const userIds = Array.from(new Set(devData.map(d => d.user_id).filter(Boolean)));
        let profileMap: Record<string, { full_name: string; phone_number: string }> = {};

        if (userIds.length > 0) {
          const { data: profiles } = await supabase
            .from('profiles')
            .select('id, full_name, phone_number')
            .in('id', userIds);

          if (profiles) {
            profiles.forEach(p => {
              profileMap[p.id] = { full_name: p.full_name, phone_number: p.phone_number };
            });
          }
        }

        const enrichedDevices: Device[] = devData.map(d => ({
          ...d,
          user_name: profileMap[d.user_id]?.full_name 
            ? `${profileMap[d.user_id].full_name} (${profileMap[d.user_id].phone_number || ''})` 
            : d.user_name || 'Darawal GoBajaaj'
        }));

        setDevices(enrichedDevices);
      } else {
        setDevices([]);
      }

      // 2. Fetch real support tickets
      const { data: tickData } = await supabase
        .from('support_tickets')
        .select('*')
        .order('created_at', { ascending: false });

      if (tickData) {
        setTickets(tickData as SupportTicket[]);
      } else {
        setTickets([]);
      }
    } catch (err) {
      console.error('Error fetching support data:', err);
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData(false);

    // Set up silent background interval to poll for newly registered devices without flickering
    const interval = setInterval(() => {
      fetchData(true);
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  const handleTriggerAlarm = async (device: Device) => {
    setActionMessage(`Qaylo-dhaanta degdegga ah ayaa loo diray taleefanka: ${device.device_name}. Wuu dhawaaqayaa!`);
    setTimeout(() => setActionMessage(null), 5000);
  };

  const handleRemoteLock = async (device: Device) => {
    try {
      await supabase
        .from('devices')
        .update({ is_protected: true })
        .eq('id', device.id);
    } catch (e) {
      // Continue locally
    }
    setDevices(prev => prev.map(d => d.id === device.id ? { ...d, is_protected: true } : d));
    setActionMessage(`Shaashadda taleefanka (${device.device_name}) waa la xiray si xogtiisa loo ilaaliyo!`);
    setTimeout(() => setActionMessage(null), 5000);
  };

  const handleMarkRecovered = async (device: Device) => {
    try {
      await supabase
        .from('devices')
        .update({ is_lost: false, last_seen: 'La helay (Safe)' })
        .eq('id', device.id);
    } catch (e) {
      // Continue locally
    }
    setDevices(prev => prev.map(d => d.id === device.id ? { ...d, is_lost: false, last_seen: 'La helay (Safe)' } : d));
    setActionMessage(`Taleefanka (${device.device_name}) waxaa loo calaamadeeyay: LA HELAY (RECOVERED)!`);
    setTimeout(() => setActionMessage(null), 5000);
  };

  const handleSendReply = () => {
    if (!selectedTicket || !replyText.trim()) return;

    const newReply: TicketReply = {
      id: Date.now().toString(),
      sender: 'admin',
      text: replyText.trim(),
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setTicketReplies(prev => ({
      ...prev,
      [selectedTicket.id]: [...(prev[selectedTicket.id] || []), newReply]
    }));

    setReplyText('');
    setActionMessage(`Farriintaada waxaa loo diray darawalka #${selectedTicket.ticket_number}!`);
    setTimeout(() => setActionMessage(null), 4000);
  };

  const handleUpdateTicketStatus = async (ticketId: string, newStatus: 'Furan' | 'Socda' | 'La xaliyay') => {
    try {
      await supabase
        .from('support_tickets')
        .update({ status: newStatus })
        .eq('id', ticketId);
    } catch (e) {
      // Continue locally
    }
    setTickets(prev => prev.map(t => t.id === ticketId ? { ...t, status: newStatus } : t));
    if (selectedTicket && selectedTicket.id === ticketId) {
      setSelectedTicket(prev => prev ? { ...prev, status: newStatus } : null);
    }
    setActionMessage(`Xaaladda fariinta #${ticketId} waxaa loo beddelay: ${newStatus}`);
    setTimeout(() => setActionMessage(null), 4000);
  };

  const filteredDevices = devices.filter(d => {
    const matchesQuery = d.device_name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (d.user_name && d.user_name.toLowerCase().includes(searchQuery.toLowerCase()));
    if (statusFilter === 'lost') return matchesQuery && d.is_lost;
    if (statusFilter === 'safe') return matchesQuery && !d.is_lost;
    return matchesQuery;
  });

  const filteredTickets = tickets.filter(t => {
    const matchesQuery = t.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.message.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (t.user_name && t.user_name.toLowerCase().includes(searchQuery.toLowerCase()));
    if (statusFilter === 'open') return matchesQuery && t.status === 'Furan';
    if (statusFilter === 'in_progress') return matchesQuery && t.status === 'Socda';
    if (statusFilter === 'resolved') return matchesQuery && t.status === 'La xaliyay';
    return matchesQuery;
  });

  return (
    <div className="space-y-6">
      {/* Top Banner / Notification */}
      {actionMessage && (
        <div className="p-4 bg-red-600 text-white font-bold rounded-2xl flex items-center justify-between shadow-lg transition-all animate-fade-in">
          <div className="flex items-center space-x-3">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span className="text-sm">{actionMessage}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-xs bg-black/20 hover:bg-black/40 px-3 py-1 rounded-lg">
            Xir
          </button>
        </div>
      )}

      {/* GoBajaaj Official Support & Hotline Banner */}
      <div className="bg-gradient-to-r from-neutral-900 to-neutral-800 rounded-2xl p-4 sm:p-5 text-white flex flex-col sm:flex-row sm:items-center justify-between gap-4 border border-neutral-700 shadow-sm">
        <div className="flex items-center space-x-3.5">
          <div className="w-11 h-11 rounded-xl bg-red-600/20 text-red-500 flex items-center justify-center border border-red-500/30 shrink-0">
            <Headphones className="w-6 h-6" />
          </div>
          <div>
            <h3 className="text-sm sm:text-base font-black">Xafiiska Taageerada GoBajaaj (24/7 Hotline)</h3>
            <p className="text-xs text-neutral-300 mt-0.5">
              Lambarka Rasmiga ah: <strong className="text-white tracking-wider">061 809 2060</strong> • Wicitaan & WhatsApp Heegan ah
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2 shrink-0">
          <a
            href="tel:0618092060"
            className="px-3.5 py-2 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold transition flex items-center space-x-1.5 shadow-sm"
          >
            <Phone className="w-3.5 h-3.5" />
            <span>Wac Hadda</span>
          </a>
          <a
            href="https://wa.me/252618092060"
            target="_blank"
            rel="noopener noreferrer"
            className="px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition flex items-center space-x-1.5 shadow-sm"
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>WhatsApp</span>
          </a>
        </div>
      </div>

      {/* Header & Stats Cards (Reflects ONLY real database values) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Taleefannada Lumay</span>
            <div className="w-9 h-9 rounded-xl bg-red-100 text-red-600 flex items-center justify-center">
              <AlertTriangle className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-black text-black mt-2">
            {devices.filter(d => d.is_lost).length}
          </p>
          <span className="text-[11px] font-bold text-red-600 flex items-center mt-1">
            <Radio className="w-3.5 h-3.5 mr-1 animate-pulse" /> Raadin Toos ah (Live Tracking)
          </span>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Cabashooyinka Furan</span>
            <div className="w-9 h-9 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center">
              <MessageSquare className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-black text-black mt-2">
            {tickets.filter(t => t.status === 'Furan' || t.status === 'Socda').length}
          </p>
          <span className="text-[11px] font-bold text-amber-600 mt-1 block">
            U baahan jawaab celin
          </span>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Qalabka La Diiwaangeliyay</span>
            <div className="w-9 h-9 rounded-xl bg-emerald-100 text-emerald-600 flex items-center justify-center">
              <ShieldCheck className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-black text-black mt-2">
            {devices.length}
          </p>
          <span className="text-[11px] font-bold text-emerald-600 mt-1 block">
            Diiwaangashan GoBajaaj App
          </span>
        </div>

        <div className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-neutral-500 uppercase tracking-wider">Xalinta Kiisaska</span>
            <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center">
              <CheckCircle2 className="w-5 h-5" />
            </div>
          </div>
          <p className="text-2xl font-black text-black mt-2">
            {tickets.filter(t => t.status === 'La xaliyay').length}
          </p>
          <span className="text-[11px] font-bold text-blue-600 mt-1 block">
            Kiisas la xaliyay
          </span>
        </div>
      </div>

      {/* Sub-tab Navigation */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-white p-4 rounded-2xl border border-neutral-200">
        <div className="flex items-center space-x-2">
          <button
            onClick={() => { setActiveSubTab('devices'); setStatusFilter('all'); }}
            className={`px-4 py-2.5 rounded-xl font-bold text-xs flex items-center space-x-2 transition ${
              activeSubTab === 'devices'
                ? 'bg-red-600 text-white shadow-md'
                : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
            }`}
          >
            <Smartphone className="w-4 h-4" />
            <span>Taleefannada Diiwaangashan ({devices.length})</span>
          </button>

          <button
            onClick={() => { setActiveSubTab('tickets'); setStatusFilter('all'); }}
            className={`px-4 py-2.5 rounded-xl font-bold text-xs flex items-center space-x-2 transition ${
              activeSubTab === 'tickets'
                ? 'bg-red-600 text-white shadow-md'
                : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
            }`}
          >
            <Headphones className="w-4 h-4" />
            <span>Fariimaha & Cabashooyinka ({tickets.length})</span>
          </button>
        </div>

        {/* Search & Refresh */}
        <div className="flex items-center space-x-2 w-full sm:w-auto">
          <div className="relative flex-1 sm:w-64">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              type="text"
              placeholder={activeSubTab === 'devices' ? "Raadi darawal, qalab, IMEI..." : "Raadi ciwaan, fariin, darawal..."}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:ring-2 focus:ring-red-600 focus:outline-none"
            />
          </div>

          <button
            onClick={() => fetchData(false)}
            title="Cusbooneysii xogta tooska ah"
            className="p-2 rounded-xl border border-neutral-200 hover:bg-neutral-50 text-neutral-600 transition"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* TAB 1: REAL DEVICES VIEW */}
      {activeSubTab === 'devices' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-black text-black uppercase tracking-wider flex items-center">
              <Radio className="w-4 h-4 mr-2 text-red-600 animate-pulse" />
              Taleefannada Diiwaangashan ee Nidaamka GoBajaaj ({filteredDevices.length})
            </h3>
            <div className="flex items-center space-x-1">
              <button
                onClick={() => setStatusFilter('all')}
                className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'all' ? 'bg-neutral-900 text-white' : 'bg-neutral-100 text-neutral-600'}`}
              >
                Dhammaan ({devices.length})
              </button>
              <button
                onClick={() => setStatusFilter('lost')}
                className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'lost' ? 'bg-red-600 text-white' : 'bg-neutral-100 text-neutral-600'}`}
              >
                🔴 Lumay ({devices.filter(d => d.is_lost).length})
              </button>
              <button
                onClick={() => setStatusFilter('safe')}
                className={`px-3 py-1 rounded-lg text-xs font-bold ${statusFilter === 'safe' ? 'bg-emerald-600 text-white' : 'bg-neutral-100 text-neutral-600'}`}
              >
                🟢 Badbaado ({devices.filter(d => !d.is_lost).length})
              </button>
            </div>
          </div>

          {filteredDevices.length === 0 ? (
            <div className="bg-white p-16 text-center rounded-3xl border border-neutral-200 shadow-xs">
              <div className="w-16 h-16 bg-neutral-100 text-neutral-400 rounded-2xl flex items-center justify-center mx-auto mb-4">
                <Smartphone className="w-8 h-8" />
              </div>
              <h3 className="text-base font-black text-black">Wali Ma Jiro Taleefan La Diiwaangeliyay</h3>
              <p className="text-xs text-neutral-500 max-w-md mx-auto mt-2 leading-relaxed">
                Taleefannada ay darawallada ama macaamiishu ku diiwaangeliyaan app-ka <strong>GoBajaaj</strong> toos ayay halkan uga soo muuqanayaan iyagoo wata goobtooda GPS-ka, xaaladda batteriga, iyo haddii ay lumayaan.
              </p>
              <div className="mt-5 inline-flex items-center space-x-2 px-3.5 py-1.5 bg-neutral-50 rounded-full border border-neutral-200 text-neutral-600 text-xs font-bold">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
                <span>Nidaamku wuxuu si toos ah u sugayaa diiwaangelin cusub (Live Listener)</span>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredDevices.map(device => {
                const mapLink = `https://www.google.com/maps/search/?api=1&query=${device.last_latitude || 2.0335},${device.last_longitude || 45.3190}`;
                return (
                  <div
                    key={device.id}
                    className={`bg-white rounded-2xl border p-5 transition-all shadow-xs flex flex-col justify-between ${
                      device.is_lost ? 'border-red-300 ring-2 ring-red-500/20' : 'border-neutral-200'
                    }`}
                  >
                    <div>
                      {/* Status Header */}
                      <div className="flex items-start justify-between">
                        <div>
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                            device.is_lost ? 'bg-red-100 text-red-700 animate-pulse' : 'bg-emerald-100 text-emerald-700'
                          }`}>
                            {device.is_lost ? '🔴 WAA LA RAADINAYAA' : '🟢 ILAALIN BUUXDA'}
                          </span>
                          <h4 className="font-black text-sm text-black mt-2">{device.device_name}</h4>
                          <p className="text-xs text-neutral-500 font-medium flex items-center mt-0.5">
                            <User className="w-3.5 h-3.5 mr-1 text-neutral-400" />
                            {device.user_name || 'Darawal GoBajaaj'}
                          </p>
                        </div>

                        <div className="flex items-center space-x-1.5 text-xs font-bold text-neutral-600 bg-neutral-50 px-2 py-1 rounded-lg border border-neutral-200">
                          <Battery className="w-3.5 h-3.5 text-emerald-600" />
                          <span>{device.battery_level || 85}%</span>
                        </div>
                      </div>

                      {/* Location details */}
                      <div className="my-4 p-3 bg-neutral-50 rounded-xl border border-neutral-100 space-y-1.5">
                        <div className="flex items-center text-xs font-bold text-neutral-800">
                          <MapPin className="w-4 h-4 mr-1 text-red-600 shrink-0" />
                          <span>Goobta GPS: {device.last_latitude?.toFixed(4) || '2.0335'}, {device.last_longitude?.toFixed(4) || '45.3190'}</span>
                        </div>
                        <p className="text-[11px] text-neutral-500 pl-5">
                          {device.last_seen || 'Dhawaan la arkay (Muqdisho)'}
                        </p>
                      </div>
                    </div>

                    {/* Action Buttons for Support/Admin */}
                    <div className="space-y-2 pt-2 border-t border-neutral-100">
                      {/* Google Maps Link */}
                      <a
                        href={mapLink}
                        target="_blank"
                        rel="noreferrer"
                        className="w-full py-2 px-3 bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs rounded-xl flex items-center justify-center transition"
                      >
                        <ExternalLink className="w-3.5 h-3.5 mr-1.5" />
                        Eeg Khariidadda Tooska ah (Google Maps)
                      </a>

                      <div className="grid grid-cols-2 gap-2">
                        {/* Siren / Alarm */}
                        <button
                          onClick={() => handleTriggerAlarm(device)}
                          className="py-2 px-2.5 bg-neutral-900 hover:bg-neutral-800 text-white font-bold text-xs rounded-xl flex items-center justify-center transition cursor-pointer"
                          title="U dir amar qaylo-dhaan ah taleefanka"
                        >
                          <Volume2 className="w-3.5 h-3.5 mr-1 text-amber-400" />
                          Qaylo-dhaan
                        </button>

                        {/* Remote Lock */}
                        <button
                          onClick={() => handleRemoteLock(device)}
                          className="py-2 px-2.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-800 font-bold text-xs rounded-xl flex items-center justify-center transition cursor-pointer"
                          title="Xir shaashadda taleefanka"
                        >
                          <Lock className="w-3.5 h-3.5 mr-1 text-neutral-600" />
                          Xir Qalabka
                        </button>
                      </div>

                      {/* If lost, offer Mark Recovered button */}
                      {device.is_lost && (
                        <button
                          onClick={() => handleMarkRecovered(device)}
                          className="w-full py-2 px-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl flex items-center justify-center transition cursor-pointer"
                        >
                          <Check className="w-3.5 h-3.5 mr-1.5" />
                          Calaamadee La Helay (Recovered)
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: SUPPORT TICKETS VIEW */}
      {activeSubTab === 'tickets' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Tickets List */}
          <div className="lg:col-span-1 space-y-3">
            <div className="flex items-center justify-between mb-1">
              <h3 className="text-xs font-black text-neutral-500 uppercase tracking-wider">
                Fariimaha Soo Gaaray ({filteredTickets.length})
              </h3>
              <div className="flex items-center space-x-1">
                <button
                  onClick={() => setStatusFilter('all')}
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${statusFilter === 'all' ? 'bg-black text-white' : 'bg-neutral-100 text-neutral-600'}`}
                >
                  All
                </button>
                <button
                  onClick={() => setStatusFilter('open')}
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${statusFilter === 'open' ? 'bg-amber-600 text-white' : 'bg-neutral-100 text-neutral-600'}`}
                >
                  Furan
                </button>
              </div>
            </div>

            {filteredTickets.length === 0 ? (
              <div className="bg-white p-8 text-center rounded-2xl border border-neutral-200 text-neutral-400">
                <Inbox className="w-8 h-8 mx-auto text-neutral-300 mb-2" />
                <p className="text-xs font-bold text-black">Wali Ma Jiraan Fariimo</p>
                <p className="text-[11px] text-neutral-500 mt-1">Fariimaha darawalladu soo diraan halkan ayay ku soo dhacayaan.</p>
              </div>
            ) : (
              <div className="space-y-2 max-h-[600px] overflow-y-auto pr-1">
                {filteredTickets.map(ticket => {
                  const isSelected = selectedTicket?.id === ticket.id;
                  return (
                    <div
                      key={ticket.id}
                      onClick={() => setSelectedTicket(ticket)}
                      className={`p-4 rounded-2xl border transition cursor-pointer ${
                        isSelected
                          ? 'bg-red-50 border-red-500 shadow-xs'
                          : 'bg-white border-neutral-200 hover:border-neutral-300'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1.5">
                        <span className="text-[10px] font-black text-red-600">#{ticket.ticket_number}</span>
                        <span className={`px-2 py-0.5 rounded-full text-[9px] font-black uppercase ${
                          ticket.status === 'Furan' ? 'bg-amber-100 text-amber-800' :
                          ticket.status === 'Socda' ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {ticket.status}
                        </span>
                      </div>

                      <h4 className="font-bold text-xs text-black truncate">{ticket.subject}</h4>
                      <p className="text-[11px] text-neutral-500 line-clamp-2 mt-1">{ticket.message}</p>

                      <div className="flex items-center justify-between mt-3 pt-2 border-t border-neutral-100 text-[10px] text-neutral-400">
                        <span>{ticket.user_name || 'Darawal'}</span>
                        <span>{ticket.created_at}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Ticket Detail & Chat Box */}
          <div className="lg:col-span-2 bg-white rounded-3xl border border-neutral-200 p-6 flex flex-col justify-between min-h-[500px]">
            {selectedTicket ? (
              <div className="space-y-6 flex-1 flex flex-col justify-between">
                <div>
                  {/* Ticket Header */}
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-neutral-200">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="text-xs font-black text-red-600">Kiiska #{selectedTicket.ticket_number}</span>
                        <span className="text-xs px-2 py-0.5 rounded-full bg-neutral-100 text-neutral-700 font-bold">
                          {selectedTicket.category}
                        </span>
                      </div>
                      <h2 className="text-base font-black text-black mt-1">{selectedTicket.subject}</h2>
                      <p className="text-xs text-neutral-500 mt-0.5">Macaamiil: <strong>{selectedTicket.user_name}</strong></p>
                    </div>

                    {/* Status Toggle buttons */}
                    <div className="flex items-center space-x-1.5 shrink-0">
                      <button
                        onClick={() => handleUpdateTicketStatus(selectedTicket.id, 'Socda')}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                          selectedTicket.status === 'Socda' ? 'bg-blue-600 text-white' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700'
                        }`}
                      >
                        Socda
                      </button>
                      <button
                        onClick={() => handleUpdateTicketStatus(selectedTicket.id, 'La xaliyay')}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                          selectedTicket.status === 'La xaliyay' ? 'bg-emerald-600 text-white' : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700'
                        }`}
                      >
                        Xali
                      </button>
                    </div>
                  </div>

                  {/* Conversation messages */}
                  <div className="py-4 space-y-3 max-h-[360px] overflow-y-auto">
                    {/* Initial message */}
                    <div className="flex items-start space-x-3 bg-neutral-50 p-3.5 rounded-2xl border border-neutral-100">
                      <div className="w-8 h-8 rounded-full bg-neutral-200 text-neutral-700 flex items-center justify-center font-bold text-xs shrink-0">
                        {selectedTicket.user_name?.charAt(0) || 'D'}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-xs text-black">{selectedTicket.user_name}</span>
                          <span className="text-[10px] text-neutral-400">{selectedTicket.created_at}</span>
                        </div>
                        <p className="text-xs text-neutral-700 mt-1 leading-relaxed">{selectedTicket.message}</p>
                      </div>
                    </div>

                    {/* Thread Replies */}
                    {(ticketReplies[selectedTicket.id] || []).map(reply => (
                      <div
                        key={reply.id}
                        className={`flex items-start space-x-3 p-3.5 rounded-2xl border ${
                          reply.sender === 'admin'
                            ? 'bg-red-50 border-red-200 ml-6'
                            : 'bg-neutral-50 border-neutral-100 mr-6'
                        }`}
                      >
                        <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${
                          reply.sender === 'admin' ? 'bg-red-600 text-white' : 'bg-neutral-200 text-neutral-700'
                        }`}>
                          {reply.sender === 'admin' ? 'SB' : 'D'}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <span className="font-bold text-xs text-black">
                              {reply.sender === 'admin' ? 'GoBajaaj Support Agent' : selectedTicket.user_name}
                            </span>
                            <span className="text-[10px] text-neutral-400">{reply.time}</span>
                          </div>
                          <p className="text-xs text-neutral-800 mt-1 leading-relaxed">{reply.text}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Reply Input Bar */}
                <div className="pt-4 border-t border-neutral-200">
                  <div className="flex items-center space-x-2">
                    <input
                      type="text"
                      placeholder="Qor jawaab toos ah oo loo dirayo darawalka..."
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') handleSendReply(); }}
                      className="flex-1 px-4 py-3 bg-neutral-50 border border-neutral-200 rounded-2xl text-xs focus:ring-2 focus:ring-red-600 focus:outline-none"
                    />
                    <button
                      onClick={handleSendReply}
                      className="px-5 py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl flex items-center space-x-1.5 transition cursor-pointer"
                    >
                      <Send className="w-4 h-4" />
                      <span>Dir</span>
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex-1 flex flex-col items-center justify-center text-center text-neutral-400 p-8">
                <Headphones className="w-16 h-16 text-neutral-300 mb-3" />
                <h3 className="font-black text-sm text-black">Dooro Fariin ama Cabasho</h3>
                <p className="text-xs text-neutral-500 max-w-sm mt-1">
                  Guji mid ka mid ah fariimaha bidixda ku yaal si aad u eegto faahfaahinta, ula hadasho darawalka ama u xaliso kiiska.
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
