import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { ShiriMerchant } from '../types';
import { Store, CheckCircle, XCircle, Search, RefreshCw, Plus, Edit2, ShieldCheck, MapPin, Phone, Star } from 'lucide-react';

interface MerchantsManagementProps {
  canManage: boolean;
  actorName: string;
}

export const MerchantsManagement: React.FC<MerchantsManagementProps> = ({ canManage, actorName }) => {
  const [merchants, setMerchants] = useState<ShiriMerchant[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');

  useEffect(() => {
    fetchMerchants();
  }, []);

  const fetchMerchants = async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('merchants')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) {
        console.warn('Could not fetch merchants, using mock preview:', error.message);
        // Fallback default Somali merchants
        setMerchants([
          {
            id: 'm1',
            name: 'Pizza House Mogadishu',
            category: 'cunto',
            district: 'Hodan',
            phone: '0615551024',
            rating: 4.9,
            review_count: 142,
            delivery_time_minutes: 25,
            min_order: 3.00,
            delivery_fee: 2.00,
            is_open: true,
            verification_status: 'verified',
            commission_rate: 15.00
          },
          {
            id: 'm2',
            name: 'Al-Najax Central Pharmacy',
            category: 'daawo',
            district: 'Taleex',
            phone: '0615554911',
            rating: 4.9,
            review_count: 88,
            delivery_time_minutes: 15,
            min_order: 2.00,
            delivery_fee: 1.50,
            is_open: true,
            verification_status: 'verified',
            commission_rate: 12.00
          },
          {
            id: 'm3',
            name: 'Bakaaro Fresh Supermarket',
            category: 'supermarket',
            district: 'Bakaaro',
            phone: '0615553392',
            rating: 4.7,
            review_count: 210,
            delivery_time_minutes: 30,
            min_order: 5.00,
            delivery_fee: 2.00,
            is_open: true,
            verification_status: 'verified',
            commission_rate: 10.00
          },
          {
            id: 'm4',
            name: 'Hilibka Geela & Ariga Hodan',
            category: 'hilib_qudaar',
            district: 'Hodan',
            phone: '0615556012',
            rating: 4.8,
            review_count: 75,
            delivery_time_minutes: 20,
            min_order: 5.00,
            delivery_fee: 1.50,
            is_open: true,
            verification_status: 'verified',
            commission_rate: 12.00
          },
          {
            id: 'm5',
            name: 'Hormuud Telecom Store',
            category: 'electronics',
            district: 'KM4',
            phone: '0615557011',
            rating: 4.9,
            review_count: 160,
            delivery_time_minutes: 30,
            min_order: 10.00,
            delivery_fee: 2.50,
            is_open: true,
            verification_status: 'verified',
            commission_rate: 8.00
          }
        ]);
      } else if (data) {
        setMerchants(data as ShiriMerchant[]);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const toggleOpenStatus = async (merchantId: string, currentStatus: boolean) => {
    if (!canManage) return;
    try {
      const { error } = await supabase
        .from('merchants')
        .update({ is_open: !currentStatus })
        .eq('id', merchantId);

      if (error) throw error;
      setMerchants(merchants.map(m => m.id === merchantId ? { ...m, is_open: !currentStatus } : m));
    } catch (err: any) {
      alert(`Khalad: ${err.message}`);
    }
  };

  const updateCommission = async (merchantId: string, newRate: number) => {
    if (!canManage) return;
    try {
      const { error } = await supabase
        .from('merchants')
        .update({ commission_rate: newRate })
        .eq('id', merchantId);

      if (error) throw error;
      setMerchants(merchants.map(m => m.id === merchantId ? { ...m, commission_rate: newRate } : m));
      alert('Commission rate updated successfully!');
    } catch (err: any) {
      alert(`Khalad: ${err.message}`);
    }
  };

  const filteredMerchants = merchants.filter(m => {
    const matchesSearch = searchQuery === '' || 
      m.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.district.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || m.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-neutral-900 flex items-center gap-2">
            <Store className="w-5 h-5 text-red-600" />
            Maamulka Ganacsatada (Merchants & Stores)
          </h2>
          <p className="text-xs text-neutral-500 mt-1">
            Liiska dhammaan dukaamada ku jira SHIRI App: xakamee commission-ka, furan/xiran, iyo xaqiijinta rasmiga ah.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={fetchMerchants}
            className="px-3.5 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-bold rounded-xl transition flex items-center gap-1.5 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 absolute left-3 top-3 text-neutral-400" />
          <input
            type="text"
            placeholder="Raadi dukaan, degmo..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs font-bold text-neutral-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-red-600"
          />
        </div>

        <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {['all', 'cunto', 'supermarket', 'daawo', 'hilib_qudaar', 'electronics'].map(cat => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition whitespace-nowrap cursor-pointer ${
                selectedCategory === cat ? 'bg-red-600 text-white' : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              {cat === 'all' ? 'Dhammaan' : cat.toUpperCase()}
            </button>
          ))}
        </div>
      </div>

      {/* Merchants Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-500 font-black uppercase text-[10px]">
                <th className="py-3.5 px-4">Ganacsiga</th>
                <th className="py-3.5 px-4">Qeybta</th>
                <th className="py-3.5 px-4">Degmada</th>
                <th className="py-3.5 px-4">Taleefanka</th>
                <th className="py-3.5 px-4">Commission %</th>
                <th className="py-3.5 px-4">Xaaladda</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {filteredMerchants.map(merchant => (
                <tr key={merchant.id} className="hover:bg-neutral-50/60 transition">
                  <td className="py-3.5 px-4">
                    <div className="font-black text-neutral-900 flex items-center gap-2">
                      <span>{merchant.name}</span>
                      {merchant.verification_status === 'verified' && (
                        <ShieldCheck className="w-4 h-4 text-blue-600" title="Verified Merchant" />
                      )}
                    </div>
                    <div className="text-[11px] text-neutral-400 flex items-center gap-1 mt-0.5">
                      <Star className="w-3 h-3 text-amber-500 fill-amber-500" />
                      <span>{merchant.rating} ({merchant.review_count} reviews)</span>
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="px-2 py-0.5 bg-neutral-100 text-neutral-700 rounded-md font-bold text-[11px] uppercase">
                      {merchant.category}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-bold text-neutral-700">
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3.5 h-3.5 text-neutral-400" />
                      {merchant.district}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-mono text-neutral-600">
                    {merchant.phone}
                  </td>
                  <td className="py-3.5 px-4 font-black text-red-600">
                    <div className="flex items-center gap-1.5">
                      <span>{merchant.commission_rate}%</span>
                      {canManage && (
                        <button
                          onClick={() => {
                            const newRate = prompt(`Geli commission rate-ka cusub ee ${merchant.name}:`, merchant.commission_rate.toString());
                            if (newRate !== null) {
                              const parsed = parseFloat(newRate);
                              if (!isNaN(parsed)) updateCommission(merchant.id, parsed);
                            }
                          }}
                          className="text-[10px] text-neutral-400 hover:text-neutral-900 underline"
                        >
                          Beddel
                        </button>
                      )}
                    </div>
                  </td>
                  <td className="py-3.5 px-4">
                    <button
                      onClick={() => toggleOpenStatus(merchant.id, merchant.is_open)}
                      disabled={!canManage}
                      className={`px-2.5 py-1 rounded-full text-[10px] font-black cursor-pointer transition ${
                        merchant.is_open
                          ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                          : 'bg-neutral-100 text-neutral-500 hover:bg-neutral-200'
                      }`}
                    >
                      {merchant.is_open ? '✓ Furan (Open)' : '✕ Xiran (Closed)'}
                    </button>
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <span className="text-[11px] font-bold text-neutral-400">
                      Min: ${merchant.min_order} • Delivery: ${merchant.delivery_fee}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

