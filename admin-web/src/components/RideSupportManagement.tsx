import React, { useState, useEffect } from 'react';
import { 
  Bike, 
  Shield, 
  MessageSquare, 
  User, 
  CheckCircle, 
  Clock, 
  AlertCircle, 
  Search, 
  Phone, 
  MapPin, 
  Send,
  Check,
  X,
  RefreshCw
} from 'lucide-react';
import { supabase } from '../supabase';

interface RideRequest {
  id: string;
  passenger_name: string;
  passenger_phone: string;
  pickup_location: string;
  destination: string;
  status: 'pending' | 'accepted' | 'completed' | 'cancelled';
  driver_id?: string;
  driver_name?: string;
  created_at: string;
}

interface SupportTicket {
  id: string;
  user_phone: string;
  subject: string;
  message: string;
  status: 'open' | 'in_progress' | 'resolved';
  admin_reply?: string;
  created_at: string;
}

export function RideSupportManagement() {
  const [activeTab, setActiveTab] = useState<'rides' | 'support'>('rides');
  const [rides, setRides] = useState<RideRequest[]>([]);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTicket, setSelectedTicket] = useState<SupportTicket | null>(null);
  const [replyText, setReplyText] = useState('');
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    try {
      const { data: rData, error: rErr } = await supabase.from('bajaj_rides').select('*').order('created_at', { ascending: false });
      if (!rErr && rData) {
        setRides(rData as RideRequest[]);
      } else {
        setRides([]);
      }

      const { data: tData, error: tErr } = await supabase.from('support_tickets').select('*').order('created_at', { ascending: false });
      if (!tErr && tData) {
        setTickets(tData as SupportTicket[]);
      } else {
        setTickets([]);
      }
    } catch (e) {
      setRides([]);
      setTickets([]);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleUpdateRideStatus = async (rideId: string, newStatus: 'accepted' | 'completed' | 'cancelled') => {
    setRides(prev => prev.map(r => r.id === rideId ? { ...r, status: newStatus } : r));
    setSuccessMsg('Xaaladda dalabka waa la beddelay.');
    setTimeout(() => setSuccessMsg(null), 3000);
    try {
      await supabase.from('bajaj_rides').update({ status: newStatus }).eq('id', rideId);
    } catch (err) {
      // fallback
    }
  };

  const handleSendReply = async () => {
    if (!selectedTicket || !replyText.trim()) return;
    const updatedTickets = tickets.map(t => t.id === selectedTicket.id ? { ...t, status: 'resolved' as const, admin_reply: replyText } : t);
    setTickets(updatedTickets);
    setSelectedTicket(null);
    setReplyText('');
    setSuccessMsg('Cabashada waa laga jawaabay oo waa la xiray.');
    setTimeout(() => setSuccessMsg(null), 3000);
    try {
      await supabase.from('support_tickets').update({ status: 'resolved', admin_reply: replyText }).eq('id', selectedTicket.id);
    } catch (err) {
      // fallback
    }
  };

  const filteredRides = rides.filter(r => 
    r.passenger_name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    r.passenger_phone.includes(searchQuery) ||
    r.pickup_location.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredTickets = tickets.filter(t => 
    t.user_phone.includes(searchQuery) ||
    t.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.message.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Header & Tabs */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-neutral-100">
        <div>
          <h1 className="text-xl font-bold text-neutral-900 flex items-center gap-2">
            <Bike className="w-6 h-6 text-rose-600" />
            Maamulka GoBajaaj Ride & Cabashooyinka
          </h1>
          <p className="text-sm text-neutral-500 mt-1">
            Kormeerka dalabaadka bajajyada ee rabaabka iyo ka jawaabista cabashooyinka macaamiisha.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('rides')}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              activeTab === 'rides' 
                ? 'bg-rose-600 text-white shadow-md shadow-rose-600/20' 
                : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
            }`}
          >
            Dalabaadka Bajajyada ({rides.length})
          </button>
          <button
            onClick={() => setActiveTab('support')}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition ${
              activeTab === 'support' 
                ? 'bg-rose-600 text-white shadow-md shadow-rose-600/20' 
                : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
            }`}
          >
            Cabashooyinka & Taageerada ({tickets.filter(t => t.status === 'open').length})
          </button>
        </div>
      </div>

      {successMsg && (
        <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-4 py-3 rounded-xl text-sm flex items-center gap-2">
          <CheckCircle className="w-5 h-5 text-emerald-600" />
          {successMsg}
        </div>
      )}

      {/* Search & Refresh */}
      <div className="flex items-center justify-between gap-4 bg-white p-4 rounded-xl shadow-sm border border-neutral-100">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
          <input
            type="text"
            placeholder={activeTab === 'rides' ? "Raadi magaca rakaabka, telefoonka ama goobta..." : "Raadi telefoonka ama mawduuca cabashada..."}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 bg-neutral-50 border border-neutral-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-600"
          />
        </div>
        <button
          onClick={fetchData}
          className="flex items-center gap-2 px-4 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-lg text-sm font-medium transition"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          Cusbooneysii
        </button>
      </div>

      {/* Content Area */}
      {activeTab === 'rides' ? (
        <div className="bg-white rounded-2xl shadow-sm border border-neutral-100 overflow-hidden">
          <div className="p-4 border-b border-neutral-100 font-bold text-neutral-800 flex items-center justify-between">
            <span>Liiska Dalabaadka GoBajaaj Ride</span>
            <span className="text-xs bg-rose-50 text-rose-700 px-2.5 py-1 rounded-full font-medium">
              {filteredRides.length} dalab
            </span>
          </div>
          <div className="divide-y divide-neutral-100 overflow-x-auto">
            {filteredRides.length === 0 ? (
              <div className="p-8 text-center text-neutral-400 text-sm">Hadda ma jiraan dalabaad la helay.</div>
            ) : (
              filteredRides.map(ride => (
                <div key={ride.id} className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-neutral-50/50 transition">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-neutral-900">{ride.passenger_name}</span>
                      <span className="text-xs bg-neutral-100 text-neutral-600 px-2 py-0.5 rounded flex items-center gap-1">
                        <Phone className="w-3 h-3" /> {ride.passenger_phone}
                      </span>
                    </div>
                    <div className="text-xs text-neutral-500 flex items-center gap-3">
                      <span className="flex items-center gap-1">
                        <MapPin className="w-3.5 h-3.5 text-rose-600" /> Laga bilaabo: <b>{ride.pickup_location}</b>
                      </span>
                      <span>•</span>
                      <span>Ku socota: <b>{ride.destination}</b></span>
                    </div>
                    {ride.driver_name && (
                      <div className="text-xs text-emerald-700 font-medium mt-1">
                        Darawalka Xiriirka: {ride.driver_name}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    <span className={`text-xs px-3 py-1 rounded-full font-semibold ${
                      ride.status === 'pending' ? 'bg-amber-100 text-amber-800' :
                      ride.status === 'accepted' ? 'bg-blue-100 text-blue-800' :
                      ride.status === 'completed' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {ride.status === 'pending' ? 'Sugaya Darawal' :
                       ride.status === 'accepted' ? 'Waa La Aqbalay' :
                       ride.status === 'completed' ? 'La Dhammeeyay' : 'La Joojiyay'}
                    </span>

                    {ride.status === 'pending' && (
                      <button
                        onClick={() => handleUpdateRideStatus(ride.id, 'accepted')}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold transition"
                      >
                        Aqbal (Assign)
                      </button>
                    )}
                    {ride.status === 'accepted' && (
                      <button
                        onClick={() => handleUpdateRideStatus(ride.id, 'completed')}
                        className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-xs font-semibold transition"
                      >
                        Dhammee
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl shadow-sm border border-neutral-100 overflow-hidden">
          <div className="p-4 border-b border-neutral-100 font-bold text-neutral-800 flex items-center justify-between">
            <span>Cabashooyinka & Tigidhada Taageerada Macaamiisha</span>
            <span className="text-xs bg-amber-50 text-amber-700 px-2.5 py-1 rounded-full font-medium">
              {filteredTickets.filter(t => t.status === 'open').length} furan
            </span>
          </div>
          <div className="divide-y divide-neutral-100 overflow-x-auto">
            {filteredTickets.length === 0 ? (
              <div className="p-8 text-center text-neutral-400 text-sm">Ma jiraan cabashooyin ama tigidho taageero ah.</div>
            ) : (
              filteredTickets.map(ticket => (
                <div key={ticket.id} className="p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 hover:bg-neutral-50/50 transition">
                  <div className="space-y-1 max-w-xl">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-neutral-900">{ticket.subject}</span>
                      <span className="text-xs text-neutral-500">({ticket.user_phone})</span>
                    </div>
                    <p className="text-xs text-neutral-600">{ticket.message}</p>
                    {ticket.admin_reply && (
                      <div className="text-xs bg-emerald-50 text-emerald-800 p-2 rounded mt-2 border border-emerald-100">
                        <b>Jawaabta Admin:</b> {ticket.admin_reply}
                      </div>
                    )}
                  </div>

                  <div className="flex items-center gap-3">
                    <span className={`text-xs px-3 py-1 rounded-full font-semibold ${
                      ticket.status === 'open' ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {ticket.status === 'open' ? 'Furan (Open)' : 'La Xaliyay (Resolved)'}
                    </span>

                    {ticket.status === 'open' && (
                      <button
                        onClick={() => setSelectedTicket(ticket)}
                        className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold transition"
                      >
                        Ka Jawaab
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Reply Modal */}
      {selectedTicket && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-lg font-bold text-neutral-900 flex items-center gap-2">
              <MessageSquare className="w-5 h-5 text-rose-600" />
              Ka Jawaab Cabashada
            </h3>
            <div className="bg-neutral-50 p-3 rounded-xl text-xs space-y-1">
              <div className="font-bold text-neutral-800">{selectedTicket.subject}</div>
              <div className="text-neutral-600">{selectedTicket.message}</div>
              <div className="text-neutral-400 pt-1">Tel: {selectedTicket.user_phone}</div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-neutral-700 mb-1">Jawaabta Maamulaha:</label>
              <textarea
                rows={3}
                placeholder="Qor jawaabta ama xalka cabashada..."
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                className="w-full p-3 bg-neutral-50 border border-neutral-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-rose-500/20 focus:border-rose-600"
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                onClick={() => setSelectedTicket(null)}
                className="px-4 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 rounded-xl text-xs font-semibold"
              >
                Jooji
              </button>
              <button
                onClick={handleSendReply}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-semibold flex items-center gap-1.5 shadow-md shadow-rose-600/20"
              >
                <Send className="w-3.5 h-3.5" /> Dir Jawaabta
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
