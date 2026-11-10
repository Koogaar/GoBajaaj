import React, { useState, useEffect } from 'react';
import { AppRole, Language } from './types';
import { platformStore } from './services/store';
import { AppSwitcher } from './components/AppSwitcher';
import { DeviceFrame } from './components/DeviceFrame';
import { CustomerApp } from './components/customer/CustomerApp';
import { MerchantApp } from './components/merchant/MerchantApp';
import { CourierApp } from './components/courier/CourierApp';
import { AdminDashboard } from './components/admin/AdminDashboard';
import { RoleLoginView } from './components/auth/RoleLoginView';

export default function App() {
  const [currentRole, setCurrentRole] = useState<AppRole>(platformStore.getCurrentRole());
  const [lang, setLang] = useState<Language>(platformStore.getLanguage());
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(platformStore.isRoleLoggedIn(currentRole));

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      const role = platformStore.getCurrentRole();
      setCurrentRole(role);
      setLang(platformStore.getLanguage());
      setIsLoggedIn(platformStore.isRoleLoggedIn(role));
    });
    return unsub;
  }, []);

  const handleSelectRole = (role: AppRole) => {
    platformStore.setRole(role);
    setCurrentRole(role);
    setIsLoggedIn(platformStore.isRoleLoggedIn(role));
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col font-sans selection:bg-red-600 selection:text-white relative">
      {/* Universal Top Switcher */}
      <AppSwitcher
        currentRole={currentRole}
        onSelectRole={handleSelectRole}
        isMultiView={false}
        onToggleMultiView={() => {}}
        lang={lang}
      />

      {/* Main View Area */}
      <div className="flex-1 flex flex-col justify-start">
        {!isLoggedIn ? (
          <RoleLoginView
            role={currentRole}
            onLoginSuccess={() => {
              setIsLoggedIn(true);
            }}
          />
        ) : (
          <>
            {currentRole === 'customer' && (
              <DeviceFrame appName="Shiri Delivery" roleBadge="Customer App">
                <CustomerApp />
              </DeviceFrame>
            )}

            {currentRole === 'merchant' && (
              <DeviceFrame appName="Shiri Merchant" roleBadge="Business App">
                <MerchantApp />
              </DeviceFrame>
            )}

            {currentRole === 'courier' && (
              <DeviceFrame appName="Shiri Driver" roleBadge="Courier App">
                <CourierApp />
              </DeviceFrame>
            )}

            {currentRole === 'admin' && (
              <div className="flex-1 w-full bg-slate-100 min-h-screen">
                <AdminDashboard />
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
