import React, { useState, useEffect } from 'react';
import { AppRole, Language } from './types';
import { platformStore } from './services/store';
import { AppSwitcher } from './components/AppSwitcher';
import { DeviceFrame } from './components/DeviceFrame';
import { CustomerApp } from './components/customer/CustomerApp';
import { MerchantApp } from './components/merchant/MerchantApp';
import { CourierApp } from './components/courier/CourierApp';
import { AdminDashboard } from './components/admin/AdminDashboard';
import { translations } from './i18n/translations';
import { CheckCircle2, FileCode, Server, ShieldCheck, ArrowRight, BookOpen, Layers } from 'lucide-react';

export default function App() {
  const [currentRole, setCurrentRole] = useState<AppRole>(platformStore.getCurrentRole());
  const [lang, setLang] = useState<Language>(platformStore.getLanguage());
  const [isMultiView, setIsMultiView] = useState<boolean>(false);
  const [isProductionMode, setIsProductionMode] = useState<boolean>(false);
  const [showDocsModal, setShowDocsModal] = useState<boolean>(false);

  useEffect(() => {
    const unsub = platformStore.subscribe(() => {
      setCurrentRole(platformStore.getCurrentRole());
      setLang(platformStore.getLanguage());
    });
    return unsub;
  }, []);

  const handleSelectRole = (role: AppRole) => {
    platformStore.setRole(role);
    setCurrentRole(role);
  };

  return (
    <div className="min-h-screen bg-slate-900 flex flex-col font-sans selection:bg-red-600 selection:text-white relative">
      {/* Universal Top Switcher (Hidden in Pure Mobile View) */}
      {!isProductionMode && (
        <AppSwitcher
          currentRole={currentRole}
          onSelectRole={handleSelectRole}
          isMultiView={isMultiView}
          onToggleMultiView={() => setIsMultiView(!isMultiView)}
          lang={lang}
          isProductionMode={isProductionMode}
          onToggleProductionMode={() => setIsProductionMode(true)}
        />
      )}

      {/* Floating Toggle to return to Simulator if in Production Mode */}
      {isProductionMode && (
        <div className="fixed bottom-4 right-4 z-50">
          <button
            onClick={() => setIsProductionMode(false)}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-950/90 hover:bg-slate-900 text-white text-xs font-bold rounded-2xl shadow-2xl border border-slate-700 backdrop-blur-md transition-all active:scale-95"
            title="Dib ugu noqo Nidaamka Simulator-ka"
          >
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Habka Simulator-ka (4 Apps)</span>
          </button>
        </div>
      )}

      {/* Main View Area */}
      <div className="flex-1 flex flex-col justify-start">
        {/* MULTI-VIEW MODE: Shows Customer, Merchant & Courier Side-by-Side */}
        {isMultiView ? (
          <div className="p-4 sm:p-6 space-y-4">
            <div className="max-w-7xl mx-auto bg-slate-800/80 border border-slate-700 p-4 rounded-3xl text-white flex flex-col sm:flex-row items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-bold flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-ping" />
                  <span>SHIRI DELIVERY — Live Multi-Party Sync Simulator</span>
                </h2>
                <p className="text-xs text-slate-300 mt-0.5">
                  Test the complete lifecycle: Customer orders → Merchant accepts & marks ready → Driver receives dispatch, accepts & enters handover PIN.
                </p>
              </div>

              <button
                onClick={() => setShowDocsModal(true)}
                className="px-3.5 py-1.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors shrink-0 flex items-center gap-1.5"
              >
                <BookOpen className="w-3.5 h-3.5" />
                <span>Architecture & Docs</span>
              </button>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 max-w-7xl mx-auto items-start">
              {/* Column 1: Customer Mobile App */}
              <div className="flex flex-col items-center">
                <DeviceFrame appName="Shiri Customer" roleBadge="Customer App">
                  <CustomerApp />
                </DeviceFrame>
              </div>

              {/* Column 2: Merchant Mobile App */}
              <div className="flex flex-col items-center">
                <DeviceFrame appName="Shiri Merchant" roleBadge="Business App">
                  <MerchantApp />
                </DeviceFrame>
              </div>

              {/* Column 3: Courier Mobile App */}
              <div className="flex flex-col items-center">
                <DeviceFrame appName="Shiri Driver" roleBadge="Courier App">
                  <CourierApp />
                </DeviceFrame>
              </div>
            </div>
          </div>
        ) : (
          /* SINGLE ROLE MODE */
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

      {/* Architecture & Documentation Modal */}
      {showDocsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-xs animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 shadow-2xl space-y-5 text-slate-800">
            <div className="flex items-center justify-between pb-3 border-b">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-red-600" />
                <h3 className="text-base font-bold text-slate-900">
                  SHIRI DELIVERY — Master Architecture & Deliverables
                </h3>
              </div>
              <button
                onClick={() => setShowDocsModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-bold"
              >
                ✕
              </button>
            </div>

            <div className="space-y-4 text-xs">
              <div>
                <h4 className="font-bold text-slate-900 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                  <Server className="w-4 h-4 text-red-600" />
                  <span>1. Four Dedicated Applications & Realtime Sync</span>
                </h4>
                <p className="text-slate-600 leading-relaxed">
                  All four apps (Customer, Merchant, Courier, Admin) run on the unified SHIRI DELIVERY reactive engine. When a customer places an order, the merchant receives an instant alert; marking it ready dispatches it to available online couriers; couriers verify package handover using the customer's 4-digit PIN.
                </p>
              </div>

              <div>
                <h4 className="font-bold text-slate-900 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                  <Layers className="w-4 h-4 text-red-600" />
                  <span>2. Shiri Address (Guri Number Digital Ah)</span>
                </h4>
                <p className="text-slate-600 leading-relaxed">
                  Generates deterministic building & entrance identifiers (e.g., <code className="bg-slate-100 px-1 py-0.5 rounded font-mono text-red-600">SHR-MG-48291</code>) with GPS latitude/longitude, accuracy radius (±8m), district, street, landmark, and QR code for sharing with drivers and family.
                </p>
              </div>

              <div>
                <h4 className="font-bold text-slate-900 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                  <FileCode className="w-4 h-4 text-red-600" />
                  <span>3. Database & Supabase Migrations</span>
                </h4>
                <p className="text-slate-600 leading-relaxed">
                  Full SQL schema available in <code className="bg-slate-100 px-1 py-0.5 rounded font-mono">src/supabase/schema.sql</code> including PostgreSQL tables (<code className="font-mono">profiles</code>, <code className="font-mono">shiri_addresses</code>, <code className="font-mono">merchants</code>, <code className="font-mono">products</code>, <code className="font-mono">orders</code>, <code className="font-mono">order_items</code>, <code className="font-mono">telecom_transactions</code>, <code className="font-mono">system_audit_logs</code>), PostGIS spatial geometry, and Row Level Security (RLS) policies.
                </p>
              </div>

              <div>
                <h4 className="font-bold text-slate-900 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>4. Somalia Telecom Payment Gateways</span>
                </h4>
                <p className="text-slate-600 leading-relaxed">
                  Adapters for Hormuud EVC Plus (<code className="font-mono">*712*...#</code>), Somtel eDahab (<code className="font-mono">*789*...#</code>), Telesom ZAAD, Golis Sahal, and Cash on Delivery with idempotency keys and server verification.
                </p>
              </div>
            </div>

            <div className="pt-3 border-t flex justify-end">
              <button
                onClick={() => setShowDocsModal(false)}
                className="px-5 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow-xs transition-colors"
              >
                Close & Continue
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
