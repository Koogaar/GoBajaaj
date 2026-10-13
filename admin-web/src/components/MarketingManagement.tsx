import React, { useState, useEffect, useCallback } from 'react';
import {
  Megaphone,
  UserPlus,
  RefreshCw,
  AlertCircle,
  Copy,
  Check,
  Eye,
  EyeOff,
  ShieldOff,
  ShieldCheck,
  Trash2,
  KeyRound,
  X
} from 'lucide-react';
import { supabase } from '../supabase';
import { MarketingAgent } from '../types';

interface MarketingManagementProps {
  canManage: boolean;
  actorName: string;
}

function generateCode(): string {
  const prefix = 'MKT';
  const num = Math.floor(10 + Math.random() * 90);
  const suffix = Math.random().toString(36).substring(2, 4).toUpperCase();
  return `${prefix}-${num}${suffix}`;
}

function generatePassword(length = 10): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#!';
  return Array.from({ length }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
}

function sanitizeUsername(val: string): string {
  return val.toLowerCase().replace(/[^a-z0-9._-]/g, '');
}

export const MarketingManagement: React.FC<MarketingManagementProps> = ({ canManage, actorName }) => {
  const [agents, setAgents] = useState<MarketingAgent[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const [showCreateForm, setShowCreateForm] = useState(false);
  const [fullName, setFullName] = useState('');
  const [username, setUsername] = useState('');
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState(generateCode());
  const [password, setPassword] = useState(generatePassword());
  const [showPassword, setShowPassword] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const [copiedId, setCopiedId] = useState<string | null>(null);

  const [resetModal, setResetModal] = useState<{
    isOpen: boolean;
    agent: MarketingAgent | null;
    newPassword: string;
    showPw: boolean;
  }>({ isOpen: false, agent: null, newPassword: '', showPw: false });

  const [confirmState, setConfirmState] = useState<{
    isOpen: boolean;
    agentId: string;
    agentName: string;
    action: 'suspend' | 'activate' | 'delete';
  }>({ isOpen: false, agentId: '', agentName: '', action: 'suspend' });

  const fetchAgents = useCallback(async (silent = false) => {
    if (!silent) setIsLoading(true);
    setErrorMsg(null);
    try {
      const { data, error } = await supabase
        .from('marketing_staff')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) {
        if (error.message?.includes('does not exist') || error.code === '42P01') {
          setErrorMsg('Miiska marketing_staff weli lama aburin Supabase-ka. Fadlan wax ka qabso SQL migration-ka hoosta.');
          setAgents([]);
          return;
        }
        throw error;
      }

      if (data) {
        const agentsWithCounts: MarketingAgent[] = await Promise.all(
          data.map(async (a: any) => {
            const { count: refCount } = await supabase
              .from('profiles')
              .select('*', { count: 'exact', head: true })
              .eq('referred_by', a.code);

            const { count: activeCount } = await supabase
              .from('profiles')
              .select('*', { count: 'exact', head: true })
              .eq('referred_by', a.code)
              .eq('subscription_status', 'active');

            return {
              id: a.id,
              user_id: a.user_id,
              username: a.username,
              full_name: a.full_name,
              phone: a.phone,
              code: a.code,
              status: a.status || 'active',
              created_at: a.created_at,
              referred_count: refCount || 0,
              active_referred_count: activeCount || 0,
            } as MarketingAgent;
          })
        );
        setAgents(agentsWithCounts);
      }
    } catch (err: any) {
      setErrorMsg(err?.message || 'Cilad ayaa dhacday marka la soo qaadayay shaqaalaha suuq-geynta.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAgents();
  }, [fetchAgents]);

  const handleCopyCode = (codeVal: string, id: string) => {
    navigator.clipboard.writeText(codeVal).catch(() => {});
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleCreateAgent = async () => {
    if (!fullName.trim() || !username.trim() || !password.trim() || !code.trim()) {
      setErrorMsg('Fadlan buuxi dhammaan goobaha loo baahdo (Magac, Username, Password, Code).');
      return;
    }
    setIsSaving(true);
    setErrorMsg(null);
    try {
      const { data: existing } = await supabase
        .from('marketing_staff')
        .select('id')
        .or(`username.eq.${username.trim()},code.eq.${code.trim().toUpperCase()}`);

      if (existing && existing.length > 0) {
        setErrorMsg('Username ama Code-kan horaan ayaa loo isticmaalay. Fadlan bedel mid cusub.');
        setIsSaving(false);
        return;
      }

      const { error } = await supabase.from('marketing_staff').insert([{
        username: username.trim().toLowerCase(),
        full_name: fullName.trim(),
        phone: phone.trim() || null,
        code: code.trim().toUpperCase(),
        password_hash: password,
        status: 'active',
      }]);

      if (error) throw error;

      await supabase.from('audit_logs').insert([{
        actor_name: actorName,
        action: 'CREATE_MARKETER',
        target_type: 'marketing_staff',
        metadata: { username: username.trim(), code: code.trim().toUpperCase(), full_name: fullName.trim() }
      }]).catch(() => {});

      setSuccessMsg(`Guul! Shaqaalaha "${fullName.trim()}" waa la abuuray!\nUsername: @${username.trim()}\nPassword: ${password}\nCode: ${code.trim().toUpperCase()}`);
      setFullName('');
      setUsername('');
      setPhone('');
      setCode(generateCode());
      setPassword(generatePassword());
      setShowCreateForm(false);
      await fetchAgents(true);
    } catch (err: any) {
      setErrorMsg(err?.message || 'Cilad ayaa dhacday marka la abuurayay shaqaalaha.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleConfirmAction = async () => {
    const { agentId, action } = confirmState;
    setConfirmState(s => ({ ...s, isOpen: false }));
    setErrorMsg(null);
    try {
      if (action === 'delete') {
        const { error } = await supabase.from('marketing_staff').delete().eq('id', agentId);
        if (error) throw error;
        setSuccessMsg('Shaqaalaha waa la tirtiray.');
      } else {
        const newStatus = action === 'suspend' ? 'suspended' : 'active';
        const { error } = await supabase.from('marketing_staff').update({ status: newStatus }).eq('id', agentId);
        if (error) throw error;
        setSuccessMsg(`Shaqaalaha waa la ${action === 'suspend' ? 'hakiyay' : 'furay'}.`);
      }
      await fetchAgents(true);
    } catch (err: any) {
      setErrorMsg(err?.message || 'Ficilka kuma guulaysanin.');
    }
  };

  const handleResetPassword = async () => {
    if (!resetModal.agent || !resetModal.newPassword.trim()) return;
    setErrorMsg(null);
    try {
      const { error } = await supabase
        .from('marketing_staff')
        .update({ password_hash: resetModal.newPassword.trim() })
        .eq('id', resetModal.agent.id);
      if (error) throw error;
      setSuccessMsg(`Password-ka ${resetModal.agent.full_name} waa la beddelay.\nPassword Cusub: ${resetModal.newPassword.trim()}`);
      setResetModal({ isOpen: false, agent: null, newPassword: '', showPw: false });
    } catch (err: any) {
      setErrorMsg(err?.message || 'Password beddelka kuma guulaysanin.');
    }
  };

  return (
    <div className="p-4 sm:p-6 space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center space-x-3">
          <div className="p-2.5 bg-teal-100 rounded-xl">
            <Megaphone className="w-6 h-6 text-teal-700" />
          </div>
          <div>
            <h2 className="text-lg font-black text-black">Shaqaalaha Suuq-geynta</h2>
            <p className="text-xs text-neutral-500">Marketing Staff · Referral Codes · Diiwaangelinta Darawallada</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => fetchAgents()}
            className="p-2 bg-white border border-neutral-200 hover:bg-neutral-50 rounded-xl transition cursor-pointer"
            title="Cusboona"
          >
            <RefreshCw className={`w-4 h-4 text-neutral-600 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          {canManage && (
            <button
              onClick={() => { setShowCreateForm(true); setErrorMsg(null); setSuccessMsg(null); }}
              className="flex items-center space-x-2 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-black shadow transition cursor-pointer"
            >
              <UserPlus className="w-4 h-4" />
              <span>Abuur Shaqaale Cusub</span>
            </button>
          )}
        </div>
      </div>

      {successMsg && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-xs font-mono text-emerald-900 whitespace-pre-line">
          {successMsg}
          <button onClick={() => setSuccessMsg(null)} className="ml-2 text-emerald-600 hover:underline font-bold">Xir</button>
        </div>
      )}
      {errorMsg && (
        <div className="flex items-start space-x-2 p-4 bg-red-50 border border-red-200 rounded-2xl">
          <AlertCircle className="w-4 h-4 text-red-500 shrink-0 mt-0.5" />
          <p className="text-xs text-red-800">{errorMsg}</p>
        </div>
      )}

      {/* SQL Notice */}
      <div className="p-4 bg-amber-50 border border-amber-200 rounded-2xl text-xs text-amber-900 leading-relaxed">
        <p className="font-bold mb-2">SQL Migration (Supabase SQL Editor):</p>
        <pre className="text-[10px] font-mono bg-amber-100 p-3 rounded-lg overflow-x-auto whitespace-pre">{`CREATE TABLE IF NOT EXISTS marketing_staff (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
  username TEXT UNIQUE NOT NULL,
  full_name TEXT NOT NULL,
  phone TEXT,
  code TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  status TEXT DEFAULT 'active',
  created_at TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS referred_by TEXT;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS subscription_status TEXT DEFAULT 'inactive';
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS subscription_expiry TIMESTAMPTZ;
ALTER TABLE marketing_staff ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Admin full access" ON marketing_staff FOR ALL USING (true);`}</pre>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[10px] font-bold uppercase tracking-wider text-neutral-500 mb-1">Shaqaalaha</p>
          <p className="text-2xl font-black text-black">{agents.length}</p>
        </div>
        <div className="bg-teal-50 p-4 rounded-2xl border border-teal-200 shadow-xs">
          <p className="text-[10px] font-bold uppercase tracking-wider text-teal-700 mb-1">Active</p>
          <p className="text-2xl font-black text-teal-700">{agents.filter(a => a.status === 'active').length}</p>
        </div>
        <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs">
          <p className="text-[10px] font-bold uppercase tracking-wider text-neutral-500 mb-1">Wadarta Diiwaangeliyay</p>
          <p className="text-2xl font-black text-black">{agents.reduce((a, b) => a + (b.referred_count || 0), 0)}</p>
        </div>
        <div className="bg-emerald-50 p-4 rounded-2xl border border-emerald-200 shadow-xs">
          <p className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 mb-1">Active Pass</p>
          <p className="text-2xl font-black text-emerald-700">{agents.reduce((a, b) => a + (b.active_referred_count || 0), 0)}</p>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-teal-500" />
            <p className="text-sm font-semibold">Soo qaadaya shaqaalaha...</p>
          </div>
        ) : agents.length === 0 ? (
          <div className="p-12 text-center">
            <Megaphone className="w-12 h-12 text-neutral-200 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Weli shaqaale lama aburin</h3>
            <p className="text-xs text-neutral-500 mt-1">Riix "Abuur Shaqaale Cusub" si aad u bilowdo.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[800px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Shaqaalaha</th>
                  <th className="p-4 font-bold">Username</th>
                  <th className="p-4 font-bold">Koodhka (Referral)</th>
                  <th className="p-4 font-bold">Taleefanka</th>
                  <th className="p-4 font-bold text-center">Diiwaangeliyay</th>
                  <th className="p-4 font-bold text-center">Active Pass</th>
                  <th className="p-4 font-bold">Xaaladda</th>
                  <th className="p-4 font-bold text-right">Ficilka</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {agents.map(agent => (
                  <tr key={agent.id} className="hover:bg-neutral-50 transition">
                    <td className="p-4">
                      <p className="font-bold text-black">{agent.full_name}</p>
                      <p className="text-[10px] text-neutral-400 font-mono">{new Date(agent.created_at).toLocaleDateString('so-SO')}</p>
                    </td>
                    <td className="p-4">
                      <span className="font-mono text-xs bg-neutral-100 px-2 py-1 rounded-lg text-neutral-800">@{agent.username}</span>
                    </td>
                    <td className="p-4">
                      <div className="flex items-center space-x-2">
                        <span className="font-mono text-sm font-black text-teal-700 bg-teal-50 px-3 py-1 rounded-lg border border-teal-200">{agent.code}</span>
                        <button
                          onClick={() => handleCopyCode(agent.code, agent.id)}
                          className="p-1.5 hover:bg-teal-100 rounded-lg transition cursor-pointer"
                          title="Copy"
                        >
                          {copiedId === agent.id ? <Check className="w-3.5 h-3.5 text-teal-600" /> : <Copy className="w-3.5 h-3.5 text-teal-500" />}
                        </button>
                      </div>
                    </td>
                    <td className="p-4 text-xs font-mono text-neutral-700">{agent.phone || '—'}</td>
                    <td className="p-4 text-center">
                      <span className="text-xl font-black text-black block">{agent.referred_count || 0}</span>
                      <span className="text-[9px] text-neutral-400 uppercase">Daraawal</span>
                    </td>
                    <td className="p-4 text-center">
                      <span className="text-xl font-black text-emerald-600 block">{agent.active_referred_count || 0}</span>
                      <span className="text-[9px] text-neutral-400 uppercase">Active</span>
                    </td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-lg text-xs font-bold ${agent.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'}`}>
                        {agent.status === 'active' ? 'Furan' : 'Hakad'}
                      </span>
                    </td>
                    <td className="p-4">
                      {canManage && (
                        <div className="flex items-center justify-end space-x-1.5">
                          <button onClick={() => setResetModal({ isOpen: true, agent, newPassword: generatePassword(), showPw: true })} className="p-1.5 hover:bg-blue-50 text-blue-500 hover:text-blue-700 rounded-lg transition cursor-pointer" title="Password Cusbooni">
                            <KeyRound className="w-4 h-4" />
                          </button>
                          {agent.status === 'active' ? (
                            <button onClick={() => setConfirmState({ isOpen: true, agentId: agent.id, agentName: agent.full_name, action: 'suspend' })} className="p-1.5 hover:bg-amber-50 text-amber-500 hover:text-amber-700 rounded-lg transition cursor-pointer" title="Hakin">
                              <ShieldOff className="w-4 h-4" />
                            </button>
                          ) : (
                            <button onClick={() => setConfirmState({ isOpen: true, agentId: agent.id, agentName: agent.full_name, action: 'activate' })} className="p-1.5 hover:bg-emerald-50 text-emerald-500 hover:text-emerald-700 rounded-lg transition cursor-pointer" title="Fur">
                              <ShieldCheck className="w-4 h-4" />
                            </button>
                          )}
                          <button onClick={() => setConfirmState({ isOpen: true, agentId: agent.id, agentName: agent.full_name, action: 'delete' })} className="p-1.5 hover:bg-red-50 text-red-400 hover:text-red-600 rounded-lg transition cursor-pointer" title="Tirtir">
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create Form Modal */}
      {showCreateForm && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg border border-neutral-200 overflow-y-auto max-h-[90vh]">
            <div className="p-6 border-b border-neutral-100 flex items-center justify-between sticky top-0 bg-white rounded-t-3xl">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-teal-100 rounded-xl"><UserPlus className="w-5 h-5 text-teal-700" /></div>
                <h3 className="text-base font-black text-black">Abuur Shaqaale Suuq-geyn Cusub</h3>
              </div>
              <button onClick={() => setShowCreateForm(false)} className="p-1.5 hover:bg-neutral-100 rounded-lg cursor-pointer">
                <X className="w-5 h-5 text-neutral-500" />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Magaca Buuxa *</label>
                <input type="text" value={fullName} onChange={e => setFullName(e.target.value)} placeholder="Tusaale: Faarax Cali" className="w-full px-4 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm text-black focus:outline-none focus:border-teal-400 focus:bg-white" />
              </div>
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Username (Login) *</label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 text-sm font-mono">@</span>
                  <input type="text" value={username} onChange={e => setUsername(sanitizeUsername(e.target.value))} placeholder="faarax.cali" className="w-full pl-7 pr-4 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm font-mono text-black focus:outline-none focus:border-teal-400 focus:bg-white" />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Taleefanka (Ikhtiyaari)</label>
                <input type="tel" value={phone} onChange={e => setPhone(e.target.value)} placeholder="0615xxxxxx" className="w-full px-4 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm text-black focus:outline-none focus:border-teal-400 focus:bg-white" />
              </div>
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Koodhka Referral *</label>
                <div className="flex space-x-2">
                  <input type="text" value={code} onChange={e => setCode(e.target.value.toUpperCase())} className="flex-1 px-4 py-2.5 bg-teal-50 border-2 border-teal-300 rounded-xl text-sm font-black font-mono text-teal-800 focus:outline-none focus:border-teal-500" />
                  <button onClick={() => setCode(generateCode())} className="px-3 py-2.5 bg-teal-100 hover:bg-teal-200 text-teal-700 rounded-xl transition cursor-pointer" title="Cusbooni">
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
                <p className="text-[10px] text-neutral-500 mt-1">Koodhkan daraawalku isticmaalay marka uu diiwaangelanayo.</p>
              </div>
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Password *</label>
                <div className="flex space-x-2">
                  <div className="relative flex-1">
                    <input type={showPassword ? 'text' : 'password'} value={password} onChange={e => setPassword(e.target.value)} className="w-full px-4 pr-10 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm font-mono text-black focus:outline-none focus:border-teal-400 focus:bg-white" />
                    <button type="button" onClick={() => setShowPassword(v => !v)} className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 cursor-pointer">
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  <button onClick={() => setPassword(generatePassword())} className="px-3 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-neutral-600 rounded-xl transition cursor-pointer">
                    <RefreshCw className="w-4 h-4" />
                  </button>
                </div>
                <p className="text-[10px] text-neutral-500 mt-1">Ka nuqul qaad oo u dir shaqaalaha.</p>
              </div>
            </div>
            <div className="p-6 border-t border-neutral-100 flex space-x-3 sticky bottom-0 bg-white rounded-b-3xl">
              <button onClick={() => setShowCreateForm(false)} className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-black text-sm font-bold rounded-xl transition cursor-pointer">Ka noqo</button>
              <button onClick={handleCreateAgent} disabled={isSaving} className="flex-1 py-2.5 bg-teal-600 hover:bg-teal-700 disabled:opacity-60 text-white text-sm font-black rounded-xl transition cursor-pointer flex items-center justify-center space-x-2">
                {isSaving ? <RefreshCw className="w-4 h-4 animate-spin" /> : <UserPlus className="w-4 h-4" />}
                <span>{isSaving ? 'Abuuraya...' : 'Abuur Shaqaale'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {resetModal.isOpen && resetModal.agent && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md border border-neutral-200">
            <div className="p-6 border-b border-neutral-100 flex items-center justify-between">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-blue-100 rounded-xl"><KeyRound className="w-5 h-5 text-blue-700" /></div>
                <h3 className="text-base font-black text-black">Cusbooni Password</h3>
              </div>
              <button onClick={() => setResetModal({ isOpen: false, agent: null, newPassword: '', showPw: false })} className="p-1.5 hover:bg-neutral-100 rounded-lg cursor-pointer">
                <X className="w-5 h-5 text-neutral-500" />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <p className="text-xs text-neutral-600">Shaqaalaha: <strong>{resetModal.agent.full_name}</strong> (@{resetModal.agent.username})</p>
              <div>
                <label className="block text-xs font-bold text-neutral-700 mb-1.5">Password Cusub *</label>
                <div className="flex space-x-2">
                  <div className="relative flex-1">
                    <input type={resetModal.showPw ? 'text' : 'password'} value={resetModal.newPassword} onChange={e => setResetModal(s => ({ ...s, newPassword: e.target.value }))} className="w-full px-4 pr-10 py-2.5 bg-neutral-50 border border-neutral-200 rounded-xl text-sm font-mono text-black focus:outline-none focus:border-blue-400" />
                    <button type="button" onClick={() => setResetModal(s => ({ ...s, showPw: !s.showPw }))} className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 cursor-pointer">
                      {resetModal.showPw ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  <button onClick={() => setResetModal(s => ({ ...s, newPassword: generatePassword() }))} className="px-3 py-2.5 bg-neutral-100 hover:bg-neutral-200 rounded-xl cursor-pointer">
                    <RefreshCw className="w-4 h-4 text-neutral-600" />
                  </button>
                </div>
              </div>
            </div>
            <div className="p-6 border-t border-neutral-100 flex space-x-3">
              <button onClick={() => setResetModal({ isOpen: false, agent: null, newPassword: '', showPw: false })} className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-black text-sm font-bold rounded-xl transition cursor-pointer">Ka noqo</button>
              <button onClick={handleResetPassword} className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-black rounded-xl transition cursor-pointer">Bedel Password</button>
            </div>
          </div>
        </div>
      )}

      {/* Confirm Action Modal */}
      {confirmState.isOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-sm border border-neutral-200 p-6 text-center">
            <div className={`w-14 h-14 rounded-full flex items-center justify-center mx-auto mb-4 ${confirmState.action === 'delete' ? 'bg-red-100' : 'bg-amber-100'}`}>
              {confirmState.action === 'delete' ? <Trash2 className="w-7 h-7 text-red-500" /> : <ShieldOff className="w-7 h-7 text-amber-500" />}
            </div>
            <h3 className="text-base font-black text-black mb-2">
              {confirmState.action === 'delete' ? 'Tirtirista Xaqiiji' : confirmState.action === 'suspend' ? 'Hakinta Xaqiiji' : 'Furista Xaqiiji'}
            </h3>
            <p className="text-xs text-neutral-600 mb-6">
              Ma hubtaa inaad rabto inaad <strong>{confirmState.action === 'delete' ? 'tirtirto' : confirmState.action === 'suspend' ? 'hakiso' : 'furto'}</strong> shaqaalaha <strong>{confirmState.agentName}</strong>?
              {confirmState.action === 'delete' && <span className="text-red-600 block mt-1">Ficilkan looma soo celin karo.</span>}
            </p>
            <div className="flex space-x-3">
              <button onClick={() => setConfirmState(s => ({ ...s, isOpen: false }))} className="flex-1 py-2.5 bg-neutral-100 hover:bg-neutral-200 text-black text-sm font-bold rounded-xl transition cursor-pointer">Ka noqo</button>
              <button onClick={handleConfirmAction} className={`flex-1 py-2.5 text-white text-sm font-black rounded-xl transition cursor-pointer ${confirmState.action === 'delete' ? 'bg-red-600 hover:bg-red-700' : 'bg-amber-500 hover:bg-amber-600'}`}>
                Haa, Xaqiiji
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};