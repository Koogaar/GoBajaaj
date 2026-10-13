import React, { useState, useEffect, useCallback } from 'react';
import { Navigation, Navigation2, Radar, RefreshCw } from 'lucide-react';
import { supabase } from '../supabase';

interface ActiveRideRow {
  id: string;
  status: string;
  rider_name?: string;
  rider_phone?: string;
  pickup_address?: string;
  destination_address?: string;
  vehicle_type?: string;
  rider_offer?: number;
  agreed_fare?: number | null;
  assigned_driver_id?: string | null;
  assigned_driver_name?: string | null;
  assigned_driver_plate?: string | null;
  driver_lat?: number | null;
  driver_lng?: number | null;
  driver_heading?: number | null;
  driver_speed?: number | null;
  driver_location_at?: string | null;
  eta_minutes?: number;
  created_at?: string;
  updated_at?: string;
}

const ACTIVE_STATUSES = ['assigned', 'accepted', 'arrived', 'in_progress'];

const RIDE_STATUS_META: Record<string, { label: string; cls: string }> = {
  assigned: { label: 'Darawal la helay', cls: 'bg-indigo-100 text-indigo-800' },
  accepted: { label: 'ON THE WAY', cls: 'bg-blue-100 text-blue-800' },
  arrived: { label: 'SOO GAARAY', cls: 'bg-emerald-100 text-emerald-800' },
  in_progress: { label: 'SAFAR SOCDA', cls: 'bg-orange-100 text-orange-800' },
  searching: { label: 'Laa raadinayaa', cls: 'bg-neutral-100 text-neutral-700' },
  pending: { label: 'Sugaya', cls: 'bg-neutral-100 text-neutral-700' },
};

export const ActiveRides: React.FC<{ actorName: string }> = ({ actorName }) => {
  const [rides, setRides] = useState<ActiveRideRow[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [lastSync, setLastSync] = useState<string | null>(null);

  const fetchActiveRides = useCallback(async () => {
    const nowToIso = new Date().toISOString();
    try {
      const { data, error } = await supabase
        .from('bajaj_rides')
        .select('*')
        .in('status', ACTIVE_STATUSES)
        .order('updated_at', { ascending: false });

      if (error) {
        console.error('Fetch active rides error:', error.message);
        return;
      }
      setRides((data as ActiveRideRow[]) || []);
      setLastSync(new Date().toLocaleTimeString());
    } catch (err) {
      console.error('ActiveRides error:', err);
    }
  }, []);

  useEffect(() => {
    fetchActiveRides();
    const interval = setInterval(fetchActiveRides, 4000);
    return () => clearInterval(interval);
  }, [fetchActiveRides]);

  const ageSeconds = (locationAt?: string | null): number | null => {
    if (!locationAt) return null;
    const t = Date.parse(locationAt);
    if (Number.isNaN(t)) return null;
    return Math.max(0, Math.floor((Date.now() - t) / 1000));
  };

  const formatAge = (secs: number | null): string => {
    if (secs == null) return '—';
    if (secs < 60) return `${secs}s`;
    const m = Math.floor(secs / 60);
    return `${m}m${secs % 60}s`;
  };

  const liveCount = rides.length;
  const staleCount = rides.filter((r) => {
    const age = ageSeconds(r.driver_location_at);
    return age != null && age > 90;
  }).length;

  return (
    <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div>
          <div className="flex items-center space-x-2">
            <Radar className="w-5 h-5 text-[#E50914]" />
            <h3 className="text-sm font-bold text-black uppercase tracking-wider">
              GoBajaaj • Safarro Socda (Live Driver Tracking)
            </h3>
          </div>
          <p className="text-xs text-neutral-500 mt-1">
            {liveCount} safar socda {staleCount > 0 && `• ${staleCount} GPS-kiisu waa jiray (>90s)`} • La cusboonaysiiyay: {lastSync || '—'}
          </p>
        </div>
        <button
          onClick={() => { setIsLoading(true); fetchActiveRides().finally(() => setIsLoading(false)); }}
          className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer shrink-0"
          title="Cusbooneysii"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
        </button>
      </div>

      {rides.length === 0 ? (
        <div className="p-8 text-center">
          <Navigation className="w-10 h-10 text-neutral-300 mx-auto mb-2" />
          <p className="text-sm font-bold text-black">Ma jiraan safarro socda</p>
          <p className="text-xs text-neutral-500 mt-1">Marka rakaabku dalbado gaadiid, halkan ayay ku soo bixi doonaan.</p>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm min-w-[900px]">
            <thead className="bg-neutral-100 border-y border-neutral-200 text-black">
              <tr>
                <th className="p-3 font-bold">Rakaab</th>
                <th className="p-3 font-bold">Wadada (Pickup → Dest)</th>
                <th className="p-3 font-bold">Darawal</th>
                <th className="p-3 font-bold">Xaaladda</th>
                <th className="p-3 font-bold">Live GPS (Lat, Lng)</th>
                <th className="p-3 font-bold">Xawaare</th>
                <th className="p-3 font-bold">Qiimaha</th>
                <th className="p-3 font-bold">Waqtigii Cusboonaysiinta</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {rides.map((r) => {
                const age = ageSeconds(r.driver_location_at);
                const hasLive = r.driver_lat != null && r.driver_lng != null;
                const meta = RIDE_STATUS_META[r.status] || { label: r.status, cls: 'bg-neutral-100 text-neutral-700' };
                return (
                  <tr key={r.id} className="hover:bg-neutral-50 transition">
                    <td className="p-3">
                      <p className="font-bold text-black">{r.rider_name || 'Rakaab'}</p>
                      <p className="text-[11px] text-neutral-500 font-mono">{r.rider_phone || '—'}</p>
                    </td>
                    <td className="p-3 text-xs">
                      <p className="text-black font-semibold">{r.pickup_address || '—'}</p>
                      <p className="text-neutral-500">→ {r.destination_address || '—'}</p>
                    </td>
                    <td className="p-3">
                      <p className="font-bold text-black">{r.assigned_driver_name || 'Lamaan'}</p>
                      <p className="text-[11px] text-neutral-500 font-mono">{r.assigned_driver_plate || '—'}</p>
                    </td>
                    <td className="p-3">
                      <span className={`px-2.5 py-1 rounded-lg text-[11px] font-black ${meta.cls}`}>{meta.label}</span>
                    </td>
                    <td className="p-3">
                      {hasLive ? (
                        <p className="text-[11px] font-mono flex items-center space-x-1">
                          <Navigation2 className={`w-3.5 h-3.5 ${age != null && age > 90 ? 'text-red-500' : 'text-emerald-600'}`} />
                          <span>{r.driver_lat?.toFixed(5)}, {r.driver_lng?.toFixed(5)}</span>
                        </p>
                      ) : (
                        <span className="text-[11px] text-neutral-400">—</span>
                      )}
                    </td>
                    <td className="p-3 text-xs font-bold text-black">
                      {r.driver_speed != null ? `${Math.round(r.driver_speed)} km/h` : '—'}
                    </td>
                    <td className="p-3 text-xs font-bold text-emerald-700">
                      ${(r.agreed_fare ?? r.rider_offer ?? 0).toFixed(2)}
                    </td>
                    <td className="p-3">
                      <span className={`text-[11px] font-mono ${age != null && age > 90 ? 'text-red-600 font-black' : 'text-neutral-500'}`}>
                        {formatAge(age)} ku
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};