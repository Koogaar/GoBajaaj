import React, { useState, useEffect } from 'react';
import { UserCheck, UserPlus, RefreshCw, AlertCircle, MapPin, Check, ShieldAlert, Eye, EyeOff, KeyRound, Copy, Mail, Phone, Sparkles, Trash2 } from 'lucide-react';
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

  // Password Reset Modal for existing wariye
  const [resetModal, setResetModal] = useState<{
    isOpen: boolean;
    reporter: Profile | null;
    newPassword: string;
  }>({ isOpen: false, reporter: null, newPassword: '' });
  const [showResetPassword, setShowResetPassword] = useState(false);

  // Credentials Success Modal State
  const [successCredentials, setSuccessCredentials] = useState<{
    fullName: string;
    username: string;
    password: string;
    intersectionName: string;
    isReset?: boolean;
  } | null>(null);
  const [copied, setCopied] = useState(false);

  // Helper to generate a memorable secure password
  const generateSecurePassword = () => {
    const chars = '23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz';
    let rnd = '';
    for (let i = 0; i < 4; i++) {
      rnd += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return `Wariye@${rnd}`;
  };

  // Confirmation Modal
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    reporterId: string;
    action: 'deactivate' | 'activate' | 'delete';
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
      alert('Fadlan geli Magaca Buuxa iyo Username-ka (Gmail ama Nambarka Taleefanka).');
      return;
    }
    if (!password.trim() || password.trim().length < 6) {
      alert('Fadlan geli Password (ugu yaraan 6 xaraf) ah.');
      return;
    }

    const cleanUsername = phoneNumber.trim();
    const isEmail = cleanUsername.includes('@') && cleanUsername.includes('.');

    if (isEmail) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!emailRegex.test(cleanUsername)) {
        alert('Fadlan geli cinwaan Gmail/Email sax ah.');
        return;
      }
    } else {
      const cleanDigits = cleanUsername.replace(/[^0-9]/g, '');
      if (cleanDigits.length < 7) {
        alert('Fadlan geli nambar taleefan oo sax ah (ugu yaraan 7 lambar) ama Gmail.');
        return;
      }
    }

    setIsLoading(true);
    try {
      // Check for duplicate username / phone number in profiles
      const { data: existingProf } = await supabase
        .from('profiles')
        .select('id, full_name, phone_number')
        .or(`phone_number.eq.${cleanUsername},phone_number.ilike.%${cleanUsername}%`)
        .limit(1);

      if (existingProf && existingProf.length > 0) {
        alert(`Username-kan (${cleanUsername}) hore ayaa loogu diiwaangeliyay (${existingProf[0].full_name || 'isticmaale kale'}). Fadlan geli username kale.`);
        setIsLoading(false);
        return;
      }

      // Create Supabase Auth account for reporter
      // If email: use the email directly!
      // If phone: normalize to E.164-based phone email:
      let authEmail = '';
      if (isEmail) {
        authEmail = cleanUsername.toLowerCase();
      } else {
        const cleanDigits = cleanUsername.replace(/[^0-9]/g, '');
        let normPhone = cleanDigits;
        if (cleanDigits.startsWith('252') && cleanDigits.length >= 10) {
          normPhone = cleanDigits;
        } else if (cleanDigits.startsWith('0')) {
          normPhone = '252' + cleanDigits.slice(1);
        } else {
          normPhone = '252' + cleanDigits;
        }
        authEmail = `${normPhone}@phone.gobajaaj.so`;
      }

      const supabaseUrl = (import.meta as any).env?.VITE_SUPABASE_URL || '';
      const supabaseAnonKey = (import.meta as any).env?.VITE_SUPABASE_ANON_KEY || '';
      const tempClient = createClient(supabaseUrl, supabaseAnonKey, {
        auth: { persistSession: false }
      });

      const { data: authData, error: authErr } = await tempClient.auth.signUp({
        email: authEmail,
        password: password.trim(),
        options: {
          data: {
            full_name: fullName.trim(),
            phone: cleanUsername,
            role: 'reporter'
          }
        }
      });

      let resolvedUserId = authData?.user?.id;

      if (!resolvedUserId) {
        // If already registered in auth or signup didn't return user id:
        // Set their password directly via RPC
        await supabase.rpc('reset_user_password_by_phone', {
          p_phone: cleanUsername,
          p_full_name: fullName.trim(),
          p_new_password: password.trim()
        });

        const { data: existingProfData } = await supabase
          .from('profiles')
          .select('id')
          .eq('phone_number', cleanUsername)
          .maybeSingle();

        if (existingProfData?.id) {
          resolvedUserId = existingProfData.id;
        }
      }

      const newId = resolvedUserId || ((typeof crypto !== 'undefined' && crypto.randomUUID)
        ? crypto.randomUUID()
        : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
          const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
          return v.toString(16);
        }));

      // Upsert profile with clean username in phone_number field
      const { error: pErr } = await supabase.from('profiles').upsert([{
        id: newId,
        full_name: fullName.trim(),
        phone_number: cleanUsername,
        role: 'reporter',
        status: 'active'
      }], { onConflict: 'id' });

      if (pErr) {
        console.error('Error inserting reporter profile:', pErr.message);
        alert('Cilad ayaa dhacday marka la kaydinayay wariyaha: ' + pErr.message);
        setIsLoading(false);
        return;
      }

      let targetIntersectionId = selectedIntersectionId === 'NEW_CUSTOM' ? '' : selectedIntersectionId;
      let targetIntersectionName = '';

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
          targetIntersectionName = newIntData.name;
        }
      } else if (targetIntersectionId) {
        const intMatch = intersections.find(i => i.id === targetIntersectionId);
        targetIntersectionName = intMatch?.name || '';
      }

      // Assign intersection if selected or newly created
      if (targetIntersectionId) {
        await assignReporterToIntersection(newId, targetIntersectionId);
      }

      // Write Audit Log
      try {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'REPORTER_CREATED',
          target_type: 'reporter',
          target_id: newId,
          metadata: { full_name: fullName, username: cleanUsername, intersection_id: targetIntersectionId }
        }]);
      } catch (_) { }

      await fetchReportersAndIntersections();
      setShowAddModal(false);

      // Show beautiful success credentials modal
      setSuccessCredentials({
        fullName: fullName.trim(),
        username: cleanUsername,
        password: password.trim(),
        intersectionName: targetIntersectionName || 'Ma laha (Weli looma xilsaarin)'
      });

      setFullName('');
      setPhoneNumber('');
      setPassword('');
      setSelectedIntersectionId('');
      setCustomIntersectionName('');
    } catch (err: any) {
      alert('Cilad ayaa dhacday marka la kaydinayay wariyaha: ' + (err?.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetModal.reporter || !resetModal.newPassword.trim()) return;
    if (resetModal.newPassword.trim().length < 6) {
      alert('Password-ka waa inuu ugu yaraan 6 xaraf yahay.');
      return;
    }

    setIsLoading(true);
    try {
      const rep = resetModal.reporter;
      const cleanUsername = (rep.phone_number || '').trim();
      const isEmail = cleanUsername.includes('@');

      let authEmail = '';
      if (isEmail) {
        authEmail = cleanUsername.toLowerCase();
      } else {
        const cleanDigits = cleanUsername.replace(/[^0-9]/g, '');
        let normPhone = cleanDigits;
        if (cleanDigits.startsWith('252') && cleanDigits.length >= 10) {
          normPhone = cleanDigits;
        } else if (cleanDigits.startsWith('0')) {
          normPhone = '252' + cleanDigits.slice(1);
        } else {
          normPhone = '252' + cleanDigits;
        }
        authEmail = `${normPhone}@phone.gobajaaj.so`;
      }

      // 1. Call reset_user_password_by_phone RPC
      const { data: rpcRes, error: rpcErr } = await supabase.rpc('reset_user_password_by_phone', {
        p_phone: cleanUsername,
        p_full_name: rep.full_name || '',
        p_new_password: resetModal.newPassword.trim()
      });

      if (rpcErr || (rpcRes && rpcRes.success === false)) {
        const errMsg = rpcErr?.message || rpcRes?.message || 'Furaha sirta ah lama beddeli karin.';
        alert('Cilad: ' + errMsg);
        setIsLoading(false);
        return;
      }

      // 2. Also ensure auth profile or user is created if not yet
      const supabaseUrl = (import.meta as any).env?.VITE_SUPABASE_URL || '';
      const supabaseAnonKey = (import.meta as any).env?.VITE_SUPABASE_ANON_KEY || '';
      const tempClient = createClient(supabaseUrl, supabaseAnonKey, {
        auth: { persistSession: false }
      });
      await tempClient.auth.signUp({
        email: authEmail,
        password: resetModal.newPassword.trim(),
        options: {
          data: {
            full_name: rep.full_name,
            phone: cleanUsername,
            role: 'reporter'
          }
        }
      }).catch(() => { });

      try {
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'REPORTER_PASSWORD_RESET',
          target_type: 'reporter',
          target_id: rep.id,
          metadata: { reporter_name: rep.full_name, username: cleanUsername }
        }]);
      } catch (_) { }

      const newPass = resetModal.newPassword.trim();
      setResetModal({ isOpen: false, reporter: null, newPassword: '' });

      setSuccessCredentials({
        fullName: rep.full_name,
        username: cleanUsername,
        password: newPass,
        intersectionName: 'Akoon hore',
        isReset: true
      });
    } catch (err: any) {
      alert('Cilad ayaa dhacday marka la beddelayay furaha: ' + (err?.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  const copyCredentialsToClipboard = () => {
    if (!successCredentials) return;
    const text = `GoBajaaj - Xogta Wariyaha Saxmadda:\n👤 Magaca: ${successCredentials.fullName}\n${successCredentials.username.includes('@') ? '📧' : '📱'} Username: ${successCredentials.username}\n🔑 Password: ${successCredentials.password}\n📍 Isgoyska: ${successCredentials.intersectionName}\n\nFadlan ku gal app-ka GoBajaaj qaybta Wariyaha.`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
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

  const handleConfirmReporterAction = async () => {
    const { reporterId, action, name } = confirmModal;
    setIsLoading(true);
    try {
      if (action === 'delete') {
        // 1. Remove intersection assignment
        await supabase
          .from('intersections')
          .update({ assigned_reporter_id: null })
          .eq('assigned_reporter_id', reporterId);

        // 2. Delete profile
        const { error: delErr } = await supabase
          .from('profiles')
          .delete()
          .eq('id', reporterId);

        if (delErr) {
          alert('Cilad tirtirista wariyaha: ' + delErr.message);
        }

        // 3. Write audit log
        try {
          await supabase.from('audit_logs').insert([{
            actor_name: actorName,
            action: 'REPORTER_DELETED',
            target_type: 'reporter',
            target_id: reporterId,
            metadata: { name }
          }]);
        } catch (_) { }
      } else {
        const nextStatus = action === 'activate' ? 'active' : 'disabled';
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
      }

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
              onClick={() => {
                setFullName('');
                setPhoneNumber('');
                setPassword('');
                setSelectedIntersectionId('');
                setCustomIntersectionName('');
                setShowAddModal(true);
              }}
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
                  <th className="p-4 font-bold">Username (Gmail / Tel)</th>
                  <th className="p-4 font-bold">Isgoyska Loo Xilsaaray (1 Active Rule)</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  {canManage && <th className="p-4 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {reporters.map((rep) => {
                  const assignedInt = intersections.find(i => i.assigned_reporter_id === rep.id);
                  const isEmailUser = (rep.phone_number || '').includes('@');
                  return (
                    <tr key={rep.id} className="hover:bg-neutral-50 transition">
                      <td className="p-4 font-bold text-black flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-[#E50914] text-white flex items-center justify-center text-xs font-black shrink-0">
                          {rep.full_name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-black">{rep.full_name}</p>
                          <p className="text-[11px] text-neutral-400 font-mono">ID: {rep.id.slice(0, 8)}...</p>
                        </div>
                      </td>
                      <td className="p-4 font-mono text-xs">
                        <div className="flex items-center space-x-1.5">
                          {isEmailUser ? (
                            <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-blue-50 text-blue-700 text-xs font-bold border border-blue-200">
                              <Mail className="w-3.5 h-3.5 text-blue-600" />
                              <span>{rep.phone_number}</span>
                            </span>
                          ) : (
                            <span className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-neutral-100 text-neutral-800 text-xs font-bold border border-neutral-200">
                              <Phone className="w-3.5 h-3.5 text-neutral-600" />
                              <span>{rep.phone_number}</span>
                            </span>
                          )}
                        </div>
                      </td>
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
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              onClick={() => {
                                setResetModal({
                                  isOpen: true,
                                  reporter: rep,
                                  newPassword: ''
                                });
                              }}
                              className="px-2.5 py-1.5 text-xs font-bold rounded-xl border border-blue-200 text-blue-600 hover:bg-blue-50 transition cursor-pointer flex items-center space-x-1"
                              title="Bedel Furaha Sirta ah"
                            >
                              <KeyRound className="w-3.5 h-3.5" />
                              <span>Furaha</span>
                            </button>

                            <button
                              onClick={() => setConfirmModal({
                                isOpen: true,
                                reporterId: rep.id,
                                action: rep.status === 'active' ? 'deactivate' : 'activate',
                                name: rep.full_name
                              })}
                              className={`px-3 py-1.5 text-xs font-bold rounded-xl border transition cursor-pointer ${rep.status === 'active'
                                ? 'border-amber-200 text-amber-700 hover:bg-amber-50'
                                : 'border-emerald-200 text-emerald-600 hover:bg-emerald-50'
                                }`}
                            >
                              {rep.status === 'active' ? 'Deactivate' : 'Activate'}
                            </button>

                            <button
                              onClick={() => setConfirmModal({
                                isOpen: true,
                                reporterId: rep.id,
                                action: 'delete',
                                name: rep.full_name
                              })}
                              className="px-2.5 py-1.5 text-xs font-bold rounded-xl border border-red-200 text-red-600 hover:bg-red-50 transition cursor-pointer flex items-center space-x-1"
                              title="Tirtir Wariyaha"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                              <span>Tirtir</span>
                            </button>
                          </div>
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
              <div className="flex items-center space-x-2">
                <UserPlus className="w-5 h-5 text-[#E50914]" />
                <h3 className="font-black text-lg text-black">Ku Dar Wariye Cusub</h3>
              </div>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-400 hover:text-black">✕</button>
            </div>

            <form onSubmit={handleCreateReporter} className="p-6 space-y-4" autoComplete="off">
              {/* Dummy hidden inputs to stop aggressive Chrome/browser autofill */}
              <input type="text" name="fake_user_block" style={{ display: 'none' }} tabIndex={-1} autoComplete="off" />
              <input type="password" name="fake_pass_block" style={{ display: 'none' }} tabIndex={-1} autoComplete="off" />

              <div>
                <label className="block text-xs font-bold text-black mb-1">Magaca Buuxa ee Wariyaha</label>
                <input
                  type="text"
                  name="reporter_full_name"
                  id="reporter_full_name"
                  autoComplete="off"
                  required
                  placeholder="m.g. Maxamed Cali Shire"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-black">
                    Username ee Login-ka (Gmail ama Taleefan)
                  </label>
                  {phoneNumber.trim() && (
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${phoneNumber.includes('@') ? 'bg-blue-100 text-blue-800' : 'bg-emerald-100 text-emerald-800'}`}>
                      {phoneNumber.includes('@') ? '📧 Gmail Account' : '📱 Taleefan Soomaali'}
                    </span>
                  )}
                </div>
                <input
                  type="text"
                  name="reporter_login_username"
                  id="reporter_login_username"
                  autoComplete="off"
                  required
                  placeholder="m.g. wariye@gmail.com ama 61XXXXXXX"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono"
                />
                <p className="text-[11px] text-neutral-500 mt-1">
                  💡 Wariyuhu wuxuu app-ka ku gali karaa Gmail-kiisa tooska ah ama lambarkiisa taleefanka.
                </p>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-black">Password (ugu yaraan 6 xaraf)</label>
                  <button
                    type="button"
                    onClick={() => setPassword(generateSecurePassword())}
                    className="text-[11px] font-bold text-[#E50914] hover:underline flex items-center space-x-1 cursor-pointer"
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>🎲 Dhali Password (Auto)</span>
                  </button>
                </div>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="reporter_secret_password"
                    id="reporter_secret_password"
                    autoComplete="new-password"
                    required
                    minLength={6}
                    placeholder="Qor password ama riix 'Dhali Password'..."
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
                <p className="text-[11px] text-neutral-500 mt-1">
                  🔑 Waxaad qori kartaa password kasta ama badhanka kor riix si automatic loogu sameeyo.
                </p>
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
                  Xusuusin: Sharciga nidaamku waa 1 Wariye Isgoyskiiba.
                </p>
              </div>

              <div className="pt-4 border-t border-neutral-200 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 text-sm font-bold text-neutral-600 hover:text-black cursor-pointer"
                >
                  Ka noqo
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-5 py-2 bg-[#E50914] text-white text-sm font-bold rounded-xl shadow hover:bg-[#B91C1C] transition cursor-pointer"
                >
                  {isLoading ? 'Kaydinaya...' : 'Kaydi Wariyaha'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Password Reset Modal */}
      {resetModal.isOpen && resetModal.reporter && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden">
            <div className="p-6 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
              <div className="flex items-center space-x-2">
                <KeyRound className="w-5 h-5 text-blue-600" />
                <h3 className="font-black text-lg text-black">Bedel Furaha Wariyaha</h3>
              </div>
              <button
                onClick={() => setResetModal({ isOpen: false, reporter: null, newPassword: '' })}
                className="text-neutral-400 hover:text-black"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleResetPasswordSubmit} className="p-6 space-y-4" autoComplete="off">
              {/* Dummy hidden inputs to block browser autofill */}
              <input type="text" name="fake_user_block_reset" style={{ display: 'none' }} tabIndex={-1} autoComplete="off" />
              <input type="password" name="fake_pass_block_reset" style={{ display: 'none' }} tabIndex={-1} autoComplete="off" />

              <div className="p-3 bg-neutral-100 rounded-xl">
                <p className="text-xs text-neutral-500">Wariyaha la beddelayo:</p>
                <p className="text-sm font-bold text-black">{resetModal.reporter.full_name}</p>
                <p className="text-xs font-mono text-neutral-600 mt-0.5">Username: {resetModal.reporter.phone_number}</p>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-black">Furaha Sirta ah ee Cusub</label>
                  <button
                    type="button"
                    onClick={() => setResetModal(s => ({ ...s, newPassword: generateSecurePassword() }))}
                    className="text-[11px] font-bold text-blue-600 hover:underline flex items-center space-x-1 cursor-pointer"
                  >
                    <Sparkles className="w-3 h-3" />
                    <span>🎲 Dhali Password (Auto)</span>
                  </button>
                </div>
                <div className="relative">
                  <input
                    type={showResetPassword ? 'text' : 'password'}
                    name="reset_reporter_new_password"
                    id="reset_reporter_new_password"
                    autoComplete="new-password"
                    required
                    minLength={6}
                    placeholder="Geli furaha sirta ah ee cusub..."
                    value={resetModal.newPassword}
                    onChange={(e) => setResetModal(s => ({ ...s, newPassword: e.target.value }))}
                    className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-blue-600 focus:outline-none text-black font-mono pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowResetPassword(!showResetPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-black cursor-pointer"
                  >
                    {showResetPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                <div className="p-3 bg-blue-50 border border-blue-200 rounded-xl text-xs text-blue-900 mt-2">
                  <p className="font-bold flex items-center gap-1">
                    <span>🔒</span>
                    <span>Xogta Amniga:</span>
                  </p>
                  <p className="mt-0.5 text-blue-800">
                    Furaha hore ee wariyaha waa xafidan yahay (encrypted). Halkan ku qor furaha <strong>CUSUB</strong> ee aad rabto inaad u dejiso (tusaale: 112233) ama riix badhanka "🎲 Dhali Password".
                  </p>
                </div>
              </div>

              <div className="pt-4 border-t border-neutral-200 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setResetModal({ isOpen: false, reporter: null, newPassword: '' })}
                  className="px-4 py-2 text-sm font-bold text-neutral-600 hover:text-black cursor-pointer"
                >
                  Ka noqo
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-5 py-2 bg-blue-600 text-white text-sm font-bold rounded-xl shadow hover:bg-blue-700 transition cursor-pointer"
                >
                  {isLoading ? 'Beddelaya...' : 'Xaqiiji & Bedel'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Credentials Success Modal with One-Click Copy */}
      {successCredentials && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden">
            <div className="p-6 bg-gradient-to-r from-emerald-600 to-teal-600 text-white">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
                  <Check className="w-5 h-5 text-white" />
                </div>
                <h3 className="font-black text-lg">
                  {successCredentials.isReset ? 'Furaha Wariyaha Waa La Beddelay!' : 'Wariyaha Si Guul Leh Ayaa Loo Abuuray!'}
                </h3>
              </div>
              <p className="text-xs text-white/80 mt-1">
                Kani waa faahfaahinta xogta login-ka ee wariyaha. Fadlan nuqul ka qaad oo u dir.
              </p>
            </div>

            <div className="p-6 space-y-3">
              <div className="bg-neutral-50 border border-neutral-200 rounded-xl p-3.5 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-500">Magaca:</span>
                  <span className="font-bold text-black">{successCredentials.fullName}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-500">Username:</span>
                  <span className="font-bold text-black font-mono">{successCredentials.username}</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-neutral-500">Password:</span>
                  <span className="font-bold text-[#E50914] font-mono text-sm bg-red-50 px-2 py-0.5 rounded border border-red-200">
                    {successCredentials.password}
                  </span>
                </div>
                {!successCredentials.isReset && (
                  <div className="flex items-center justify-between text-xs pt-1 border-t border-neutral-200">
                    <span className="text-neutral-500">Isgoyska:</span>
                    <span className="font-bold text-emerald-700">{successCredentials.intersectionName}</span>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={copyCredentialsToClipboard}
                className="w-full py-2.5 bg-neutral-900 text-white rounded-xl text-xs font-bold hover:bg-black transition flex items-center justify-center space-x-2 cursor-pointer shadow-sm"
              >
                {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
                <span>{copied ? '✅ Waa La Koobiyeeyay Xogtii!' : '📋 Koobiye Xogta Login-ka (WhatsApp / SMS)'}</span>
              </button>

              <button
                type="button"
                onClick={() => setSuccessCredentials(null)}
                className="w-full py-2 text-xs font-bold text-neutral-600 hover:text-black cursor-pointer border border-neutral-200 rounded-xl"
              >
                Xir
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Action Confirmation Modal */}
      <ConfirmationModal
        isOpen={confirmModal.isOpen}
        title={confirmModal.action === 'delete' ? 'Tirtir Wariyaha (Delete)' : 'Badelidda Xaaladda Wariyaha'}
        message={
          confirmModal.action === 'delete'
            ? `Ma hubtaa inaad tirtirto wariyaha "${confirmModal.name}"?\nAkoonka iyo xogta wariyaha gabi ahaanba waa la tirtiri doonaa.`
            : `Ma hubtaa inaad ${confirmModal.action === 'deactivate' ? 'joojiso (deactivate)' : 'furto (activate)'} wariyaha "${confirmModal.name}"?`
        }
        confirmLabel={confirmModal.action === 'delete' ? 'Haa, Tirtir' : 'Xaqiiji'}
        cancelLabel="Jooji"
        isDanger={confirmModal.action === 'delete' || confirmModal.action === 'deactivate'}
        isLoading={isLoading}
        onConfirm={handleConfirmReporterAction}
        onCancel={() => setConfirmModal({ isOpen: false, reporterId: '', action: 'deactivate', name: '' })}
      />
    </div>
  );
};
