import React, { useState } from 'react';
import { Shield, Lock, User, AlertCircle, CheckCircle2, RefreshCw } from 'lucide-react';
import { supabase } from '../supabase';
import logoImg from '../assets/logo.png';

interface LoginScreenProps {
  onLoginSuccess: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess }) => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [resetSuccess, setResetSuccess] = useState<string | null>(null);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanUsername = username.trim();
    const cleanPassword = password.trim();

    if (!cleanUsername || !cleanPassword) {
      setErrorMsg('Fadlan geli Username-ka iyo Furaha sirta ah.');
      return;
    }

    setIsLoading(true);
    setErrorMsg(null);
    setResetSuccess(null);

    try {
      // 1. Check if user is a Marketing Staff member logging into the web app (Chrome)
      try {
        const { data: marketerRow } = await supabase
          .from('marketing_staff')
          .select('*')
          .ilike('username', cleanUsername)
          .maybeSingle();

        if (marketerRow) {
          if (marketerRow.password_hash !== cleanPassword) {
            setErrorMsg('Username ama Furaha sirta ah ma saxna.');
            setIsLoading(false);
            return;
          }

          if (marketerRow.status === 'suspended' || marketerRow.status === 'disabled') {
            setErrorMsg('Akoonkaaga suuq-geynta waa la hakiyay. Fadlan la xiriir Maamulka GoBajaaj.');
            setIsLoading(false);
            return;
          }

          // Save marketer session to localStorage for web app Chrome session
          localStorage.setItem('gobajaaj_marketing_agent', JSON.stringify(marketerRow));
          onLoginSuccess();
          return;
        }
      } catch (marketerCheckErr) {
        // If marketing_staff table does not exist yet or fails, fallback cleanly to admin auth
        console.warn('Marketing staff check fallback:', marketerCheckErr);
      }

      // Identity Mapping: Determine target email for Supabase Auth
      let targetEmail = cleanUsername;

      if (!cleanUsername.includes('@')) {
        if (cleanUsername.toLowerCase() === 'owner' || cleanUsername.toLowerCase() === 'koogaaryare' || cleanUsername.toLowerCase() === 'kuluntamedia' || cleanUsername.toLowerCase() === 'admin') {
          targetEmail = 'kuluntamedia@gmail.com';
        } else {
          // Look up email from admin_users table where username = cleanUsername
          const { data: adminRow } = await supabase
            .from('admin_users')
            .select('email, username, status')
            .ilike('username', cleanUsername)
            .maybeSingle();

          if (adminRow && adminRow.email) {
            targetEmail = adminRow.email;
          } else {
            // Standard mapping convention for usernames without domain
            targetEmail = `${cleanUsername.toLowerCase()}@gobajaaj.so`;
          }
        }
      }

      // Real Supabase Auth Authentication
      let { data, error } = await supabase.auth.signInWithPassword({
        email: targetEmail,
        password: cleanPassword,
      });

      // Owner Account First-Time Bootstrap
      const isOwnerAccount = targetEmail === 'kuluntamedia@gmail.com' || 
                             targetEmail === 'koogaaryare@gmail.com' || 
                             cleanUsername.toLowerCase() === 'owner' || 
                             cleanUsername.toLowerCase() === 'admin';

      if (error && isOwnerAccount) {
        const signUpRes = await supabase.auth.signUp({
          email: targetEmail,
          password: cleanPassword,
          options: {
            data: {
              full_name: 'GoBajaaj Super Admin',
              username: cleanUsername.toLowerCase(),
              role: 'super_admin'
            }
          }
        });

        if (signUpRes.data?.session?.user || signUpRes.data?.user) {
          const retryAuth = await supabase.auth.signInWithPassword({
            email: targetEmail,
            password: cleanPassword,
          });
          data = retryAuth.data;
          error = retryAuth.error;
        }
      }

      if (error) {
        console.error('Login Auth Error:', error.message);
        setErrorMsg('Email/Username ama Furaha sirta ah waa meel aan sax ahayn.');
        setIsLoading(false);
        return;
      }

      if (data.session?.user) {
        // Verify admin_users role and status by user_id, email, or username
        let { data: adminData } = await supabase
          .from('admin_users')
          .select('*')
          .or(`user_id.eq.${data.session.user.id},email.eq.${targetEmail},email.eq.${data.session.user.email},username.ilike.${cleanUsername}`)
          .maybeSingle();

        // Link user_id on admin_users table if not set yet
        if (adminData && !adminData.user_id) {
          await supabase
            .from('admin_users')
            .update({ user_id: data.session.user.id })
            .eq('id', adminData.id);
        }

        // Ensure owner / super admin entry exists in admin_users if first time
        if (!adminData && (isOwnerAccount || data.session.user.email?.includes('gmail.com'))) {
          const { data: insertedAdmin } = await supabase.from('admin_users').insert([{
            user_id: data.session.user.id,
            username: cleanUsername.toLowerCase() || 'owner',
            email: data.session.user.email || targetEmail,
            full_name: 'GoBajaaj Super Admin',
            role_key: 'super_admin',
            status: 'active'
          }]).select().maybeSingle();

          if (insertedAdmin) {
            adminData = insertedAdmin;
          }
        }

        // If status is suspended or disabled, immediately block access and sign out
        if (adminData && (adminData.status === 'suspended' || adminData.status === 'disabled')) {
          await supabase.auth.signOut();
          setErrorMsg('Akkawnkaaga maamul waa la hakanay/laga saaray nidaamka. La xiriir Owner/Super Admin.');
          setIsLoading(false);
          return;
        }

        // Update last_login timestamp and log audit event
        try {
          if (adminData) {
            await supabase
              .from('admin_users')
              .update({ last_login: new Date().toISOString() })
              .eq('id', adminData.id);
          }

          await supabase.from('audit_logs').insert([{
            actor_name: adminData?.full_name || data.session.user.email || cleanUsername,
            action: 'ADMIN_LOGIN',
            target_type: 'admin_session',
            target_id: data.session.user.id,
            metadata: { username: cleanUsername, email: targetEmail }
          }]);
        } catch (auditErr) {
          console.warn('Audit log write notice:', auditErr);
        }

        onLoginSuccess();
      }
    } catch (err: any) {
      console.error('Unexpected Auth Error:', err);
      setErrorMsg('Cilad ayaa dhacday marka la galayay habka. Dib u tijaabi.');
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordReset = async () => {
    const cleanUsername = username.trim();
    if (!cleanUsername) {
      setErrorMsg('Fadlan geli Username-kaaga ama email-kaaga si loogu soo diro xiriirka furaha sirta ah.');
      return;
    }
    setIsLoading(true);
    setErrorMsg(null);
    try {
      let targetEmail = cleanUsername;
      if (!cleanUsername.includes('@')) {
        const { data: adminRow } = await supabase
          .from('admin_users')
          .select('email')
          .ilike('username', cleanUsername)
          .maybeSingle();
        
        if (adminRow?.email) {
          targetEmail = adminRow.email;
        } else {
          targetEmail = `${cleanUsername.toLowerCase()}@somalitraffic.so`;
        }
      }

      const { error } = await supabase.auth.resetPasswordForEmail(targetEmail);
      if (error) {
        setErrorMsg(error.message);
      } else {
        setResetSuccess('Xiriirka furaha sirta ah waxaa loo soo diray email-kaaga.');
      }
    } catch (err: any) {
      setErrorMsg('Cilad ayaa dhacday.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#111] flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-3xl shadow-2xl overflow-hidden border border-neutral-800">
        {/* Header */}
        <div className="bg-black p-8 text-center text-white border-b border-neutral-800">
          <img 
            src={logoImg} 
            alt="GoBajaaj Logo" 
            className="w-16 h-16 rounded-2xl mx-auto mb-4 object-cover shadow-lg border border-neutral-700" 
          />
          <h1 className="text-2xl font-black tracking-wider text-white uppercase">Maamulka GoBajaaj</h1>
          <p className="text-xs text-[#E50914] font-bold mt-1 tracking-wider">
            Gal Dashboard-ka Maamulka
          </p>
        </div>

        {/* Form Body */}
        <form onSubmit={handleLogin} className="p-8 space-y-5">
          {errorMsg && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}

          {resetSuccess && (
            <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-xl text-emerald-800 text-xs flex items-center space-x-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{resetSuccess}</span>
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-black uppercase tracking-wider mb-2">
              Admin Email ama Username
            </label>
            <div className="relative">
              <User className="w-5 h-5 absolute left-3.5 top-3 text-neutral-400" />
              <input
                type="text"
                required
                placeholder="Geli Email ama Username (tusaale: admin@gobajaaj.so)"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full pl-11 pr-4 py-3 bg-neutral-100 border border-neutral-200 rounded-xl text-sm font-medium focus:bg-white focus:border-[#E50914] focus:outline-none transition text-black"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-black uppercase tracking-wider mb-2">
              Furaha sirta
            </label>
            <div className="relative">
              <Lock className="w-5 h-5 absolute left-3.5 top-3 text-neutral-400" />
              <input
                type="password"
                required
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-11 pr-4 py-3 bg-neutral-100 border border-neutral-200 rounded-xl text-sm font-medium focus:bg-white focus:border-[#E50914] focus:outline-none transition text-black"
              />
            </div>
          </div>

          <div className="flex items-center justify-end text-xs pt-1">
            <button
              type="button"
              onClick={handlePasswordReset}
              className="text-[#E50914] font-bold hover:underline"
            >
              Furaha sirta ma illowday?
            </button>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-3.5 bg-[#E50914] hover:bg-[#B91C1C] text-white font-black text-sm rounded-xl shadow-lg hover:shadow-xl transition flex items-center justify-center space-x-2 cursor-pointer"
          >
            {isLoading ? (
              <RefreshCw className="w-5 h-5 animate-spin" />
            ) : (
              <span>Gal</span>
            )}
          </button>
        </form>

        <div className="p-4 bg-neutral-50 border-t border-neutral-200 text-center text-[11px] text-neutral-500 font-medium">
          Maamulka GoBajaaj Dashboard © 2026
        </div>
      </div>
    </div>
  );
};

