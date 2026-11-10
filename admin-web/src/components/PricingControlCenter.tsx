import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { PlatformPricingSettings } from '../types';
import { DollarSign, Save, RefreshCw, CheckCircle, TrendingUp, Truck, Store, Bike, AlertCircle, Plus, Minus } from 'lucide-react';

interface PricingControlCenterProps {
  actorName: string;
  canManage: boolean;
}

const DEFAULT_PRICING: PlatformPricingSettings = {
  id: 'shiri_global_pricing',
  driver_daily_pass_usd: 0.25,
  driver_weekly_pass_usd: 1.50,
  driver_monthly_pass_usd: 5.00,
  driver_grace_period_hours: 2,
  courier_base_delivery_fee_usd: 2.00,
  courier_per_km_fee_usd: 0.30,
  courier_share_percentage: 80.0,
  courier_monthly_subscription_usd: 3.00,
  service_fee_usd: 0.50,
  night_surge_multiplier: 1.25,
  merchant_monthly_subscription_usd: 3.00,
  merchant_default_commission_rate: 15.0,
  merchant_min_order_usd: 3.00,
  updated_by: 'Super Admin'
};

// ---- Reusable +/- Stepper Component ----
interface StepperFieldProps {
  label: string;
  value: number;
  step: number;
  min: number;
  max?: number;
  prefix?: string;
  suffix?: string;
  hint?: string;
  onChange: (val: number) => void;
  disabled?: boolean;
  highlight?: 'red' | 'blue' | 'green' | 'orange';
}

