import React, { useState, useEffect } from 'react';
import { UserCheck, UserPlus, RefreshCw, AlertCircle, Shield, Edit, KeyRound } from 'lucide-react';
import { createClient } from '@supabase/supabase-js';
import { AdminUser, AdminRole } from '../types';
import { ADMIN_ROLES, getRoleDisplay } from '../rbac';
import { supabase } from '../supabase';
import { ConfirmationModal } from './ConfirmationModal';

interface AdminManagementProps {
  currentAdminRole: AdminRole;
  actorName: string;
}

export const AdminManagement: React.FC<AdminManagementProps> = ({ currentAdminRole, actorName }) => {
  const [admins, setAdmins] = useState<AdminUser[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [selectedAdmin, setSelectedAdmin] = useState<AdminUser | null>(null);
  
  // Form State
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [roleKey, setRoleKey] = useState<AdminRole>('analyst');
  const [status, setStatus] = useState<'active' | 'suspended' | 'disabled'>('active');

  // Password reset modal state
  const [resetModal, setResetModal] = useState<{
    isOpen: boolean;
    admin: AdminUser | null;
    newPassword: string;
  }>({ isOpen: false, admin: null, newPassword: '' });

  // Confirmation state
  const [confirmState, setConfirmState] = useState<{
    isOpen: boolean;
    adminId: string;
    action: 'suspend' | 'disable' | 'activate';
    name: string;
  }>({ isOpen: false, adminId: '', action: 'suspend', name: '' });

  const isSuperAdmin = currentAdminRole === 'super_admin';

  const fetchAdmins = async (isSilent = false) => {
    if (!isSilent) setIsLoading(true);
    setErrorMsg(null);
    try {
      const combinedMap = new Map<string, AdminUser>();

      // 1. Try querying admin_users table
      const { data, error } = await supabase
        .from('admin_users')
        .select('*')
        .order('created_at', { ascending: false });

      if (data && data.length > 0) {
        data.forEach((a: any) => {
          combinedMap.set(a.id || a.user_id, a as AdminUser);
        });
      }

      // 2. Also query profiles table for any admin accounts
      const { data: profData, error: profErr } = await supabase
        .from('profiles')
        .select('*')
        .in('role', ['super_admin', 'payment_admin', 'traffic_admin', 'reporter_manager', 'support_agent', 'analyst', 'admin']);

      if (profData && profData.length > 0) {
        profData.forEach((p: any) => {
          if (!combinedMap.has(p.id)) {
            combinedMap.set(p.id, {
              id: p.id,
              user_id: p.id,
              username: p.username || p.phone_number || p.email?.split('@')[0] || p.full_name?.toLowerCase().replace(/\s+/g, '_') || 'admin',
              email: p.email || `${p.id.slice(0, 8)}@somalitraffic.so`,
              full_name: p.full_name || 'Admin',
              phone: p.phone_number || '',
              role_key: (p.role as AdminRole) || 'analyst',
              status: p.status === 'disabled' ? 'disabled' : (p.status === 'suspended' ? 'suspended' : 'active'),
              created_at: p.created_at
            });
          }
        });
      }

      if (combinedMap.size === 0 && (error || profErr)) {
        if (error && profErr) {
          setErrorMsg('Cilad ayaa ka dhacday xogta maamulayaasha: ' + (error?.message || profErr?.message));
        }
      }

      setAdmins(Array.from(combinedMap.values()));
    } catch (err: any) {
      setErrorMsg('Cilad ayaa ka dhacday xogta: ' + (err?.message || 'Dib u tijaabi.'));
    } finally {
      if (!isSilent) setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAdmins(false);

    const interval = setInterval(() => {
      fetchAdmins(true);
    }, 10000);

    return () => clearInterval(interval);
  }, []);

  const handleSaveAdmin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isSuperAdmin) {
      alert('Keliya Super Admin / Owner ayaa samayn kara ama wax ka beddeli kara Admin.');
      return;
    }

    if (!fullName.trim() || !username.trim()) {
      alert('Fadlan geli Magaca Buuxa iyo Username-ka.');
      return;
    }

    if (!selectedAdmin && !password.trim()) {
      alert('Fadlan geli Furaha sirta ah (Password) ee admin-ka cusub.');
      return;
    }

    setIsLoading(true);
    try {
      const cleanUsername = username.trim().toLowerCase();
      const targetEmail = email.trim() || `${cleanUsername}@somalitraffic.so`;

      if (selectedAdmin) {
        // Update existing admin
        const { error } = await supabase
          .from('admin_users')
          .update({
            full_name: fullName.trim(),
            username: cleanUsername,
            phone: phone.trim(),
            email: targetEmail,
            role_key: roleKey,
            status: status,
            updated_at: new Date().toISOString()
          })
          .eq('id', selectedAdmin.id);

        if (error) {
          await supabase.from('profiles').update({
            full_name: fullName.trim(),
            phone_number: phone.trim(),
            role: roleKey,
            status: status
          }).eq('id', selectedAdmin.user_id);
        }

        // Audit Log
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'ADMIN_UPDATED',
          target_type: 'admin_user',
          target_id: selectedAdmin.id,
          metadata: { username: cleanUsername, role: roleKey, status: status, name: fullName }
        }]);

      } else {
        // Create Real Auth Account using an unpersisted Supabase client
        const supabaseUrl = (import.meta as any).env?.VITE_SUPABASE_URL || 'https://dummy.supabase.co';
        const supabaseAnonKey = (import.meta as any).env?.VITE_SUPABASE_ANON_KEY || 'dummy';

        const tempClient = createClient(supabaseUrl, supabaseAnonKey, {
          auth: { persistSession: false }
        });

        const { data: authData, error: authErr } = await tempClient.auth.signUp({
          email: targetEmail,
          password: password.trim(),
          options: {
            data: {
              full_name: fullName.trim(),
              username: cleanUsername,
              phone: phone.trim(),
              role: roleKey
            }
          }
        });

        const authUserId = authData?.user?.id || ((typeof crypto !== 'undefined' && crypto.randomUUID) 
          ? crypto.randomUUID() 
          : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
              const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
              return v.toString(16);
            }));

        if (authErr && !authErr.message?.includes('already registered')) {
          console.warn('Auth creation notice:', authErr.message);
        }

        // 1. Insert record into admin_users table
        const { error: dbErr } = await supabase.from('admin_users').insert([{
          id: authUserId,
          user_id: authUserId,
          username: cleanUsername,
          email: targetEmail,
          full_name: fullName.trim(),
          phone: phone.trim(),
          role_key: roleKey,
          status: status
        }]);

        // 2. Also ensure profile exists in profiles table
        const { error: pErr } = await supabase.from('profiles').upsert([{
          id: authUserId,
          full_name: fullName.trim(),
          phone_number: phone.trim() || cleanUsername,
          role: roleKey,
          status: status
        }]);

        if (dbErr && pErr) {
          console.error('Both admin_users and profiles insert failed:', dbErr.message, pErr.message);
          alert(`Cilad ayaa dhacday: ${dbErr.message || pErr.message}\nFadlan dib u hubi xogta.`);
          setIsLoading(false);
          return;
        }

        // Audit Log
        await supabase.from('audit_logs').insert([{
          actor_name: actorName,
          action: 'ADMIN_CREATED',
          target_type: 'admin_user',
          target_id: authUserId,
          metadata: { username: cleanUsername, role: roleKey, phone: phone, name: fullName }
        }]);
      }

      await fetchAdmins();
      setShowModal(false);
      resetForm();
    } catch (err: any) {
      alert('Cilad ayaa dhacday marka la kaydinayay admin-ka: ' + (err?.message || ''));
    } finally {
      setIsLoading(false);
    }
  };

  const handleExecutePasswordReset = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!resetModal.admin || !resetModal.newPassword.trim()) return;

    setIsLoading(true);
    try {
      // Trigger reset email or update audit log for password reset
      await supabase.auth.resetPasswordForEmail(resetModal.admin.email);

      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: 'ADMIN_PASSWORD_RESET',
        target_type: 'admin_user',
        target_id: resetModal.admin.id,
        metadata: { admin_username: resetModal.admin.username, admin_email: resetModal.admin.email }
      }]);

      alert(`Codsiga furaha sirta ah ee ${resetModal.admin.full_name} waa la socodsiiyay!`);
      setResetModal({ isOpen: false, admin: null, newPassword: '' });
    } catch (err) {
      alert('Cilad ayaa dhacday marka furaha sirta ah la beddelayay.');
    } finally {
      setIsLoading(false);
    }
  };

  const executeStatusChange = async () => {
    const { adminId, action } = confirmState;
    const nextStatus = action === 'activate' ? 'active' : (action === 'suspend' ? 'suspended' : 'disabled');
    setIsLoading(true);
    try {
      await supabase.from('admin_users').update({ status: nextStatus, updated_at: new Date().toISOString() }).eq('id', adminId);
      await supabase.from('profiles').update({ status: nextStatus }).eq('id', adminId);

      const actionType = nextStatus === 'suspended' ? 'ADMIN_SUSPENDED' : (nextStatus === 'active' ? 'ADMIN_ACTIVATED' : 'ADMIN_DISABLED');

      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: actionType,
        target_type: 'admin_user',
        target_id: adminId,
        metadata: { new_status: nextStatus }
      }]);

      await fetchAdmins();
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
      setConfirmState({ isOpen: false, adminId: '', action: 'suspend', name: '' });
    }
  };

  const resetForm = () => {
    setSelectedAdmin(null);
    setFullName('');
    setUsername('');
    setPhone('');
    setEmail('');
    setPassword('');
    setRoleKey('analyst');
    setStatus('active');
  };

  const openEdit = (admin: AdminUser) => {
    setSelectedAdmin(admin);
    setFullName(admin.full_name);
    setUsername(admin.username || '');
    setPhone(admin.phone || '');
    setEmail(admin.email);
    setPassword('');
    setRoleKey(admin.role_key);
    setStatus(admin.status);
    setShowModal(true);
  };

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <Shield className="w-6 h-6 text-[#E50914]" />
            <h2 className="text-xl font-black text-black">Maamulayaasha Nidaamka (Admin Management)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Maamul xilalka 6-da ah ee rasmiga ah (Super Admin/Owner, Payment Admin, Traffic Admin, Reporter Manager, Support Agent, Analyst).
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button 
            onClick={fetchAdmins}
            disabled={isLoading}
            className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
          </button>
          
          {isSuperAdmin && (
            <button 
              onClick={() => { resetForm(); setShowModal(true); }}
              className="px-4 py-2.5 bg-[#E50914] text-white rounded-xl text-sm font-bold shadow hover:bg-[#B91C1C] transition flex items-center space-x-2 cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span>+ Ku Dar Admin</span>
            </button>
          )}
        </div>
      </div>

      {!isSuperAdmin && (
        <div className="p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
          <span>Fiiro gaar ah: Keliya OWNER / SUPER ADMIN ayaa samayn kara ama beddeli kara xilalka admin-yada. Waxaad ku jirtaa habka akhriska keliya.</span>
        </div>
      )}

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya maamulayaasha...</p>
          </div>
        ) : admins.length === 0 ? (
          <div className="p-12 text-center">
            <UserCheck className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Xog lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1 mb-4">Ma jiro admin ku jira nidaamka hadda.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm whitespace-nowrap">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Magaca Admin</th>
                  <th className="p-4 font-bold">Username</th>
                  <th className="p-4 font-bold">Taleefanka</th>
                  <th className="p-4 font-bold">Xilka (Role)</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  <th className="p-4 font-bold">Galkii Ugu Dambeeyay</th>
                  <th className="p-4 font-bold">Taariikhda</th>
                  {isSuperAdmin && <th className="p-4 font-bold text-right">Ficil</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {admins.map((adm) => {
                  const roleDisplay = getRoleDisplay(adm.role_key);
                  return (
                    <tr key={adm.id} className="hover:bg-neutral-50 transition">
                      <td className="p-4 font-bold text-black flex items-center space-x-3">
                        <div className="w-8 h-8 rounded-full bg-neutral-900 text-white flex items-center justify-center text-xs font-black shrink-0">
                          {adm.full_name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-black">{adm.full_name}</p>
                          <p className="text-[11px] text-neutral-400 font-mono">{adm.email}</p>
                        </div>
                      </td>
                      <td className="p-4 text-black font-mono text-xs font-bold">
                        {adm.username || '-'}
                      </td>
                      <td className="p-4 text-neutral-700 text-xs font-mono">
                        {adm.phone || '-'}
                      </td>
                      <td className="p-4">
                        <span className={`px-3 py-1 rounded-xl text-xs font-black uppercase tracking-wider ${roleDisplay.bg} ${roleDisplay.text}`}>
                          {roleDisplay.name} ({roleDisplay.nameSomali})
                        </span>
                      </td>
                      <td className="p-4">
                        <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          adm.status === 'active' ? 'bg-emerald-100 text-emerald-800' :
                          adm.status === 'suspended' ? 'bg-amber-100 text-amber-800' :
                          'bg-red-100 text-red-800'
                        }`}>
                          {adm.status === 'active' ? '🟢 ACTIVE' : adm.status === 'suspended' ? '🟡 SUSPENDED' : '🔴 DISABLED'}
                        </span>
                      </td>
                      <td className="p-4 text-neutral-500 font-mono text-xs">
                        {adm.last_login ? adm.last_login.replace('T', ' ').slice(0, 16) : 'Lama galin'}
                      </td>
                      <td className="p-4 text-neutral-500 font-mono text-xs">
                        {adm.created_at ? adm.created_at.slice(0, 10) : '-'}
                      </td>
                      {isSuperAdmin && (
                        <td className="p-4 text-right space-x-2">
                          <button
                            onClick={() => openEdit(adm)}
                            className="p-1.5 text-neutral-600 hover:text-black hover:bg-neutral-100 rounded-lg transition inline-block cursor-pointer"
                            title="Wax ka beddel Admin"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setResetModal({ isOpen: true, admin: adm, newPassword: '' })}
                            className="p-1.5 text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded-lg transition inline-block cursor-pointer"
                            title="Sii Password Cusub"
                          >
                            <KeyRound className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setConfirmState({
                              isOpen: true,
                              adminId: adm.id,
                              action: adm.status === 'active' ? 'suspend' : 'activate',
                              name: adm.full_name
                            })}
                            className={`px-2.5 py-1 text-xs font-bold rounded-lg border transition cursor-pointer ${
                              adm.status === 'active' ? 'border-amber-200 text-amber-700 hover:bg-amber-50' : 'border-emerald-200 text-emerald-700 hover:bg-emerald-50'
                            }`}
                          >
                            {adm.status === 'active' ? 'Haki' : 'Furey'}
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

      {/* Modal Add/Edit Admin */}
      {showModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden">
            <div className="p-6 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
              <h3 className="font-black text-lg text-black">
                {selectedAdmin ? 'Wax ka beddel Admin' : 'Ku Dar Admin Cusub'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-neutral-400 hover:text-black font-bold">✕</button>
            </div>

            <form onSubmit={handleSaveAdmin} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-black mb-1">Magaca Buuxa (Full Name)</label>
                <input
                  type="text"
                  required
                  placeholder="m.g. Eng. Axmed Siyaad"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Username-ka (Admin Username)</label>
                <input
                  type="text"
                  required
                  placeholder="m.g. ahmed_admin"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Taleefanka (Phone Number)</label>
                <input
                  type="text"
                  placeholder="+252 61XXXXXXX"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Email-ka (Akkawnka Auth)</label>
                <input
                  type="email"
                  placeholder="admin@somalitraffic.so"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black font-mono"
                />
              </div>

              {!selectedAdmin && (
                <div>
                  <label className="block text-xs font-bold text-black mb-1">Furaha Sirta ah (Password)</label>
                  <input
                    type="password"
                    required
                    placeholder="••••••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black"
                  />
                </div>
              )}

              <div>
                <label className="block text-xs font-bold text-black mb-1">Xilka Maamulka (Admin Role)</label>
                <select
                  value={roleKey}
                  onChange={(e) => setRoleKey(e.target.value as AdminRole)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black bg-white font-bold"
                >
                  <option value="super_admin">SUPER ADMIN (Fasax Buuxa / Owner)</option>
                  <option value="payment_admin">PAYMENT ADMIN (Ansixinta Driver Pass)</option>
                  <option value="traffic_admin">TRAFFIC ADMIN (Isgoysyada & Saxmadda)</option>
                  <option value="reporter_manager">REPORTER MANAGER (Wariyeyaasha)</option>
                  <option value="support_agent">SUPPORT AGENT (Taageerada Macaamiisha)</option>
                  <option value="analyst">ANALYST (Read-Only Analytics)</option>
                </select>
                <p className="text-[11px] text-neutral-500 mt-1">
                  {ADMIN_ROLES[roleKey]?.description}
                </p>
              </div>

              <div>
                <label className="block text-xs font-bold text-black mb-1">Xaaladda (Status)</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as any)}
                  className="w-full px-3.5 py-2.5 text-sm border border-neutral-300 rounded-xl focus:border-[#E50914] focus:outline-none text-black bg-white font-bold"
                >
                  <option value="active">ACTIVE (🟢 Furan)</option>
                  <option value="suspended">SUSPENDED (🟡 La Hakiye)</option>
                  <option value="disabled">DISABLED (🔴 La Xiray)</option>
                </select>
              </div>

              <div className="pt-4 border-t border-neutral-200 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm font-bold text-neutral-600 hover:text-black"
                >
                  Ka noqo
                </button>
                <button
                  type="submit"
                  disabled={isLoading}
                  className="px-5 py-2 bg-[#E50914] text-white text-sm font-bold rounded-xl shadow hover:bg-[#B91C1C] cursor-pointer"
                >
                  Kaydi Admin
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Password Reset Modal */}
      {resetModal.isOpen && resetModal.admin && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl max-w-sm w-full border border-neutral-200 p-6 space-y-4">
            <h3 className="font-black text-base text-black flex items-center space-x-2">
              <KeyRound className="w-5 h-5 text-blue-600" />
              <span>Sii Password Cusub: {resetModal.admin.full_name}</span>
            </h3>
            <p className="text-xs text-neutral-600">
              U dir xiriirka beddelidda password-ka ama dib u cusboonaysii akkawnka maamulaha <strong>{resetModal.admin.username}</strong>.
            </p>
            <div className="flex justify-end space-x-3 pt-2">
              <button
                onClick={() => setResetModal({ isOpen: false, admin: null, newPassword: '' })}
                className="px-3.5 py-2 text-xs font-bold text-neutral-600"
              >
                Ka noqo
              </button>
              <button
                onClick={handleExecutePasswordReset}
                disabled={isLoading}
                className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl shadow hover:bg-blue-700 cursor-pointer"
              >
                U dir Reset Link
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Modal */}
      <ConfirmationModal
        isOpen={confirmState.isOpen}
        title="Badelidda Xaaladda Admin"
        message={`Ma hubtaa inaad beddelo xaaladda admin-ka: "${confirmState.name}"?`}
        confirmLabel="Xaqiiji"
        cancelLabel="Jooji"
        isDanger={confirmState.action !== 'activate'}
        isLoading={isLoading}
        onConfirm={executeStatusChange}
        onCancel={() => setConfirmState({ isOpen: false, adminId: '', action: 'suspend', name: '' })}
      />
    </div>
  );
};

