import React, { useState, useEffect } from 'react';
import { UserCheck, UserPlus, RefreshCw, AlertCircle, MapPin, Check, ShieldAlert, Eye, EyeOff, KeyRound, Copy } from 'lucide-react';
import { createClient } from '@supabase/supabase-js';
import { Profile, Intersection } from '../types';
import { supabase } from '../supabase';
import { ConfirmationModal } from './ConfirmationModal';

interface ReporterManagementProps {
  actorName: string;
  canManage: boolean;
}

export const ReporterManagement: React.FC<ReporterManagementProps> = ({ actorName, canManage }) => {
  const [reporters, setReporters] = useState<Profile[]>([]);
  const [intersections, setIntersections] = useState<Intersection[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form Modal State
  const [showAddModal, setShowAddModal] = useState(false);
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [selectedIntersectionId, setSelectedIntersectionId] = useState('');
  const [customIntersectionName, setCustomIntersectionName] = useState('');

  // Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    reporterId: string;
    action: 'deactivate' | 'activate';
    name: string;
  }>({ isOpen: false, reporterId: '', action: 'deactivate', name: '' });

  const fetchReportersAndIntersections = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    setErrorMsg(null);
    try {
      // Query profiles with role = 'reporter'
      const { data: repData, error: repError } = await supabase
        .from('profiles')
        .select('*')
        .eq('role', 'reporter')
        .order('created_at', { ascending: false });

      if (repError) {
        console.error('Fetch reporters error:', repError.message);
        setErrorMsg('Cilad ayaa ka dhacday soo qaadista xogta wariyeyaasha.');
        setReporters([]);
      } else {
        setReporters(repData as Profile[] || []);
      }

      // Query intersections
      const { data: intData, error: intError } = await supabase
        .from('intersections')
        .select('*')
        .order('name', { ascending: true });

      if (intError) {
        console.error('Fetch intersections error:', intError.message);
      } else {
        setIntersections(intData as Intersection[] || []);
      }
    } catch (err: any) {
      setErrorMsg('Cilad ayaa ka dhacday xogta. Dib u tijaabi.');
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReportersAndIntersections(false);

    const interval = setInterval(() => {
      fetchReportersAndIntersections(true);
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  const handleCreateReporter = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canManage) {
      alert('Ma lehid fasax aad ku abuurtid Wariye.');
      return;
    }
    if (!fullName.trim() || !phoneNumber.trim()) {
      alert('Fadlan geli Magaca Buuxa iyo Nambarka Taleefanka.');
      return;
    }
    if (!password.trim() || password.trim().length < 6) {
      alert('Fadlan geli Password (ugu yaraan 6 xaraf) ah.');
      return;
    }

    setIsLoading(true);
    try {
      // Check for duplicate phone number
      const cleanInputPhone = phoneNumber.trim();
      const { data: existingProf } = await supabase
        .from('profiles')
        .select('id, full_name')
        .or(`phone_number.eq.${cleanInputPhone},phone_number.ilike.%${cleanInputPhone.slice(-8)}%`)
        .limit(1);

      if (existingProf && existingProf.length > 0) {
        alert(`Lambarkani (${cleanInputPhone}) hore ayaa loogu diiwaangeliyay (${existingProf[0].full_name || 'isticmaale kale'}). Fadlan geli lambar cusub.`);
        setIsLoading(false);
        return;
      }

      // Create Supabase Auth account for reporter
      // Username = phone number, Email = phone@phone.gobajaaj.so
      const cleanPhone = phoneNumber.trim().replace(/\s+/g, '');
      const phoneEmail = `${cleanPhone.replace(/[^0-9]/g, '')}@phone.gobajaaj.so`;

      const supabaseUrl = (import.meta as any).env?.VITE_SUPABASE_URL || '';
      const supabaseAnonKey = (import.meta as any).env?.VITE_SUPABASE_ANON_KEY || '';
      const tempClient = createClient(supabaseUrl, supabaseAnonKey, {
        auth: { persistSession: false }
      });

      const { data: authData, error: authErr } = await tempClient.auth.signUp({
        email: phoneEmail,
        password: password.trim(),
        options: {
          data: {
            full_name: fullName.trim(),
            phone: cleanPhone,
            role: 'reporter'
          }
        }
      });

      if (authErr && !authErr.message?.includes('already registered')) {
        console.warn('Auth creation notice:', authErr.message);
      }

      const newId = authData?.user?.id || ((typeof crypto !== 'undefined' && crypto.randomUUID)
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
          const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        }));

      // Insert profile
      const { error: pErr } = await supabase.from('profiles').insert([{
        id: newId,
        full_name: fullName.trim(),
        phone_number: phoneNumber.trim(),
        role: 'reporter',
        status: 'active'
      }]);

      if (pErr) {
        console.error('Error inserting reporter profile:', pErr.message);
        alert('Cilad ayaa dhacday marka la kaydinayay wariyaha: ' + pErr.message);
        setIsLoading(false);
        return;
      }

      let targetIntersectionId = selectedIntersectionId === 'NEW_CUSTOM' ? '' : selectedIntersectionId;

      // Handle custom typed intersection name
      if (customIntersectionName.trim()) {
        const { data: newIntData, error: newIntErr } = await supabase
          .from('intersections')
          .insert([{
            name: customIntersectionName.trim(),
            latitude: 2.0335,
            longitude: 45.3190,
            allowed_radius: 400,
            city: 'Muqdisho',
            current_status: 'Furan',
            last_updated: new Date().toISOString()
          }])
          .select()
          .single();

        if (newIntErr) {
          console.error('Error creating custom intersection:', newIntErr.message);
        } else if (newIntData) {
          targetIntersectionId = newIntData.id;
        }
      }

      // Assign intersection if selected or newly created
      if (targetIntersectionId) {
        await assignReporterToIntersection(newId, targetIntersectionId);
      }

      // Write Audit Log
      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: 'REPORTER_CREATED',
        target_type: 'reporter',
        target_id: newId,
        metadata: { full_name: fullName, phone_number: phoneNumber, intersection_id: targetIntersectionId }
      }]);

      await fetchReportersAndIntersections();
      setShowAddModal(false);
      setFullName('');
      setPhoneNumber('');
      setPassword('');
      setSelectedIntersectionId('');
      setCustomIntersectionName('');
      alert(`✅ Wariyaha si guul leh ayaa loogu diiwaangeliyay!\n\nXogta Login-ka:\n📱 Username: ${phoneNumber.trim()}\n🔑 Password: ${password.trim()}\n\nWariyaha gacanta uga sii xogahan si uu u galo app-ka.`);
    } catch (err: any) {
      alert('Cilad ayaa dhacday marka la kaydinayay wariyaha.');
    } finally {
      setIsLoading(false);
    }
  };

  const assignReporterToIntersection = async (reporterId: string, intersectionId: string) => {
    if (!canManage) return;
    try {
      // 1. Clear any previous assignment for this intersection (Enforces ONE ACTIVE REPORTER PER INTERSECTION rule)
      await supabase
        .from('intersections')
        .update({ assigned_reporter_id: null })
        .eq('id', intersectionId);

      // 2. Set new assigned_reporter_id
      const { error } = await supabase
        .from('intersections')
        .update({ assigned_reporter_id: reporterId, last_updated: new Date().toISOString() })
        .eq('id', intersectionId);

      if (error) {
        console.error('Failed to assign reporter:', error.message);
      } else {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'REPORTER_ASSIGNED',
          target_type: 'intersection',
          target_id: intersectionId,
          metadata: { reporter_id: reporterId }
        }]);
      }
    } catch (err) {
      console.error('Assign error:', err);
    }
  };

  const handleToggleReporterStatus = async () => {
    const { reporterId, action } = confirmModal;
    const nextStatus = action === 'activate' ? 'active' : 'disabled';
    setIsLoading(true);
    try {
      await supabase.from('profiles').update({ status: nextStatus }).eq('id', reporterId);

      // If deactivating, remove intersection assignment
      if (nextStatus === 'disabled') {
        await supabase.from('intersections').update({ assigned_reporter_id: null }).eq('assigned_reporter_id', reporterId);
      }

      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: `REPORTER_${nextStatus.toUpperCase()}`,
        target_type: 'reporter',
        target_id: reporterId,
        metadata: { new_status: nextStatus }
      }]);

      await fetchReportersAndIntersections();
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
      setConfirmModal({ isOpen: false, reporterId: '', action: 'deactivate', name: '' });
    }
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <UserCheck className="w-6 h-6 text-[#E50914] shrink-0" />
            <h2 className="text-lg sm:text-xl font-black text-black">Maamulka Wariyeyaasha Saxmadda (Reporter Manager)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Diiwaangeli wariyeyaasha saxmadda oo u qoondee isgoysyada Muqdisho (Keliya 1 Wariye Isgoyskiiba).
          </p>
        </div>

        <div className="flex items-center space-x-3 shrink-0">
          <button
            onClick={fetchReportersAndIntersections}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
            title="Cusbooneysii"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>

          {canManage && (
            <button
              onClick={() => setShowAddModal(true)}
              className="px-4 py-2.5 bg-[#E50914] text-white rounded-xl text-sm font-bold shadow hover:bg-[#B91C1C] transition flex items-center space-x-2 cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span>+ Ku Dar Wariye</span>
            </button>
          )}
        </div>
      </div>

      {!canManage && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-semibold flex items-center space-x-2">
          <ShieldAlert className="w-4 h-4 text-amber-600 shrink-0" />
          <span>Fiiro gaar ah: Keliya REPORTER MANAGER ama SUPER ADMIN ayaa maamuli kara ama abuuri kara Wariyetada.</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Reporters Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya wariyeyaasha...</p>
          </div>
        ) : reporters.length === 0 ? (
          <div className="p-12 text-center">
            <UserCheck className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Xog lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1 mb-4">Ma jiro wariye saxmad oo diiwaangashan hadda.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[700px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Wariyaha</th>
                  <th className="p-4 font-bold">Nambarka Taleefanka</th>
                  <th className="p-4 font-bold">Isgoyska Loo Xilsaaray (1 Active Rule)</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  {canManage && <th className="p-4 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {reporters.map((rep) => {
                  const assignedInt = intersections.find(i => i.assigned_reporter_id === rep.id);
                  return (
                    <tr key={rep.id} className="hover:bg-neutral-50 transition">
                      <td className="p-4 font-bold text-black flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-[#E50914] text-white flex items-center justify-center text-xs font-black shrink-0">
                          {rep.full_name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-black">{rep.full_name}</p>
                          <p className="text-[11px] text-neutral-400 font-mono">ID: {rep.id}</p>
                        </div>
                      </td>
                      <td className="p-4 text-black font-mono text-xs">{rep.phone_number}</td>
                      <td className="p-4">
                        {canManage ? (
                          <select
                            value={assignedInt?.id || ''}
                            onChange={(e) => assignReporterToIntersection(rep.id, e.target.value)}
                            className="px-3 py-1.5 bg-neutral-100 border border-neutral-200 rounded-xl text-xs font-bold text-black focus:outline-none focus:border-[#E50914]"
                          >
                            <option value="">-- Ma laha Isgoys --</option>
                            {intersections.map((int) => (
                              <option key={int.id} value={int.id}>
                                {int.name} {int.assigned_reporter_id && int.assigned_reporter_id !== rep.id ? '(Wariye kale ayaa ku jira)' : ''}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <span className="text-xs font-bold text-neutral-700">
                            {assignedInt ? assignedInt.name : 'Ma laha isgoys'}
                          </span>
                        )}
                      </td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${rep.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                          }`}>
                          {rep.status === 'active' ? '🟢 SHAKAL' : '🔴 LA JOOJIYAY'}
                        </span>
                      </td>
                      {canManage && (
                        <td className="p-4 text-right">
                          <button
                            onClick={() => setConfirmModal({
                              isOpen: true,
                              reporterId: rep.id,
                              action: rep.status === 'active' ? 'deactivate' : 'activate',
                              name: rep.full_name
                            })}
                            className={`px-3 py-1.5 text-xs font-bold rounded-xl border transition cursor-pointer ${rep.status === 'active'
                                ? 'border-red-200 text-red-600 hover:bg-red-50'
                                : 'border-emerald-200 text-emerald-600 hover:bg-emerald-50'
                              }`}
                          >
                            {rep.status === 'active' ? 'Deactivate' : 'Activate'}
                          </button>
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Reporter Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden">
            <div className="p-6 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
              <h3 className="font-black text-lg text-black">Ku Dar Wariye Gaadiid</h3>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-400 hover:text-black">✕</button>
            </div>

            <form onSubmit={handleCreateReporter} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-black mb-1">Magaca Buuxa ee Wariyaha</label>
                <input
                  type="text"
                  required
                  placeholder="m.g. Wariye Cabdi Shire"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Nambarka Taleefanka (= Username ee Login-ka)</label>
                <input
                  type="text"
                  required
                  placeholder="+252 61XXXXXXX"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono"
                />
                <p className="text-[11px] text-neutral-500 mt-1">📱 Nambarkani waa username-ka ee wariyahu ku galay app-ka.</p>
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Password ee App-ka (ugu yaraan 6 xaraf)</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    minLength={6}
                    placeholder="Password cusub..."
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-black cursor-pointer"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <p className="text-[11px] text-neutral-500 mt-1">🔑 Password-kan wariyaha ayaa isticmaali doona marka uu app-ka ku galo.</p>
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Isgoyska Loo Xilsaarayo (Select ama Qor Magaca)</label>
                {intersections.length > 0 && (
                  <select
                    value={selectedIntersectionId}
                    onChange={(e) => {
                      setSelectedIntersectionId(e.target.value);
                      if (e.target.value !== 'NEW_CUSTOM') {
                        setCustomIntersectionName('');
                      }
                    }}
                    className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black bg-white font-bold mb-2"
                  >
                    <option value="">-- Dooro Isgoysyada Diiwaangashan --</option>
                    {intersections.map(int => (
                      <option key={int.id} value={int.id}>{int.name}</option>
                    ))}
                    <option value="NEW_CUSTOM">✍️ Qor Isgoys Cusub (Enter Custom Intersection)...</option>
                  </select>
                )}

                <input
                  type="text"
                  placeholder="Qor Magaca Isgoyska (m.g. Isgoyska Towfiiq, Digfeer, Fagax)"
                  value={customIntersectionName}
                  onChange={(e) => {
                    setCustomIntersectionName(e.target.value);
                    if (e.target.value.trim() && selectedIntersectionId !== 'NEW_CUSTOM') {
                      setSelectedIntersectionId('NEW_CUSTOM');
                    }
                  }}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-semibold bg-white"
                />
                <p className="text-[11px] text-neutral-500 mt-1">
                  Xusuusin: Halkan ku qor isgoyska aad rabto ama ka dooro kuwa aad abuurtay. Sharciga nidaamku waa 1 Wariye Isgoyskiiba.
                </p>
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
                  Kaydi Wariyaha
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Status Confirmation Modal */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen}
        title="Badelidda Xaaladda Wariyaha"
        message={`Ma hubtaa inaad deactivat-gareyso ama badesho wariyaha "${confirmModal.name}"?`}
        confirmLabel="Xaqiiji"
        cancelLabel="Jooji"
        isDanger={confirmModal.action === 'deactivate'}
        isLoading={isLoading}
        onConfirm={handleToggleReporterStatus}
        onCancel={() => setConfirmModal({ isOpen: false, reporterId: '', action: 'deactivate', name: '' })}
      />
    </div>
  );
};