const StepperField: React.FC<StepperFieldProps> = ({
  label, value, step, min, max, prefix, suffix, hint, onChange, disabled, highlight = 'red'
}) => {
  const colorMap: Record<string, string> = {
    red: 'bg-red-600 hover:bg-red-700',
    blue: 'bg-blue-600 hover:bg-blue-700',
    green: 'bg-emerald-600 hover:bg-emerald-700',
    orange: 'bg-orange-500 hover:bg-orange-600',
  };
  const btnClass = `${colorMap[highlight]} text-white w-9 h-9 rounded-xl flex items-center justify-center font-black transition disabled:opacity-40 cursor-pointer`;

  const inc = () => {
    const next = parseFloat((value + step).toFixed(4));
    if (max === undefined || next <= max) onChange(next);
  };
  const dec = () => {
    const next = parseFloat((value - step).toFixed(4));
    if (next >= min) onChange(next);
  };

  return (
    <div>
      <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">{label}</label>
      <div className="flex items-center gap-2">
        <button type="button" onClick={dec} disabled={disabled || value <= min} className={btnClass}>
          <Minus className="w-3.5 h-3.5" />
        </button>
        <div className="flex-1 relative">
          {prefix && <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">{prefix}</span>}
          <input
            type="number"
            step={step}
            min={min}
            max={max}
            value={value}
            onChange={(e) => {
              const v = parseFloat(e.target.value);
              if (!isNaN(v) && v >= min && (max === undefined || v <= max)) onChange(v);
            }}
            disabled={disabled}
            className={`w-full text-center py-2 ${prefix ? 'pl-7' : 'pl-3'} ${suffix ? 'pr-7' : 'pr-3'} bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-black text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none disabled:opacity-60`}
          />
          {suffix && <span className="absolute right-3 top-2.5 text-neutral-400 font-bold text-sm">{suffix}</span>}
        </div>
        <button type="button" onClick={inc} disabled={disabled || (max !== undefined && value >= max)} className={btnClass}>
          <Plus className="w-3.5 h-3.5" />
        </button>
      </div>
      {hint && <p className="text-[11px] text-neutral-500 mt-1">{hint}</p>}
    </div>
  );
};

// ---- Main Component ----
export const PricingControlCenter: React.FC<PricingControlCenterProps> = ({ actorName, canManage }) => {
  const [pricing, setPricing] = useState<PlatformPricingSettings>(DEFAULT_PRICING);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [calcOrderAmount, setCalcOrderAmount] = useState<number>(10.00);
  const [calcDeliveryKm, setCalcDeliveryKm] = useState<number>(3.0);

  useEffect(() => { fetchPricing(); }, []);

  const fetchPricing = async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const { data, error } = await supabase
        .from('platform_pricing_settings')
        .select('*')
        .eq('id', 'shiri_global_pricing')
        .single();

      if (error && error.code !== 'PGRST116') {
        console.warn('Could not fetch remote pricing, using defaults:', error.message);
      } else if (data) {
        setPricing({
          id: data.id || 'shiri_global_pricing',
          driver_daily_pass_usd: Number(data.driver_daily_pass_usd) || 0.25,
          driver_weekly_pass_usd: Number(data.driver_weekly_pass_usd) || 1.50,
          driver_monthly_pass_usd: Number(data.driver_monthly_pass_usd) || 5.00,
          driver_grace_period_hours: Number(data.driver_grace_period_hours) || 2,
          courier_base_delivery_fee_usd: Number(data.courier_base_delivery_fee_usd) || 2.00,
          courier_per_km_fee_usd: Number(data.courier_per_km_fee_usd) || 0.30,
          courier_share_percentage: Number(data.courier_share_percentage) || 80.0,
          courier_monthly_subscription_usd: Number(data.courier_monthly_subscription_usd) || 3.00,
          service_fee_usd: Number(data.service_fee_usd) || 0.50,
          night_surge_multiplier: Number(data.night_surge_multiplier) || 1.25,
          merchant_monthly_subscription_usd: Number(data.merchant_monthly_subscription_usd) || 3.00,
          merchant_default_commission_rate: Number(data.merchant_default_commission_rate) || 15.0,
          merchant_min_order_usd: Number(data.merchant_min_order_usd) || 3.00,
          updated_at: data.updated_at,
          updated_by: data.updated_by || 'Admin'
        });
      }
    } catch (err: any) {
      console.error('Error fetching pricing:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canManage) {
      alert('Ma lihid awoodda aad wax kaga beddesho qiimaha guud.');
      return;
    }
    setIsSaving(true);
    setSaveSuccess(false);
    setErrorMessage(null);
    try {
      const payload = {
        ...pricing,
        id: 'shiri_global_pricing',
        updated_at: new Date().toISOString(),
        updated_by: actorName || 'Admin'
      };
      const { error } = await supabase
        .from('platform_pricing_settings')
        .upsert(payload, { onConflict: 'id' });
      if (error) throw error;
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 5000);
    } catch (err: any) {
      setErrorMessage(`Khalad: ${err.message || 'Lama kaydin karin qiimaha.'}`);
    } finally {
      setIsSaving(false);
    }
  };

  const set = (key: keyof PlatformPricingSettings) => (val: number) =>
    setPricing(prev => ({ ...prev, [key]: val }));

  const computedDeliveryFee = pricing.courier_base_delivery_fee_usd + (calcDeliveryKm * pricing.courier_per_km_fee_usd);
  const merchantCommission = calcOrderAmount * (pricing.merchant_default_commission_rate / 100);
  const merchantPayout = calcOrderAmount - merchantCommission;
  const courierPayout = computedDeliveryFee * (pricing.courier_share_percentage / 100);
  const platformCourierCut = computedDeliveryFee - courierPayout;
  const platformTotalRevenue = merchantCommission + platformCourierCut + pricing.service_fee_usd;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-neutral-900 via-neutral-800 to-black text-white p-6 rounded-2xl shadow-sm border border-neutral-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="px-2.5 py-1 bg-red-600 text-white text-[10px] font-black rounded-lg uppercase tracking-wider">
              Play Store Update La'aan
            </span>
            <span className="text-xs text-neutral-400 font-medium">Live Cloud Sync</span>
          </div>
          <h2 className="text-2xl font-black mt-2 text-white flex items-center gap-2">
            ⚙️ Maamulka Qiimaha &amp; Boowdka
          </h2>
          <p className="text-xs text-neutral-300 mt-1 max-w-2xl leading-relaxed">
            Badhanka <strong>+</strong> iyo <strong>−</strong> isticmaal si aad u kordhin ama u dhimid qiimaha. 
            Isbeddel kasta wuxuu <strong>isla markiiba</strong> ugu dhaqan galayaa dhammaan isticmaalayaasha.
          </p>
        </div>
        <button
          onClick={fetchPricing}
          disabled={isLoading}
          className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-bold rounded-xl border border-neutral-700 transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50 shrink-0"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Dib u soo cusbooneysii
        </button>
      </div>

      {saveSuccess && (
        <div className="p-4 bg-emerald-50 border-2 border-emerald-500 text-emerald-950 rounded-2xl flex items-center gap-3">
          <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
          <div>
            <div className="text-sm font-black text-emerald-900">✓ Qiimaha Cusub Waa La Dhaqan Galiyay!</div>
            <div className="text-xs text-emerald-800 mt-0.5">
              Dhammaan telefoonada (Macmiilka, Ganacsatada, Darawallada) waxay hadda isticmaalayaan qiimahan cusub.
            </div>
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-red-50 border-2 border-red-500 rounded-2xl flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <div className="text-xs font-bold text-red-800">{errorMessage}</div>
        </div>
      )}

      <form onSubmit={handleSave} className="grid grid-cols-1 lg:grid-cols-3 gap-6">

        {/* ── COLUMN 1: DARAWAL BAJAAJ ── */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
          <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
            <div className="w-10 h-10 rounded-xl bg-orange-100 flex items-center justify-center text-orange-600">
              <Bike className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-neutral-900">🛺 Darawal Bajaaj Pass</h3>
              <p className="text-xs text-neutral-500">Qiimaha liisanka</p>
            </div>
          </div>

          <div className="space-y-5">
            <StepperField
              label="Pass-ka Maalinta ($ USD)"
              value={pricing.driver_daily_pass_usd}
              step={0.05}
              min={0.05}
              prefix="$"
              hint="Hadda: $0.25 maalintii (24 saac)"
              onChange={set('driver_daily_pass_usd')}
              disabled={!canManage}
              highlight="orange"
            />
            <StepperField
              label="Pass-ka Todobaadka ($ USD)"
              value={pricing.driver_weekly_pass_usd}
              step={0.25}
              min={0.50}
              prefix="$"
              onChange={set('driver_weekly_pass_usd')}
              disabled={!canManage}
              highlight="orange"
            />
            <StepperField
              label="Pass-ka Bisha ($ USD)"
              value={pricing.driver_monthly_pass_usd}
              step={0.50}
              min={1.00}
              prefix="$"
              onChange={set('driver_monthly_pass_usd')}
              disabled={!canManage}
              highlight="orange"
            />
            <StepperField
              label="Saacadaha Cafiska (Grace Period)"
              value={pricing.driver_grace_period_hours}
              step={1}
              min={0}
              max={24}
              suffix="h"
              hint="Saacadaha darawalka la sugayo"
              onChange={set('driver_grace_period_hours')}
              disabled={!canManage}
              highlight="orange"
            />
          </div>
        </div>

        {/* ── COLUMN 2: MOOTO DELIVERY COURIER ── */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
          <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
            <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600">
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-neutral-900">🏍️ Mooto Delivery Courier</h3>
              <p className="text-xs text-neutral-500">Qiimaha gaarsiinta &amp; Rukhumadda</p>
            </div>
          </div>

          {/* Monthly subscription highlight */}
          <div className="bg-blue-50 border border-blue-200 rounded-xl p-3">
            <p className="text-xs font-black text-blue-800 mb-0.5">📋 Rukhumadda Bishii (Subscription)</p>
            <p className="text-[11px] text-blue-600">Lacagta aan courier-ka bishii ka qaadano</p>
          </div>

          <div className="space-y-5">
            <StepperField
              label="Rukhumadda Bishii — Courier ($ USD)"
              value={pricing.courier_monthly_subscription_usd ?? 3.00}
              step={0.50}
              min={1.00}
              prefix="$"
              hint="Hadda: $3.00/Bishii • Admin oggolaanshiis"
              onChange={set('courier_monthly_subscription_usd')}
              disabled={!canManage}
              highlight="blue"
            />
            <StepperField
              label="Lacagta Gaarsiinta Aasaasiga (Base Fee $)"
              value={pricing.courier_base_delivery_fee_usd}
              step={0.25}
              min={0.50}
              prefix="$"
              hint="Qiimaha ugu yar ee xirmo kasta"
              onChange={set('courier_base_delivery_fee_usd')}
              disabled={!canManage}
              highlight="blue"
            />
            <StepperField
              label="Masaafada KM Kasta ($ / KM)"
              value={pricing.courier_per_km_fee_usd}
              step={0.05}
              min={0.00}
              prefix="$"
              onChange={set('courier_per_km_fee_usd')}
              disabled={!canManage}
              highlight="blue"
            />
            <StepperField
              label="Saamiga Courier-ka (%)"
              value={pricing.courier_share_percentage}
              step={5}
              min={50}
              max={100}
              suffix="%"
              hint="Tusaale: 80% courier, 20% SHIRI"
              onChange={set('courier_share_percentage')}
              disabled={!canManage}
              highlight="blue"
            />
            <StepperField
              label="Service Fee — SHIRI ($)"
              value={pricing.service_fee_usd}
              step={0.10}
              min={0.00}
              prefix="$"
              onChange={set('service_fee_usd')}
              disabled={!canManage}
              highlight="blue"
            />
          </div>
        </div>

        {/* ── COLUMN 3: MERCHANTS + CALCULATOR + SAVE ── */}
        <div className="space-y-6">
          {/* Merchants */}
          <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
            <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
              <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center text-emerald-600">
                <Store className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-black text-neutral-900">🏪 Ganacsatada (Merchants)</h3>
                <p className="text-xs text-neutral-500">Rukhumadda &amp; Commission-ka</p>
              </div>
            </div>

            <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3">
              <p className="text-xs font-black text-emerald-800 mb-0.5">📋 Rukhumadda Bishii (Subscription)</p>
              <p className="text-[11px] text-emerald-600">Lacagta aan merchant-ka bishii ka qaadano</p>
            </div>

            <div className="space-y-5">
              <StepperField
                label="Rukhumadda Bishii — Merchant ($ USD)"
                value={pricing.merchant_monthly_subscription_usd ?? 3.00}
                step={0.50}
                min={1.00}
                prefix="$"
                hint="Hadda: $3.00/Bishii • Admin oggolaanshiis"
                onChange={set('merchant_monthly_subscription_usd')}
                disabled={!canManage}
                highlight="green"
              />
              <StepperField
                label="Commission-ka Guud (%)"
                value={pricing.merchant_default_commission_rate}
                step={0.5}
                min={0}
                max={50}
                suffix="%"
                hint="Caadiyan: 15% (Maqaayadaha, Supermarket-yada)"
                onChange={set('merchant_default_commission_rate')}
                disabled={!canManage}
                highlight="green"
              />
              <StepperField
                label="Dalabka Ugu Yar ($ Min Order)"
                value={pricing.merchant_min_order_usd}
                step={0.50}
                min={1.00}
                prefix="$"
                onChange={set('merchant_min_order_usd')}
                disabled={!canManage}
                highlight="green"
              />
            </div>
          </div>

          {/* Live Calculator */}
          <div className="bg-gradient-to-br from-neutral-900 to-neutral-950 text-white p-5 rounded-2xl border border-neutral-800 space-y-3">
            <div className="flex items-center justify-between text-xs font-black uppercase text-red-400">
              <span className="flex items-center gap-1.5"><TrendingUp className="w-3.5 h-3.5" /> Xisaab Tusaale</span>
              <span>1 Dalab</span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label className="text-[10px] text-neutral-400">Alaabta ($):</label>
                <input
                  type="number"
                  value={calcOrderAmount}
                  onChange={(e) => setCalcOrderAmount(parseFloat(e.target.value) || 0)}
                  className="w-full px-2 py-1 bg-neutral-800 border border-neutral-700 rounded-lg text-white font-bold"
                />
              </div>
              <div>
                <label className="text-[10px] text-neutral-400">Masaafada (KM):</label>
                <input
                  type="number"
                  value={calcDeliveryKm}
                  onChange={(e) => setCalcDeliveryKm(parseFloat(e.target.value) || 0)}
                  className="w-full px-2 py-1 bg-neutral-800 border border-neutral-700 rounded-lg text-white font-bold"
                />
              </div>
            </div>
            <div className="pt-2 border-t border-neutral-800 space-y-1.5 text-xs">
              <div className="flex justify-between text-neutral-400">
                <span>Gaarsiinta Wadarta:</span>
                <span className="text-white font-bold">${computedDeliveryFee.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-neutral-400">
                <span>Dakhliga Ganacsadaha ({100 - pricing.merchant_default_commission_rate}%):</span>
                <span className="text-white font-bold">${merchantPayout.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-neutral-400">
                <span>Dakhliga Courier ({pricing.courier_share_percentage}%):</span>
                <span className="text-blue-400 font-bold">${courierPayout.toFixed(2)}</span>
              </div>
              <div className="flex justify-between font-black pt-1 border-t border-neutral-800">
                <span className="text-emerald-400">Faa'iidada SHIRI:</span>
                <span className="text-emerald-400 text-sm">${platformTotalRevenue.toFixed(2)}</span>
              </div>
            </div>
          </div>

          {/* Save Button */}
          <button
            type="submit"
            disabled={isSaving || !canManage}
            className="w-full py-3.5 bg-red-600 hover:bg-red-700 text-white font-black rounded-xl shadow-lg transition flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
          >
            <Save className="w-4 h-4" />
            {isSaving ? 'Waxaa lagu kaydinayaa Cloud-ka...' : 'Kaydi Qiimaha Cusub (Save Live)'}
          </button>
        </div>
      </form>
    </div>
  );
};
