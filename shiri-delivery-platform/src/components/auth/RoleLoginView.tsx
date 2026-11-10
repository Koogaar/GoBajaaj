import React, { useState } from 'react';
import { AppRole } from '../../types';
import { platformStore } from '../../services/store';
import { ShiriLogo } from '../common/ShiriLogo';
import {
  Smartphone,
  Store,
  Bike,
  Shield,
  ArrowRight,
  CheckCircle2,
  Lock,
  Phone,
  KeyRound,
  User,
  AlertCircle,
} from 'lucide-react';

interface RoleLoginViewProps {
  role: AppRole;
  onLoginSuccess: () => void;
}

export const RoleLoginView: React.FC<RoleLoginViewProps> = ({ role, onLoginSuccess }) => {
  const lang = platformStore.getLanguage();
  const merchants = platformStore.getMerchants();
  const couriers = platformStore.getCouriers();

  // Customer state
  const [phone, setPhone] = useState<string>('0615112233');
  const [customerName, setCustomerName] = useState<string>('Cabdifataax Cismaan');
  const [otpSent, setOtpSent] = useState<boolean>(false);
  const [otp, setOtp] = useState<string>('1234');

  // Merchant state
  const [selectedMerchantId, setSelectedMerchantId] = useState<string>(merchants[0]?.id || 'merch-1');
  const [merchantPin, setMerchantPin] = useState<string>('1234');

  // Courier state
  const [selectedCourierId, setSelectedCourierId] = useState<string>(couriers[0]?.id || 'cour-1');
  const [courierPin, setCourierPin] = useState<string>('1234');

  // Admin state
  const [adminUsername, setAdminUsername] = useState<string>('admin');
  const [adminPassword, setAdminPassword] = useState<string>('shiri2026');

  const [error, setError] = useState<string | null>(null);

  const handleCustomerLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpSent) {
      if (!phone.trim()) {
        setError('Fadlan geli lambarkaaga taleefanka');
        return;
      }
      setError(null);
      setOtpSent(true);
      return;
    }

    if (otp.length < 4) {
      setError('Koodhka OTP waa inuu ahaadaa 4 god');
      return;
    }

    platformStore.loginCustomer(phone.trim(), customerName.trim() || 'Macmiil');
    onLoginSuccess();
  };

  const handleMerchantLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!merchantPin.trim()) {
      setError('Fadlan geli PIN-ka meheradda');
      return;
    }
    platformStore.loginMerchant(selectedMerchantId);
    onLoginSuccess();
  };

  const handleCourierLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!courierPin.trim()) {
      setError('Fadlan geli PIN-ka darawalka');
      return;
    }
    platformStore.loginCourier(selectedCourierId);
    onLoginSuccess();
  };

  const handleAdminLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (!adminUsername.trim() || !adminPassword.trim()) {
      setError('Fadlan geli username iyo password');
      return;
    }
    platformStore.loginAdmin();
    onLoginSuccess();
  };

  return (
    <div className="w-full min-h-[85vh] flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden">
        {/* Header Header Banner */}
        <div className="bg-slate-950 text-white p-6 text-center space-y-2 border-b border-slate-800 relative">
          <div className="flex justify-center mb-1">
            <ShiriLogo variant="white" size="md" showSlogan={false} />
          </div>

          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-red-600/20 text-red-400 border border-red-500/30">
            {role === 'customer' && <Smartphone className="w-3.5 h-3.5" />}
            {role === 'merchant' && <Store className="w-3.5 h-3.5" />}
            {role === 'courier' && <Bike className="w-3.5 h-3.5" />}
            {role === 'admin' && <Shield className="w-3.5 h-3.5" />}
            <span className="capitalize">
              {role === 'customer'
                ? 'Akoonka Macmiilka (Customer)'
                : role === 'merchant'
                ? 'Albaabka Meheradda (Merchant)'
                : role === 'courier'
                ? 'Albaabka Darawalka (Courier)'
                : 'Maamulka Sare (Admin)'}
            </span>
          </div>

          <p className="text-xs text-slate-300">
            {role === 'customer'
              ? 'Ku soo dhawoow adeegga gaarsiinta ugu degdegga badan Muqdisho'
              : role === 'merchant'
              ? 'Maamul dukaankaaga, alaabtaada, iyo dalabyada macaamiisha'
              : role === 'courier'
              ? 'Gal nidaamka si aad u hesho safarro cusub oo aad dakhli u hesho'
              : 'Xarunta kormeerka guud iyo hawlgalka fleet-ka'}
          </p>
        </div>

        {/* Error notification */}
        {error && (
          <div className="m-4 mb-0 p-3 bg-red-50 border border-red-200 rounded-2xl flex items-center gap-2 text-xs text-red-700">
            <AlertCircle className="w-4 h-4 shrink-0 text-red-600" />
            <span>{error}</span>
          </div>
        )}

        <div className="p-6">
          {/* CUSTOMER LOGIN FORM */}
          {role === 'customer' && (
            <form onSubmit={handleCustomerLogin} className="space-y-4">
              {!otpSent ? (
                <>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Magacaaga (Full Name)
                    </label>
                    <div className="relative">
                      <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                      <input
                        type="text"
                        value={customerName}
                        onChange={(e) => setCustomerName(e.target.value)}
                        placeholder="Magacaaga oo buuxa"
                        className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-medium outline-none focus:border-red-600 focus:bg-white transition-all"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Lambarka Taleefanka (EVC Plus / Somtel) *
                    </label>
                    <div className="relative">
                      <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                      <input
                        type="tel"
                        required
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="061XXXXXXX ama 062XXXXXXX"
                        className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-mono font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white transition-all"
                      />
                    </div>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      Waxaan kuugu soo diri doonaa koodhka 4-ta god ah ee OTP
                    </span>
                  </div>

                  <button
                    type="submit"
                    className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
                  >
                    <span>Dir Koodhka Xaqiijinta (Send OTP)</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </>
              ) : (
                <>
                  <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-2xl text-xs text-emerald-900 flex items-center gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                    <span>Koodhka waxaa loo diray <b>{phone}</b></span>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1 text-center">
                      Geli Koodhka 4-ta God ah (Enter 4-Digit OTP)
                    </label>
                    <input
                      type="text"
                      maxLength={4}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                      placeholder="••••"
                      className="w-36 mx-auto block text-center text-2xl font-black font-mono tracking-widest py-2.5 bg-slate-50 border-2 border-red-500 rounded-2xl focus:bg-white outline-none"
                    />
                    <span className="text-[10px] text-slate-400 text-center block mt-1">
                      Koodhka xaqiijinta tooska ah: <b>1234</b>
                    </span>
                  </div>

                  <div className="flex gap-2 pt-2">
                    <button
                      type="button"
                      onClick={() => setOtpSent(false)}
                      className="px-4 py-3 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-2xl"
                    >
                      Dib u Noqo
                    </button>
                    <button
                      type="submit"
                      className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
                    >
                      <CheckCircle2 className="w-4 h-4" />
                      <span>Gal Akoonka Macmiilka</span>
                    </button>
                  </div>
                </>
              )}
            </form>
          )}

          {/* MERCHANT LOGIN FORM */}
          {role === 'merchant' && (
            <form onSubmit={handleMerchantLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Dooro Meheraddaada (Registered Business) *
                </label>
                <select
                  value={selectedMerchantId}
                  onChange={(e) => setSelectedMerchantId(e.target.value)}
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                >
                  {merchants.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} ({m.district})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  PIN-ka / Furaha Meheradda (Merchant PIN) *
                </label>
                <div className="relative">
                  <KeyRound className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                  <input
                    type="password"
                    required
                    value={merchantPin}
                    onChange={(e) => setMerchantPin(e.target.value)}
                    placeholder="PIN-kaaga geli (e.g. 1234)"
                    className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-mono font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                  />
                </div>
                <span className="text-[10px] text-slate-400 mt-1 block">
                  PIN-ka hore ee meheradda: <b>1234</b>
                </span>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
              >
                <Store className="w-4 h-4" />
                <span>Gal Nidaamka Meheradda (Merchant Portal)</span>
              </button>
            </form>
          )}

          {/* COURIER LOGIN FORM */}
          {role === 'courier' && (
            <form onSubmit={handleCourierLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Dooro Akoonka Darawalka (Driver Profile) *
                </label>
                <select
                  value={selectedCourierId}
                  onChange={(e) => setSelectedCourierId(e.target.value)}
                  className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                >
                  {couriers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} — {c.vehicleType} ({c.plateNumber})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  PIN-ka Darawalka (Driver PIN) *
                </label>
                <div className="relative">
                  <KeyRound className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                  <input
                    type="password"
                    required
                    value={courierPin}
                    onChange={(e) => setCourierPin(e.target.value)}
                    placeholder="PIN-kaaga geli (e.g. 1234)"
                    className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-mono font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                  />
                </div>
                <span className="text-[10px] text-slate-400 mt-1 block">
                  PIN-ka hore ee darawalka: <b>1234</b>
                </span>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
              >
                <Bike className="w-4 h-4" />
                <span>Gal Nidaamka Darawalka (Driver Portal)</span>
              </button>
            </form>
          )}

          {/* ADMIN LOGIN FORM */}
          {role === 'admin' && (
            <form onSubmit={handleAdminLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Username Maamulka *
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                  <input
                    type="text"
                    required
                    value={adminUsername}
                    onChange={(e) => setAdminUsername(e.target.value)}
                    placeholder="admin"
                    className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Furaha Sirta ah (Admin Password) *
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                  <input
                    type="password"
                    required
                    value={adminPassword}
                    onChange={(e) => setAdminPassword(e.target.value)}
                    placeholder="Password-ka maamulka"
                    className="w-full pl-10 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-2xl text-xs font-mono font-bold text-slate-900 outline-none focus:border-red-600 focus:bg-white"
                  />
                </div>
                <span className="text-[10px] text-slate-400 mt-1 block">
                  Furaha maamulka: <b>shiri2026</b>
                </span>
              </div>

              <button
                type="submit"
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-2xl shadow-lg shadow-red-600/30 transition-all flex items-center justify-center gap-2"
              >
                <Shield className="w-4 h-4" />
                <span>Gal Maamulka Sare (Admin Console)</span>
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
