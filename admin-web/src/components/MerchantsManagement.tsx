import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { ShiriMerchant } from '../types';
import { Store, CheckCircle, XCircle, Search, RefreshCw, Plus, Edit2, ShieldCheck, MapPin, Phone, Star, Package, X, Trash2 } from 'lucide-react';

interface MerchantsManagementProps {
  canManage: boolean;
  actorName: string;
}

interface MerchantProduct {
  id: string;
  name: string;
  price: number;
  category: string;
  in_stock: boolean;
}

export const MerchantsManagement: React.FC<MerchantsManagementProps> = ({ canManage, actorName }) => {
  const [merchants, setMerchants] = useState<ShiriMerchant[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedMerchantForProducts, setSelectedMerchantForProducts] = useState<ShiriMerchant | null>(null);
  const [merchantProducts, setMerchantProducts] = useState<MerchantProduct[]>([
    { id: 'p1', name: 'Margherita Pizza', price: 8.50, category: 'Pizza', in_stock: true },
    { id: 'p2', name: 'Chicken Burger Special', price: 5.00, category: 'Burger', in_stock: true },
    { id: 'p3', name: 'Crispy French Fries', price: 2.50, category: 'Fast Food', in_stock: true },
    { id: 'p4', name: 'Coca-Cola Can 330ml', price: 1.00, category: 'Drinks', in_stock: true },
    { id: 'p5', name: 'Beef Somali Shawarma', price: 4.50, category: 'Fast Food', in_stock: true }
  ]);
  const [newProdName, setNewProdName] = useState<string>('');
  const [newProdPrice, setNewProdPrice] = useState<string>('');
  const [newProdCategory, setNewProdCategory] = useState<string>('Cunto');

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
        // Fallback default Somali merchants matching master mockups
        setMerchants([
          {
            id: 'm0',
            name: 'Al-Furqan Restaurant',
            category: 'cunto',
            district: 'Waberi, Muqdisho',
            phone: '0615123456',
            rating: 4.7,
            review_count: 2500,
            delivery_time_minutes: 30,
            min_order: 5.00,
            delivery_fee: 1.00,
            is_open: true,
            shiri_address_code: 'SHR-MG-0012',
            digital_guri_number: 'SHR-MG-0012',
            verification_status: 'verified',
            commission_rate: 15.00
          },
          {
            id: 'm1',
            name: 'Pizza House Mogadishu',
            category: 'cunto',
            district: 'Hodan, Muqdisho',
            phone: '0615551024',
            rating: 4.9,
            review_count: 142,
            delivery_time_minutes: 25,
            min_order: 3.00,
            delivery_fee: 2.00,
            is_open: true,
            shiri_address_code: 'SHR-MG-48291',
            digital_guri_number: 'SHR-MG-48291',
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
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition whitespace-nowrap cursor-pointer ${selectedCategory === cat ? 'bg-red-600 text-white' : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
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
                    {(merchant.digital_guri_number || merchant.shiri_address_code) && (
                      <span className="inline-block mt-0.5 px-1.5 py-0.5 bg-amber-50 text-amber-800 rounded font-mono text-[10px] font-bold">
                        🏠 Guri: {merchant.digital_guri_number || merchant.shiri_address_code}
                      </span>
                    )}
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
                      className={`px-2.5 py-1 rounded-full text-[10px] font-black cursor-pointer transition ${merchant.is_open
                          ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200'
                          : 'bg-neutral-100 text-neutral-500 hover:bg-neutral-200'
                        }`}
                    >
                      {merchant.is_open ? '✓ Furan (Open)' : '✕ Xiran (Closed)'}
                    </button>
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <button
                      onClick={() => setSelectedMerchantForProducts(merchant)}
                      className="px-3 py-1 bg-red-50 hover:bg-red-100 text-red-600 font-bold rounded-lg text-xs transition flex items-center gap-1 ml-auto cursor-pointer"
                    >
                      <Package className="w-3.5 h-3.5" />
                      Alaabta (Products)
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Merchant Products Management Modal */}
      {selectedMerchantForProducts && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-neutral-100 pb-4">
              <div>
                <h3 className="text-base font-black text-neutral-900 flex items-center gap-2">
                  <Package className="w-5 h-5 text-red-600" />
                  Maamulka Alaabta: {selectedMerchantForProducts.name}
                </h3>
                <p className="text-xs text-neutral-500 mt-0.5">
                  Degmada {selectedMerchantForProducts.district} • {selectedMerchantForProducts.phone}
                </p>
              </div>
              <button
                onClick={() => setSelectedMerchantForProducts(null)}
                className="p-1.5 hover:bg-neutral-100 rounded-lg text-neutral-400 hover:text-neutral-700 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Add Product Form */}
            <div className="bg-neutral-50 p-4 rounded-xl border border-neutral-200 space-y-3">
              <h4 className="text-xs font-black text-neutral-800 uppercase tracking-wide">+ Kudar Alaab Cusub</h4>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <input
                  type="text"
                  placeholder="Magaca Alaabta"
                  value={newProdName}
                  onChange={(e) => setNewProdName(e.target.value)}
                  className="px-3 py-2 bg-white border border-neutral-200 rounded-lg text-xs font-medium focus:ring-2 focus:ring-red-600 outline-none"
                />
                <input
                  type="number"
                  step="0.01"
                  placeholder="Qiimaha ($)"
                  value={newProdPrice}
                  onChange={(e) => setNewProdPrice(e.target.value)}
                  className="px-3 py-2 bg-white border border-neutral-200 rounded-lg text-xs font-medium focus:ring-2 focus:ring-red-600 outline-none"
                />
                <button
                  onClick={() => {
                    const price = parseFloat(newProdPrice);
                    if (newProdName.trim() && !isNaN(price) && price > 0) {
                      setMerchantProducts([
                        ...merchantProducts,
                        {
                          id: 'p-' + Date.now(),
                          name: newProdName.trim(),
                          price,
                          category: newProdCategory,
                          in_stock: true
                        }
                      ]);
                      setNewProdName('');
                      setNewProdPrice('');
                      alert(`Alaab '${newProdName}' waa lagu daray!`);
                    } else {
                      alert('Fadlan geli magac iyo qiimo sax ah');
                    }
                  }}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold transition cursor-pointer"
                >
                  + Kudar Alaabta
                </button>
              </div>
            </div>

            {/* Products List */}
            <div className="space-y-2">
              <h4 className="text-xs font-black text-neutral-800 uppercase tracking-wide">
                Alaabta Ku Jirta Dukaanka ({merchantProducts.length})
              </h4>
              <div className="divide-y divide-neutral-100 border border-neutral-200 rounded-xl overflow-hidden">
                {merchantProducts.map((p) => (
                  <div key={p.id} className="p-3.5 flex items-center justify-between hover:bg-neutral-50/60 transition bg-white">
                    <div>
                      <div className="font-bold text-xs text-neutral-900">{p.name}</div>
                      <div className="text-[11px] text-neutral-500">${p.price.toFixed(2)} • {p.category}</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => {
                          setMerchantProducts(merchantProducts.map(item =>
                            item.id === p.id ? { ...item, in_stock: !item.in_stock } : item
                          ));
                        }}
                        className={`px-2 py-1 rounded-md text-[10px] font-bold cursor-pointer transition ${p.in_stock ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-700'
                          }`}
                      >
                        {p.in_stock ? 'In Stock' : 'Out of Stock'}
                      </button>
                      <button
                        onClick={() => {
                          if (confirm(`Ma hubtaa inaad tirtirto '${p.name}'?`)) {
                            setMerchantProducts(merchantProducts.filter(item => item.id !== p.id));
                          }
                        }}
                        className="p-1 hover:bg-red-50 text-neutral-400 hover:text-red-600 rounded transition cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedMerchantForProducts(null)}
                className="px-4 py-2 bg-neutral-900 hover:bg-black text-white rounded-xl text-xs font-bold transition cursor-pointer"
              >
                Done / Xir
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

