import React, { useState } from 'react';
import { AppRole, Language } from '../types';
import { platformStore } from '../services/store';
import { translations } from '../i18n/translations';
import { ShiriLogo } from './common/ShiriLogo';
import {
  Smartphone,
  Store,
  Bike,
  Shield,
  Columns,
  Globe,
  RefreshCw,
  Bell,
  MapPin,
  Volume2,
  VolumeX,
  Play,
  Monitor,
  Database,
  Copy,
  Check,
} from 'lucide-react';
import { soundEffects } from '../utils/soundEffects';
import { isSupabaseConfigured, setSupabaseCredentials, supabaseUrl, supabaseAnonKey } from '../supabase/client';

interface AppSwitcherProps {
  currentRole: AppRole;
  onSelectRole: (role: AppRole) => void;
  isMultiView: boolean;
  onToggleMultiView: () => void;
  lang: Language;
  isProductionMode?: boolean;
  onToggleProductionMode?: () => void;
}

export const AppSwitcher: React.FC<AppSwitcherProps> = ({
  currentRole,
  onSelectRole,
  isMultiView,
  onToggleMultiView,
  lang,
  isProductionMode = false,
  onToggleProductionMode,
}) => {
  const t = translations[lang];
  const [isMuted, setIsMuted] = useState<boolean>(soundEffects.getIsMuted());
  const orders = platformStore.getOrders();
  const activeOrdersCount = orders.filter(
    (o) => !['delivered', 'cancelled'].includes(o.status)
  ).length;

  const currentApiKey = platformStore.getGoogleMapsApiKey();
  const hasKey = Boolean(currentApiKey && currentApiKey.trim());
  const [showMapsKeyModal, setShowMapsKeyModal] = useState<boolean>(false);
  const [keyInput, setKeyInput] = useState<string>(currentApiKey);

  const [showSupabaseModal, setShowSupabaseModal] = useState<boolean>(false);
  const [supaUrl, setSupaUrl] = useState<string>(supabaseUrl);
  const [supaKey, setSupaKey] = useState<string>(supabaseAnonKey);
  const [isSupabaseConnected, setIsSupabaseConnected] = useState<boolean>(isSupabaseConfigured());
  const [copiedSql, setCopiedSql] = useState<boolean>(false);

  const handleCopySqlSchema = () => {
    // Read or reference SQL Schema
    const sqlText = `-- SHIRI DELIVERY SUPABASE SCHEMA
-- Please run /src/supabase/schema.sql in your Supabase SQL Editor
-- Includes profiles, shiri_addresses, merchants, products, orders, couriers, telecom_transactions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
-- See /src/supabase/schema.sql for the full 220-line production schema!`;
    navigator.clipboard.writeText(sqlText);
    setCopiedSql(true);
    setTimeout(() => setCopiedSql(false), 2500);
  };

  const handleSaveSupabase = (e: React.FormEvent) => {
    e.preventDefault();
    setSupabaseCredentials(supaUrl, supaKey);
    setIsSupabaseConnected(isSupabaseConfigured());
    setShowSupabaseModal(false);
  };

  return (
    <header className="sticky top-0 z-50 bg-slate-950 text-white border-b border-slate-800 px-3 sm:px-6 py-2.5 shadow-md">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-3">
        {/* Brand & Market Identity */}
        <div className="flex items-center gap-3 shrink-0">
          <ShiriLogo variant="white" size="sm" showSlogan={true} sloganLanguage={lang} />
          <span className="hidden sm:inline-block text-[10px] bg-red-900/60 text-red-300 font-mono font-bold px-2 py-0.5 rounded-full border border-red-700/50">
            Mogadishu · Somalia
          </span>
        </div>

        {/* 4 Application Selectors + Multi-App Simulator Mode */}
        <div className="flex items-center gap-1.5 bg-slate-900/90 p-1 rounded-2xl border border-slate-800 overflow-x-auto max-w-full">
          <button
            onClick={() => {
              if (isMultiView) onToggleMultiView();
              onSelectRole('customer');
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
              !isMultiView && currentRole === 'customer'
                ? 'bg-red-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            <span>1. Customer App</span>
          </button>

          <button
            onClick={() => {
              if (isMultiView) onToggleMultiView();
              onSelectRole('merchant');
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
              !isMultiView && currentRole === 'merchant'
                ? 'bg-red-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Store className="w-3.5 h-3.5" />
            <span>2. Business App</span>
          </button>

          <button
            onClick={() => {
              if (isMultiView) onToggleMultiView();
              onSelectRole('courier');
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
              !isMultiView && currentRole === 'courier'
                ? 'bg-red-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Bike className="w-3.5 h-3.5" />
            <span>3. Driver App</span>
          </button>

          <button
            onClick={() => {
              if (isMultiView) onToggleMultiView();
              onSelectRole('admin');
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
              !isMultiView && currentRole === 'admin'
                ? 'bg-red-600 text-white shadow-xs'
                : 'text-slate-400 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Shield className="w-3.5 h-3.5" />
            <span>4. Admin Web</span>
          </button>

          <button
            onClick={onToggleMultiView}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap shrink-0 ${
              isMultiView
                ? 'bg-emerald-600 text-white shadow-xs'
                : 'text-emerald-400 hover:text-emerald-300 hover:bg-slate-800'
            }`}
            title="Watch Customer, Business, and Driver sync in real-time"
          >
            <Columns className="w-3.5 h-3.5" />
            <span>Multi-App Sync</span>
          </button>
        </div>

        {/* Global Language Toggle & Active Order Alert Badge */}
        <div className="flex items-center gap-2 shrink-0">
          {activeOrdersCount > 0 && (
            <div className="flex items-center gap-1 bg-red-950/80 text-red-300 text-xs font-mono font-bold px-2.5 py-1 rounded-xl border border-red-800/60">
              <span className="w-2 h-2 rounded-full bg-red-500 animate-ping" />
              <span>{activeOrdersCount} Live Order{activeOrdersCount > 1 ? 's' : ''}</span>
            </div>
          )}

          {/* Production Mode Toggle (Clean native app view) */}
          {onToggleProductionMode && (
            <button
              onClick={onToggleProductionMode}
              className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold transition-all border ${
                isProductionMode
                  ? 'bg-amber-600 text-white border-amber-500 shadow-xs'
                  : 'bg-slate-900 text-amber-300 border-amber-900/60 hover:bg-slate-800'
              }`}
              title="Keliya App-ka Macmiilka (Mobile App Only)"
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Pure App View</span>
            </button>
          )}

          {/* Sound Mute / Unmute Toggle */}
          <button
            onClick={() => {
              const muted = soundEffects.toggleMute();
              setIsMuted(muted);
              if (!muted) soundEffects.playSuccessChime();
            }}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold transition-colors border ${
              !isMuted
                ? 'bg-slate-900 text-emerald-400 border-emerald-900/60 hover:bg-slate-800'
                : 'bg-slate-900 text-slate-500 border-slate-800 hover:bg-slate-800'
            }`}
            title={isMuted ? 'Dhawaaqa waa dansan yahay (Click to Unmute)' : 'Dhawaaqa waa shidan yahay (Click to Mute)'}
          >
            {!isMuted ? <Volume2 className="w-3.5 h-3.5 text-emerald-400" /> : <VolumeX className="w-3.5 h-3.5 text-slate-500" />}
            <span className="hidden sm:inline">{!isMuted ? 'Sound: ON' : 'Muted'}</span>
          </button>

          {/* Supabase Database Status / Setup Button */}
          <button
            onClick={() => setShowSupabaseModal(true)}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold transition-colors border ${
              isSupabaseConnected
                ? 'bg-slate-900 text-emerald-400 border-emerald-900/60 hover:bg-slate-800'
                : 'bg-indigo-950/60 text-indigo-300 border-indigo-800/80 hover:bg-indigo-900/60'
            }`}
            title="Isku xirka Supabase Database & Auth"
          >
            <Database className={`w-3.5 h-3.5 ${isSupabaseConnected ? 'text-emerald-400' : 'text-indigo-400'}`} />
            <span className="hidden sm:inline">{isSupabaseConnected ? 'Supabase: Active' : 'Supabase: Setup'}</span>
          </button>

          {/* Google Maps API Key Status / Setup Button */}
          <button
            onClick={() => setShowMapsKeyModal(true)}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-bold transition-colors border ${
              hasKey
                ? 'bg-slate-900 text-emerald-400 border-emerald-900/60 hover:bg-slate-800'
                : 'bg-amber-950/60 text-amber-300 border-amber-800/80 hover:bg-amber-900/60'
            }`}
            title="Google Maps API Key Configuration"
          >
            <MapPin className={`w-3.5 h-3.5 ${hasKey ? 'text-emerald-400' : 'text-amber-400'}`} />
            <span className="hidden sm:inline">{hasKey ? 'Maps: Active' : 'Maps Key: Setup'}</span>
          </button>

          <button
            onClick={() => {
              const next = lang === 'so' ? 'en' : 'so';
              platformStore.setLanguage(next);
            }}
            className="flex items-center gap-1 bg-slate-900 hover:bg-slate-800 text-slate-300 px-2.5 py-1 rounded-xl text-xs font-bold transition-colors border border-slate-800"
          >
            <Globe className="w-3.5 h-3.5 text-red-500" />
            <span>{lang === 'so' ? 'Af-Soomaali' : 'English'}</span>
          </button>
        </div>
      </div>

      {/* Supabase Integration & Setup Modal */}
      {showSupabaseModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/80 backdrop-blur-xs text-slate-900 animate-in fade-in">
          <div className="bg-white rounded-3xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-5 sm:p-6 shadow-2xl space-y-4 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                  ⚡
                </div>
                <div>
                  <h3 className="font-bold text-sm sm:text-base text-slate-900">
                    Supabase PostgreSQL & Auth Setup
                  </h3>
                  <p className="text-[11px] text-slate-500">
                    Sida loo xiro database-ka dhabta ah ee Shiri Delivery
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowSupabaseModal(false)}
                className="w-8 h-8 rounded-full hover:bg-slate-100 text-slate-400 hover:text-slate-600 flex items-center justify-center font-bold"
              >
                ✕
              </button>
            </div>

            {/* Connection Status Banner */}
            <div className={`p-3.5 rounded-2xl border flex items-center justify-between ${
              isSupabaseConnected
                ? 'bg-emerald-50 border-emerald-200 text-emerald-950'
                : 'bg-amber-50 border-amber-200 text-amber-950'
            }`}>
              <div className="flex items-center gap-2 text-xs font-bold">
                <span className={`w-2.5 h-2.5 rounded-full ${isSupabaseConnected ? 'bg-emerald-500' : 'bg-amber-500 animate-ping'}`} />
                <span>Xaaladda: {isSupabaseConnected ? 'Waa Ku Xiran Yahay (Connected)' : 'Weli Lama Xirin (Not Configured)'}</span>
              </div>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-md bg-white border border-slate-200 font-bold">
                PostgreSQL + RLS
              </span>
            </div>

            {/* Step 1: Create Supabase Project */}
            <div className="space-y-1.5 text-xs text-slate-700">
              <h4 className="font-bold text-slate-900 flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-slate-900 text-white flex items-center justify-center text-[10px]">1</span>
                <span>Mashruuc Cusub ka Fur Supabase</span>
              </h4>
              <p className="text-[11px] text-slate-600 pl-6.5 leading-relaxed">
                Booqo <a href="https://supabase.com" target="_blank" rel="noopener noreferrer" className="text-emerald-600 font-bold underline">supabase.com</a>, sameyso mashruuc cusub (New Project), magac u bixi (tusaale: <code>shiri-delivery</code>), oo dooro gobol ku dhow.
              </p>
            </div>

            {/* Step 2: Run SQL Schema */}
            <div className="space-y-1.5 text-xs text-slate-700">
              <h4 className="font-bold text-slate-900 flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-slate-900 text-white flex items-center justify-center text-[10px]">2</span>
                <span>Ku Shubo Shiri Database Schema (SQL)</span>
              </h4>
              <p className="text-[11px] text-slate-600 pl-6.5 leading-relaxed">
                Gudaha Supabase Dashboard, tag <b>SQL Editor</b> → <b>New Query</b>. Riix badhanka hoose si aad u koobiyeysato dhammaan schema-da diyaarka ah ee <code>/src/supabase/schema.sql</code> (Profiles, Shiri Addresses, Orders, Merchants, Couriers, Triggers):
              </p>
              <div className="pl-6.5">
                <button
                  type="button"
                  onClick={handleCopySqlSchema}
                  className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 active:scale-95 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-xs transition-all"
                >
                  {copiedSql ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedSql ? 'SQL Schema Waa La Koobiyeeyay!' : 'Koobiye schema.sql (Copy SQL Script)'}</span>
                </button>
              </div>
            </div>

            {/* Step 3: API Credentials Input */}
            <div className="space-y-2 text-xs pt-2 border-t">
              <h4 className="font-bold text-slate-900 flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-slate-900 text-white flex items-center justify-center text-[10px]">3</span>
                <span>Geli Project URL & Anon Key</span>
              </h4>
              <p className="text-[11px] text-slate-500 pl-6.5">
                Ka soo qaado <b>Project Settings → API</b> ee Supabase Dashboard:
              </p>

              <form onSubmit={handleSaveSupabase} className="space-y-3 pl-6.5">
                <div>
                  <label className="block text-[11px] font-bold text-slate-700 mb-0.5">
                    Project URL:
                  </label>
                  <input
                    type="url"
                    value={supaUrl}
                    onChange={(e) => setSupaUrl(e.target.value)}
                    placeholder="https://xyzcompany.supabase.co"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 font-mono outline-none focus:border-emerald-600"
                  />
                </div>

                <div>
                  <label className="block text-[11px] font-bold text-slate-700 mb-0.5">
                    anon / public API Key:
                  </label>
                  <input
                    type="password"
                    value={supaKey}
                    onChange={(e) => setSupaKey(e.target.value)}
                    placeholder="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 font-mono outline-none focus:border-emerald-600"
                  />
                </div>

                <div className="flex items-center gap-2 pt-1">
                  <button
                    type="submit"
                    className="flex-1 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-colors shadow-xs"
                  >
                    Keydi & Xaqiiji Xiriirka
                  </button>
                  {isSupabaseConnected && (
                    <button
                      type="button"
                      onClick={() => {
                        setSupabaseCredentials('', '');
                        setSupaUrl('');
                        setSupaKey('');
                        setIsSupabaseConnected(false);
                      }}
                      className="px-3 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-colors"
                    >
                      Kala Bixi
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Google Maps API Key Setup Modal */}
      {showMapsKeyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/80 backdrop-blur-xs text-slate-900">
          <div className="bg-white rounded-3xl w-full max-w-md p-6 shadow-2xl space-y-4 border border-slate-100">
            <div className="flex items-center justify-between pb-2 border-b">
              <div className="flex items-center gap-2 font-bold text-sm text-slate-900">
                <MapPin className="w-5 h-5 text-red-600" />
                <span>Google Maps Platform Configuration</span>
              </div>
              <button
                type="button"
                onClick={() => setShowMapsKeyModal(false)}
                className="text-slate-400 hover:text-slate-600 font-bold"
              >
                ✕
              </button>
            </div>

            <div className="text-xs text-slate-600 space-y-2">
              <p>
                To display official Google Maps tiles, satellite imagery, roads, and places across Shiri Delivery:
              </p>
              <div className="bg-slate-50 p-3 rounded-2xl border border-slate-200 font-mono text-[11px] space-y-1">
                <div>Environment Variable: <span className="font-bold text-red-600">VITE_GOOGLE_MAPS_API_KEY</span></div>
                <div>Status: <span className={hasKey ? 'text-emerald-600 font-bold' : 'text-amber-600 font-bold'}>{hasKey ? 'Configured & Active' : 'Not Configured'}</span></div>
              </div>
            </div>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                platformStore.setGoogleMapsApiKey(keyInput);
                setShowMapsKeyModal(false);
              }}
              className="space-y-3 text-xs"
            >
              <div>
                <label className="block font-semibold text-slate-700 mb-1">
                  Enter / Update Google Maps API Key
                </label>
                <input
                  type="text"
                  value={keyInput}
                  onChange={(e) => setKeyInput(e.target.value)}
                  placeholder="AIzaSy..."
                  className="w-full px-3 py-2 text-xs font-mono rounded-xl border border-slate-300 outline-none focus:border-red-600"
                />
              </div>

              <div className="flex gap-2 pt-1">
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl shadow-xs transition-colors"
                >
                  Save API Key
                </button>
                {hasKey && (
                  <button
                    type="button"
                    onClick={() => {
                      platformStore.setGoogleMapsApiKey('');
                      setKeyInput('');
                    }}
                    className="px-3 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-colors"
                  >
                    Clear Key
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}
    </header>
  );
};
