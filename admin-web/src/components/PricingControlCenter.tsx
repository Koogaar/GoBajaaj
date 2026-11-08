import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { PlatformPricingSettings } from '../types';
import { DollarSign, Save, RefreshCw, CheckCircle, TrendingUp, Shield, Truck, Store, Bike, AlertCircle } from 'lucide-react';

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
  service_fee_usd: 0.50,
  night_surge_multiplier: 1.25,
  merchant_default_commission_rate: 15.0,
  merchant_min_order_usd: 3.00,
  updated_by: 'Super Admin'
};

export const PricingControlCenter: React.FC<PricingControlCenterProps> = ({ actorName, canManage }) => {
  const [pricing, setPricing] = useState<PlatformPricingSettings>(DEFAULT_PRICING);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [saveSuccess, setSaveSuccess] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Example Calculator State
  const [calcOrderAmount, setCalcOrderAmount] = useState<number>(10.00);
  const [calcDeliveryKm, setCalcDeliveryKm] = useState<number>(3.0);

  useEffect(() => {
    fetchPricing();
  }, []);

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
        console.warn('Could not fetch remote pricing settings, using defaults:', error.message);
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
          service_fee_usd: Number(data.service_fee_usd) || 0.50,
          night_surge_multiplier: Number(data.night_surge_multiplier) || 1.25,
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

      if (error) {
        throw error;
      }

      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 5000);
    } catch (err: any) {
      console.error('Failed to save pricing to Supabase:', err);
      setErrorMessage(`Khalad: ${err.message || 'Lama kaydin karin qiimaha. Fadlan hubi xiriirka Supabase.'}`);
    } finally {
      setIsSaving(false);
    }
  };

  // Live Calculations for Preview Card
  const computedDeliveryFee = pricing.courier_base_delivery_fee_usd + (calcDeliveryKm * pricing.courier_per_km_fee_usd);
  const merchantCommission = (calcOrderAmount * (pricing.merchant_default_commission_rate / 100));
  const merchantPayout = calcOrderAmount - merchantCommission;
  const courierPayout = computedDeliveryFee * (pricing.courier_share_percentage / 100);
  const platformCourierCut = computedDeliveryFee - courierPayout;
  const platformTotalRevenue = merchantCommission + platformCourierCut + pricing.service_fee_usd;

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-neutral-900 via-neutral-800 to-black text-white p-6 rounded-2xl shadow-sm border border-neutral-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <span className="px-2.5 py-1 bg-red-600 text-white text-[10px] font-black rounded-lg uppercase tracking-wider">
              Play Store Updates La'aan
            </span>
            <span className="text-xs text-neutral-400 font-medium">
              Live Cloud Synchronization
            </span>
          </div>
          <h2 className="text-2xl font-black mt-2 text-white flex items-center gap-2">
            ⚙️ Maamulka Qiimaha & Boowdka (Pricing & Commissions)
          </h2>
          <p className="text-xs text-neutral-300 mt-1 max-w-2xl leading-relaxed">
            Halkan ka beddel qiimaha darawal bajaajta ($0.25), boqolkiiba delivery-ga mootada, iyo commission-ka ganacsatada. 
            Isbeddel kasta wuxuu <strong>isla markiiba toos ugu dhaqan galayaa</strong> dhammaan dadka app-ka isticmaalaya iyadoon loo baahnayn cusbooneysiin cusub oo Play Store ah!
          </p>
        </div>

        <div className="flex items-center gap-3 shrink-0">
          <button
            onClick={fetchPricing}
            disabled={isLoading}
            className="px-4 py-2 bg-neutral-800 hover:bg-neutral-700 text-white text-xs font-bold rounded-xl border border-neutral-700 transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Dib u soo cusbooneysii
          </button>
        </div>
      </div>

      {saveSuccess && (
        <div className="p-4 bg-emerald-50 border-2 border-emerald-500 text-emerald-950 rounded-2xl flex items-center gap-3 shadow-xs animate-in fade-in">
          <CheckCircle className="w-5 h-5 text-emerald-600 shrink-0" />
          <div>
            <div className="text-sm font-black text-emerald-900">
              ✓ Qiimaha Cusub Waa La Dhaqan Galiyay!
            </div>
            <div className="text-xs text-emerald-800 mt-0.5">
              Dhammaan telefoonada isticmaaleyaasha (Macmiilka, Ganacsatada, iyo Darawallada) waxay hadda isticmaalayaan qiimahan cusub.
            </div>
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 bg-red-50 border-2 border-red-500 text-red-950 rounded-2xl flex items-center gap-3 shadow-xs">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <div className="text-xs font-bold text-red-800">
            {errorMessage}
          </div>
        </div>
      )}

      <form onSubmit={handleSave} className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* COLUMN 1: DARAWAL BAJAAJ PASS PRICING */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
          <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
            <div className="w-10 h-10 rounded-xl bg-orange-100 flex items-center justify-center text-orange-600">
              <Bike className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-neutral-900">🛺 Darawal Bajaaj Pass</h3>
              <p className="text-xs text-neutral-500">Qiimaha liisanka maalinlaha ah</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Pass-ka Maalinta ($ USD)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.01"
                  min="0.05"
                  value={pricing.driver_daily_pass_usd}
                  onChange={(e) => setPricing({ ...pricing, driver_daily_pass_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
              <p className="text-[11px] text-neutral-500 mt-1">Hadda: $0.25 maalintii (24 saac)</p>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Pass-ka Todobaadka ($ USD)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.05"
                  min="0.50"
                  value={pricing.driver_weekly_pass_usd}
                  onChange={(e) => setPricing({ ...pricing, driver_weekly_pass_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Pass-ka Bisha ($ USD)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.10"
                  min="1.00"
                  value={pricing.driver_monthly_pass_usd}
                  onChange={(e) => setPricing({ ...pricing, driver_monthly_pass_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Saacadaha Cafiska (Grace Period)
              </label>
              <input
                type="number"
                min="0"
                max="24"
                value={pricing.driver_grace_period_hours}
                onChange={(e) => setPricing({ ...pricing, driver_grace_period_hours: parseInt(e.target.value) || 0 })}
                className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                required
              />
              <p className="text-[11px] text-neutral-500 mt-1">Saacadaha darawalka la sugayo ka hor inta aan la xirin</p>
            </div>
          </div>
        </div>

        {/* COLUMN 2: MOOTO DELIVERY COURIER PRICING */}
        <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
          <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
            <div className="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center text-blue-600">
              <Truck className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black text-neutral-900">🏍️ Mooto Delivery</h3>
              <p className="text-xs text-neutral-500">Gaarsiinta baakadaha & cuntada</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Lacagta Gaarsiinta Aasaasiga ah (Base Fee $)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.10"
                  min="0.50"
                  value={pricing.courier_base_delivery_fee_usd}
                  onChange={(e) => setPricing({ ...pricing, courier_base_delivery_fee_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
              <p className="text-[11px] text-neutral-500 mt-1">Qiimaha ugu yar ee xirmo kasta lagu qaado</p>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Masaafada KM Kasta ($ USD / KM)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.05"
                  min="0.00"
                  value={pricing.courier_per_km_fee_usd}
                  onChange={(e) => setPricing({ ...pricing, courier_per_km_fee_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Saamiga Darawalka Mootada (%)
              </label>
              <div className="relative">
                <input
                  type="number"
                  step="1"
                  min="50"
                  max="100"
                  value={pricing.courier_share_percentage}
                  onChange={(e) => setPricing({ ...pricing, courier_share_percentage: parseFloat(e.target.value) || 0 })}
                  className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
                <span className="absolute right-3 top-2.5 text-neutral-400 font-bold text-sm">%</span>
              </div>
              <p className="text-[11px] text-neutral-500 mt-1">Tusaale 80% darawalka, 20% SHIRI Platform</p>
            </div>

            <div>
              <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                Adeegga SHIRI (Service Fee $)
              </label>
              <div className="relative">
                <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                <input
                  type="number"
                  step="0.05"
                  min="0.00"
                  value={pricing.service_fee_usd}
                  onChange={(e) => setPricing({ ...pricing, service_fee_usd: parseFloat(e.target.value) || 0 })}
                  className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                  required
                />
              </div>
            </div>
          </div>
        </div>

        {/* COLUMN 3: GANACSATADA (MERCHANTS) & LIVE SIMULATOR */}
        <div className="space-y-6">
          <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs space-y-5">
            <div className="flex items-center space-x-3 pb-3 border-b border-neutral-100">
              <div className="w-10 h-10 rounded-xl bg-emerald-100 flex items-center justify-center text-emerald-600">
                <Store className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-base font-black text-neutral-900">🏪 Ganacsatada (Merchants)</h3>
                <p className="text-xs text-neutral-500">Commission-ka & dalabyada</p>
              </div>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                  Commission-ka Guud (%)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    max="50"
                    value={pricing.merchant_default_commission_rate}
                    onChange={(e) => setPricing({ ...pricing, merchant_default_commission_rate: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                    required
                  />
                  <span className="absolute right-3 top-2.5 text-neutral-400 font-bold text-sm">%</span>
                </div>
                <p className="text-[11px] text-neutral-500 mt-1">Caadiyan: 15% (Maqaayadaha, Supermarket-yada, Farmashiyeyaasha)</p>
              </div>

              <div>
                <label className="block text-xs font-black text-neutral-700 uppercase mb-1.5">
                  Dalabka Ugu Yar ($ Min Order)
                </label>
                <div className="relative">
                  <span className="absolute left-3 top-2.5 text-neutral-400 font-bold text-sm">$</span>
                  <input
                    type="number"
                    step="0.50"
                    min="1.00"
                    value={pricing.merchant_min_order_usd}
                    onChange={(e) => setPricing({ ...pricing, merchant_min_order_usd: parseFloat(e.target.value) || 0 })}
                    className="w-full pl-8 pr-3 py-2 bg-neutral-50 border border-neutral-300 rounded-xl text-sm font-bold text-neutral-900 focus:bg-white focus:ring-2 focus:ring-red-600 focus:outline-none"
                    required
                  />
                </div>
              </div>
            </div>
          </div>

          {/* REAL-TIME PREVIEW CALCULATOR */}
          <div className="bg-gradient-to-br from-neutral-900 to-neutral-950 text-white p-5 rounded-2xl border border-neutral-800 shadow-sm space-y-3">
            <div className="flex items-center justify-between text-xs font-black uppercase text-red-500">
              <span className="flex items-center gap-1.5"><TrendingUp className="w-3.5 h-3.5" /> Xisaab-Tusaale Toos ah</span>
              <span>1 Dalab</span>
            </div>

            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label className="text-[10px] text-neutral-400">Alaabta Qiimaheeda ($):</label>
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
                <span>Dakhliga Mootada ({pricing.courier_share_percentage}%):</span>
                <span className="text-blue-400 font-bold">${courierPayout.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-neutral-300 font-black pt-1 border-t border-neutral-800">
                <span className="text-emerald-400">Faa'iidada SHIRI Platform:</span>
                <span className="text-emerald-400 font-black text-sm">${platformTotalRevenue.toFixed(2)}</span>
              </div>
            </div>
          </div>

          {/* SUBMIT BUTTON */}
          <button
            type="submit"
            disabled={isSaving}
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

