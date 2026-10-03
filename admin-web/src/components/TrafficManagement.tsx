import React, { useState, useEffect } from 'react';
import { TrafficCone, MapPin, AlertTriangle, RefreshCw, Plus, Edit, ShieldAlert, CheckCircle2, Trash2 } from 'lucide-react';
import { Intersection, TrafficReport } from '../types';
import { supabase } from '../supabase';
import { ConfirmationModal } from './ConfirmationModal';

interface TrafficManagementProps {
  actorName: string;
  canManage: boolean;
}

export const TrafficManagement: React.FC<TrafficManagementProps> = ({ actorName, canManage }) => {
  const [intersections, setIntersections] = useState<Intersection[]>([]);
  const [trafficReports, setTrafficReports] = useState<TrafficReport[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // New Intersection Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [name, setName] = useState('');
  const [latitude, setLatitude] = useState('2.0335');
  const [longitude, setLongitude] = useState('45.3190');
  const [allowedRadius, setAllowedRadius] = useState('400');
  const [status, setStatus] = useState<'Furan' | 'Dhexdhexaad' | 'Mashquul' | 'Saxmad badan' | 'Xiran' | 'Wuu xiranyahay'>('Furan');

  // Confirmation modal for status change
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    intersectionId: string;
    newStatus: 'Furan' | 'Dhexdhexaad' | 'Mashquul' | 'Saxmad badan' | 'Xiran' | 'Wuu xiranyahay';
    name: string;
  }>({ isOpen: false, intersectionId: '', newStatus: 'Furan', name: '' });

  // Delete modal
  const [deleteModal, setDeleteModal] = useState<{
    isOpen: boolean;
    intersectionId: string;
    name: string;
  }>({ isOpen: false, intersectionId: '', name: '' });

  // Clear all data modal
  const [clearAllModal, setClearAllModal] = useState(false);

  const fetchTrafficData = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      // 1. Fetch intersections
      const { data: intData, error: intErr } = await supabase
        .from('intersections')
        .select('*')
        .order('name', { ascending: true });

      if (intErr) {
        console.error('Fetch intersections error:', intErr.message);
        setErrorMsg('Cilad ayaa ka dhacday soo qaadista xogta isgoysyada.');
      } else if (intData) {
        setIntersections(intData as Intersection[]);
      }

      // 2. Fetch traffic reports
      const { data: repData, error: repErr } = await supabase
        .from('traffic_reports')
        .select('*')
        .order('created_at', { ascending: false });

      if (repErr) {
        console.error('Fetch traffic_reports error:', repErr.message);
      } else if (repData) {
        setTrafficReports(repData as TrafficReport[]);
      }

    } catch (err: any) {
      setErrorMsg('Cilad ayaa ka dhacday xogta. Dib u tijaabi.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTrafficData();
  }, []);

  const handleCreateIntersection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canManage) {
      alert('Ma lehid fasax aad ku abuurtid Isgoys.');
      return;
    }
    if (!name.trim()) {
      alert('Fadlan geli Magaca Isgoyska.');
      return;
    }

    setIsLoading(true);
    try {
      const newId = `int_${Date.now()}`;
      const { error } = await supabase.from('intersections').insert([{
        name: name.trim(),
        latitude: parseFloat(latitude) || 2.0335,
        longitude: parseFloat(longitude) || 45.3190,
        allowed_radius: parseInt(allowedRadius) || 400,
        city: 'Muqdisho',
        current_status: status,
        last_updated: new Date().toISOString()
      }]);

      if (error) {
        console.error('Insert intersection error:', error.message);
      }

      // Audit Log
      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: 'INTERSECTION_CREATED',
        target_type: 'intersection',
        target_id: newId,
        metadata: { name, status }
      }]);

      await fetchTrafficData();
      setShowAddModal(false);
      setName('');
    } catch (err: any) {
      alert('Cilad ayaa dhacday marka la kaydinayay isgoyska.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleExecuteStatusChange = async () => {
    const { intersectionId, newStatus, name } = confirmModal;
    setIsLoading(true);
    try {
      await supabase
        .from('intersections')
        .update({
          current_status: newStatus,
          last_updated: new Date().toISOString()
        })
        .eq('id', intersectionId);

      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: 'INTERSECTION_STATUS_UPDATED',
        target_type: 'intersection',
        target_id: intersectionId,
        metadata: { name, new_status: newStatus }
      }]);

      await fetchTrafficData();
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
      setConfirmModal({ isOpen: false, intersectionId: '', newStatus: 'Furan', name: '' });
    }
  };

  const handleDeleteIntersection = async () => {
    const { intersectionId, name } = deleteModal;
    if (!canManage || !intersectionId) return;
    setIsLoading(true);
    try {
      const { error } = await supabase.from('intersections').delete().eq('id', intersectionId);
      if (error) {
        alert(`Cilad tirtiridda: ${error.message}`);
      } else {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'INTERSECTION_DELETED',
          target_type: 'intersection',
          target_id: intersectionId,
          metadata: { name }
        }]);
        await fetchTrafficData();
      }
    } catch (err: any) {
      alert('Cilad ayaa dhacday.');
    } finally {
      setIsLoading(false);
      setDeleteModal({ isOpen: false, intersectionId: '', name: '' });
    }
  };

  const handleClearAllIntersections = async () => {
    if (!canManage) return;
    setIsLoading(true);
    try {
      const { error } = await supabase.from('intersections').delete().neq('id', 'dummy_value_to_match_all');
      if (error) {
        alert(`Cilad: ${error.message}`);
      } else {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'ALL_INTERSECTIONS_CLEARED',
          target_type: 'intersections',
          target_id: 'bulk',
          metadata: { previous_count: intersections.length }
        }]);
        await fetchTrafficData();
      }
    } catch (err: any) {
      alert('Cilad: ' + err.message);
    } finally {
      setIsLoading(false);
      setClearAllModal(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <TrafficCone className="w-6 h-6 text-[#E50914] shrink-0" />
            <h2 className="text-lg sm:text-xl font-black text-black">Maamulka Isgoysyada & Saxmadda (Traffic Management)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Kormeer oo cusbooneysii xaaladaha isgoysyada Muqdisho (Furan, Dhexdhexaad, Mashquul, Saxmad badan).
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <button 
            onClick={fetchTrafficData}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>

          {canManage && intersections.length > 0 && (
            <button 
              onClick={() => setClearAllModal(true)}
              className="px-3.5 py-2.5 bg-neutral-100 border border-neutral-300 text-neutral-700 hover:bg-red-50 hover:text-red-700 hover:border-red-300 rounded-xl text-xs font-bold transition flex items-center space-x-1.5 cursor-pointer"
              title="Tirtir dhammaan isgoysyada"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Nadiifi Dhammaan ({intersections.length})</span>
            </button>
          )}
          
          {canManage && (
            <button 
              onClick={() => setShowAddModal(true)}
              className="px-4 py-2.5 bg-[#E50914] text-white rounded-xl text-xs sm:text-sm font-bold shadow hover:bg-[#B91C1C] transition flex items-center space-x-2 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>+ Ku Dar Isgoys</span>
            </button>
          )}
        </div>
      </div>

      {!canManage && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-semibold flex items-center space-x-2">
          <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
          <span>Fiiro gaar ah: Keliya TRAFFIC ADMIN ama SUPER ADMIN ayaa wax ka beddeli kara isgoysyada.</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Intersections Grid */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6">
        <h3 className="text-sm font-bold text-black uppercase tracking-wider mb-4">
          Isgoysyada Muqdisho ({intersections.length})
        </h3>
        
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya isgoysyada...</p>
          </div>
        ) : intersections.length === 0 ? (
          <div className="p-12 text-center">
            <MapPin className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h4 className="text-base font-bold text-black">Xog lama helin</h4>
            <p className="text-xs text-neutral-500 mt-1">Ma jiro isgoys diiwaangashan.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
            {intersections.map((int) => (
              <div key={int.id} className="p-4 rounded-xl border border-neutral-200 bg-neutral-50 flex flex-col justify-between space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-bold text-black text-sm">{int.name}</p>
                    <p className="text-[11px] text-neutral-500 font-mono mt-0.5">
                      GPS: {int.latitude}, {int.longitude} ({int.allowed_radius}m radius)
                    </p>
                  </div>
                  <div className="flex items-center space-x-2 shrink-0">
                    <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                      int.current_status === 'Furan' ? 'bg-emerald-100 text-emerald-800' :
                      int.current_status === 'Dhexdhexaad' ? 'bg-amber-100 text-amber-800' :
                      int.current_status === 'Mashquul' ? 'bg-orange-100 text-orange-800' :
                      (int.current_status === 'Xiran' || int.current_status === 'Wuu xiranyahay') ? 'bg-neutral-950 text-white border border-black' :
                      'bg-red-100 text-red-800'
                    }`}>
                      {int.current_status}
                    </span>
                    {canManage && (
                      <button
                        onClick={() => setDeleteModal({ isOpen: true, intersectionId: int.id, name: int.name })}
                        className="p-1 text-neutral-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition cursor-pointer"
                        title="Tirtir isgoyskan"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>

                {canManage && (
                  <div className="flex flex-wrap items-center gap-1 pt-2 border-t border-neutral-200">
                    <span className="text-[11px] text-neutral-500 font-semibold mr-1">Badel:</span>
                    {(['Furan', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan', 'Xiran'] as const).map((st) => (
                      <button
                        key={st}
                        onClick={() => setConfirmModal({
                          isOpen: true,
                          intersectionId: int.id,
                          newStatus: st,
                          name: int.name
                        })}
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border transition cursor-pointer ${
                          int.current_status === st ? 'bg-black text-white border-black' : 'bg-white text-neutral-700 hover:bg-neutral-200'
                        }`}
                      >
                        {st}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Traffic Reports Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-neutral-200">
          <h3 className="text-sm font-bold text-black uppercase tracking-wider">
            Warbixinnada Saxmadda ee Tooska ah (Live Traffic Reports)
          </h3>
        </div>
        {trafficReports.length === 0 ? (
          <div className="p-12 text-center">
            <AlertTriangle className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h4 className="text-base font-bold text-black">Warbixin taraafik ah lama helin.</h4>
            <p className="text-xs text-neutral-500 mt-1">Ma jiro warbixin taraafik ah oo cusub.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[700px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Isgoyska</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  <th className="p-4 font-bold">Sababta & Faahfaahin</th>
                  <th className="p-4 font-bold">GPS Radius Verification</th>
                  <th className="p-4 font-bold">Waqtiga</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {trafficReports.map((tr) => (
                  <tr key={tr.id} className="hover:bg-neutral-50 transition">
                    <td className="p-4 font-bold text-black">
                      {intersections.find(i => i.id === tr.intersection_id)?.name || 'Isgoyska Muqdisho'}
                    </td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                        tr.status === 'Furan' ? 'bg-emerald-100 text-emerald-800' :
                        tr.status === 'Dhexdhexaad' ? 'bg-amber-100 text-amber-800' :
                        tr.status === 'Mashquul' ? 'bg-orange-100 text-orange-800' :
                        'bg-red-100 text-red-800'
                      }`}>
                        {tr.status}
                      </span>
                    </td>
                    <td className="p-4 text-xs">
                      <p className="font-bold text-black">{tr.cause || 'Warbixin Saxmad'}</p>
                      <p className="text-neutral-500">{tr.notes}</p>
                    </td>
                    <td className="p-4">
                      <span className="px-2 py-0.5 rounded text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200 inline-flex items-center space-x-1">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>{tr.verification_status === 'verified' ? 'Gudaha Radius-ka (Verified)' : 'Nidaamka Tooska ah'}</span>
                      </span>
                    </td>
                    <td className="p-4 text-xs font-mono text-neutral-500">{tr.created_at?.split('T')[0] || 'Hadda'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Intersection Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden">
            <div className="p-6 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
              <h3 className="font-black text-lg text-black">Ku Dar Isgoys Cusub</h3>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-400 hover:text-black">✕</button>
            </div>

            <form onSubmit={handleCreateIntersection} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-black mb-1">Magaca Isgoyska</label>
                <input
                  type="text"
                  required
                  placeholder="m.g. Isgoyska Dabka (Maka Al-Mukarama)"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black"
                />
              </div>

              <div className="space-y-1">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-black">Coordinates (Latitude & Longitude)</label>
                  <button
                    type="button"
                    onClick={() => {
                      if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                          (pos) => {
                            setLatitude(pos.coords.latitude.toFixed(6));
                            setLongitude(pos.coords.longitude.toFixed(6));
                          },
                          (err) => alert('Lama heli karin GPS-ka: ' + err.message)
                        );
                      } else {
                        alert('Browser-kaagu ma taageerayo GPS-ka.');
                      }
                    }}
                    className="text-xs font-bold text-[#E50914] hover:underline flex items-center space-x-1"
                  >
                    <span>📍 Soo Qaad GPS-kayga Hadda</span>
                  </button>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <input
                      type="text"
                      placeholder="Latitude"
                      value={latitude}
                      onChange={(e) => setLatitude(e.target.value)}
                      className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-xl font-mono text-black"
                    />
                  </div>
                  <div>
                    <input
                      type="text"
                      placeholder="Longitude"
                      value={longitude}
                      onChange={(e) => setLongitude(e.target.value)}
                      className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-xl font-mono text-black"
                    />
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Radius-ka Loo Oggolyahay (Meters)</label>
                <input
                  type="number"
                  value={allowedRadius}
                  onChange={(e) => setAllowedRadius(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-neutral-300 rounded-xl font-mono text-black"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Xaaladda Hadda</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as any)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black bg-white font-bold"
                >
                  <option value="Furan">Furan (🟢 Saxmad la'aan)</option>
                  <option value="Dhexdhexaad">Dhexdhexaad (🟡 Dhexdhexaad)</option>
                  <option value="Mashquul">Mashquul (🟠 Mashquul)</option>
                  <option value="Saxmad badan">Saxmad badan (🔴 Saxmad Daran)</option>
                  <option value="Xiran">Waddada waa xirantahay (⚫ Waa xiranyahay)</option>
                </select>
              </div>

              <div className="pt-4 border-t border-neutral-200 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 text-sm font-bold text-neutral-600 hover:text-black"
                >
                  Ka noqo
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-5 py-2 bg-[#E50914] text-white text-sm font-bold rounded-xl shadow hover:bg-[#B91C1C]"
                >
                  Kaydi Isgoyska
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen}
        title="Badelidda Xaaladda Isgoyska"
        message={`Ma hubtaa inaad beddelo xaaladda "${confirmModal.name}" oo aad ka dhigto "${confirmModal.newStatus}"?`}
        confirmLabel="Xaqiiji"
        cancelLabel="Jooji"
        isDanger={confirmModal.newStatus === 'Saxmad badan'}
        isLoading={isLoading}
        onConfirm={handleExecuteStatusChange}
        onCancel={() => setConfirmModal({ isOpen: false, intersectionId: '', newStatus: 'Furan', name: '' })}
      />

      {/* Delete Single Intersection Modal */}
      <ConfirmationModal
        isOpen={deleteModal.isOpen}
        title="Tirtiridda Isgoyska"
        message={`Ma hubtaa inaad gabi ahaanba tirtirto isgoyskan "${deleteModal.name}"? Ficilkan dib looma soo celin karo.`}
        confirmLabel="Haa, Tirtir Isgoyska"
        cancelLabel="Ka Noqo"
        isDanger={true}
        isLoading={isLoading}
        onConfirm={handleDeleteIntersection}
        onCancel={() => setDeleteModal({ isOpen: false, intersectionId: '', name: '' })}
      />

      {/* Clear All Intersections Modal */}
      <ConfirmationModal
        isOpen={clearAllModal}
        title="Tirtiridda Isgoysyada (Clear All)"
        message={`Ma hubtaa inaad tirtirto dhammaan ${intersections.length} isgoys? Tirada isgoysyada waxay noqon doontaa 0.`}
        confirmLabel={`Haa, Tirtir Dhammaan (${intersections.length})`}
        cancelLabel="Ka Noqo"
        isDanger={true}
        isLoading={isLoading}
        onConfirm={handleClearAllIntersections}
        onCancel={() => setClearAllModal(false)}
      />
    </div>
  );
};
