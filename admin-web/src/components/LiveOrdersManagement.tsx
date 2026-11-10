import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { ShiriOrder } from '../types';
import { ShoppingBag, RefreshCw, Key, CheckCircle, Clock, Truck, Search, ShieldCheck, X, Phone, MapPin, DollarSign } from 'lucide-react';

interface LiveOrdersManagementProps {
  canManage: boolean;
  actorName: string;
}

export const LiveOrdersManagement: React.FC<LiveOrdersManagementProps> = ({ canManage, actorName }) => {
  const [orders, setOrders] = useState<ShiriOrder[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedOrderModal, setSelectedOrderModal] = useState<ShiriOrder | null>(null);

  useEffect(() => {
    fetchOrders();
  }, []);

  const fetchOrders = async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('orders')
        .select('*')
        .order('created_at', { ascending: false })
        .limit(50);

      if (error) {
        console.warn('Could not fetch remote orders, using active mock orders:', error.message);
        setOrders([
          {
            id: 'ord-101',
            order_number: '#SHR-28491',
            customer_name: 'Cabdiraxmaan Cali',
            customer_phone: '0618092060',
            merchant_name: 'Pizza House Mogadishu',
            courier_name: 'Maxamed Axmed (Mooto)',
            subtotal: 10.50,
            delivery_fee: 2.00,
            service_fee: 0.50,
            total: 13.00,
            status: 'in_transit',
            payment_method: 'evc_plus',
            payment_status: 'paid',
            handover_pin: '4792',
            district: 'Hodan',
            created_at: new Date(Date.now() - 1000 * 60 * 12).toISOString()
          },
          {
            id: 'ord-102',
            order_number: '#SHR-28490',
            customer_name: 'Nimco Jaamac',
            customer_phone: '0615559810',
            merchant_name: 'Al-Najax Pharmacy',
            courier_name: 'Cali Xasan (Mooto)',
            subtotal: 8.00,
            delivery_fee: 1.50,
            service_fee: 0.50,
            total: 10.00,
            status: 'preparing',
            payment_method: 'edahab',
            payment_status: 'paid',
            handover_pin: '6182',
            district: 'Taleex',
            created_at: new Date(Date.now() - 1000 * 60 * 25).toISOString()
          },
          {
            id: 'ord-103',
            order_number: '#SHR-28489',
            customer_name: 'Axmed Yuusuf',
            customer_phone: '0612345678',
            merchant_name: 'Bakaaro Fresh Supermarket',
            courier_name: 'Faarax Nuur',
            subtotal: 24.50,
            delivery_fee: 2.00,
            service_fee: 0.50,
            total: 27.00,
            status: 'delivered',
            payment_method: 'cash_on_delivery',
            payment_status: 'paid',
            handover_pin: '1945',
            district: 'Bakaaro',
            created_at: new Date(Date.now() - 1000 * 60 * 65).toISOString()
          }
        ]);
      } else if (data) {
        setOrders(data as ShiriOrder[]);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateStatus = async (orderId: string, nextStatus: string) => {
    if (!canManage) return;
    try {
      const { error } = await supabase
        .from('orders')
        .update({ status: nextStatus })
        .eq('id', orderId);

      if (error) throw error;
      setOrders(orders.map(o => o.id === orderId ? { ...o, status: nextStatus as any } : o));
    } catch (err: any) {
      alert(`Khalad: ${err.message}`);
    }
  };

  const filteredOrders = orders.filter(o => {
    const matchesStatus = statusFilter === 'all' || o.status === statusFilter;
    const matchesSearch = searchQuery === '' ||
      o.order_number.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (o.customer_name && o.customer_name.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (o.merchant_name && o.merchant_name.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesStatus && matchesSearch;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-neutral-900 flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-red-600" />
            Dalabyada Tooska ah (Live Orders & Dispatch)
          </h2>
          <p className="text-xs text-neutral-500 mt-1">
            Raadinta tooska ah ee dalabyada, xaqiijinta koodhka sirta ah ee Handover PIN, iyo u-qoondeynta mootada.
          </p>
        </div>

        <button
          onClick={fetchOrders}
          className="px-3.5 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-bold rounded-xl transition flex items-center gap-1.5 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white p-4 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row gap-3 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 absolute left-3 top-3 text-neutral-400" />
          <input
            type="text"
            placeholder="Raadi Order #, Macmiil, Dukaan..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-3 py-2 bg-neutral-50 border border-neutral-200 rounded-xl text-xs font-bold text-neutral-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-red-600"
          />
        </div>

        <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {[
            { id: 'all', label: 'Dhammaan' },
            { id: 'pending', label: 'Cusub (Pending)' },
            { id: 'preparing', label: 'Diyaarinta (Prep)' },
            { id: 'in_transit', label: 'Waddada (In Transit)' },
            { id: 'delivered', label: 'La Dhiibay (Delivered)' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition whitespace-nowrap cursor-pointer ${
                statusFilter === tab.id ? 'bg-red-600 text-white' : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-500 font-black uppercase text-[10px]">
                <th className="py-3.5 px-4">Order #</th>
                <th className="py-3.5 px-4">Dukaanka</th>
                <th className="py-3.5 px-4">Macmiilka</th>
                <th className="py-3.5 px-4">Mootada (Courier)</th>
                <th className="py-3.5 px-4">Handover PIN</th>
                <th className="py-3.5 px-4">Wadarta</th>
                <th className="py-3.5 px-4">Xaaladda</th>
                <th className="py-3.5 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-100">
              {filteredOrders.map(order => (
                <tr key={order.id} className="hover:bg-neutral-50/60 transition">
                  <td className="py-3.5 px-4">
                    <span className="font-mono font-black text-red-600 bg-red-50 px-2 py-0.5 rounded-md">
                      {order.order_number}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-bold text-neutral-900">
                    {order.merchant_name || 'Pizza House'}
                  </td>
                  <td className="py-3.5 px-4">
                    <div className="font-bold text-neutral-800">{order.customer_name || 'Macmiil'}</div>
                    <div className="text-[10.5px] font-mono text-neutral-400">{order.customer_phone || order.district}</div>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="font-medium text-neutral-700 flex items-center gap-1">
                      <Truck className="w-3.5 h-3.5 text-blue-500" />
                      {order.courier_name || 'Lama xil-saarin'}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className="px-2.5 py-1 bg-neutral-900 text-white rounded-lg font-mono font-black text-xs tracking-wider">
                      {order.handover_pin}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 font-black text-neutral-900">
                    ${order.total.toFixed(2)}
                    <span className="block text-[10px] text-neutral-400 font-normal uppercase">
                      {order.payment_method.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <span className={`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                      order.status === 'delivered' ? 'bg-emerald-100 text-emerald-800' :
                      order.status === 'in_transit' ? 'bg-blue-100 text-blue-800' :
                      order.status === 'preparing' ? 'bg-amber-100 text-amber-800' :
                      'bg-neutral-100 text-neutral-800'
                    }`}>
                      {order.status.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => setSelectedOrderModal(order)}
                        className="px-2.5 py-1 bg-neutral-100 hover:bg-neutral-200 text-neutral-800 text-[10px] font-bold rounded-lg transition cursor-pointer"
                      >
                        Arag →
                      </button>
                      {canManage && order.status !== 'delivered' && (
                        <button
                          onClick={() => {
                            const next = order.status === 'pending' ? 'preparing' :
                                         order.status === 'preparing' ? 'in_transit' : 'delivered';
                            handleUpdateStatus(order.id, next);
                          }}
                          className="px-2.5 py-1 bg-red-600 hover:bg-red-700 text-white text-[10px] font-black rounded-lg transition cursor-pointer"
                        >
                          Gudbi →
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Details Modal */}
      {selectedOrderModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-neutral-100 pb-3">
              <div>
                <span className="font-mono font-black text-red-600 bg-red-50 px-2 py-0.5 rounded-md text-xs">
                  {selectedOrderModal.order_number}
                </span>
                <span className="ml-2 text-xs font-bold text-neutral-500">
                  {selectedOrderModal.merchant_name}
                </span>
              </div>
              <button
                onClick={() => setSelectedOrderModal(null)}
                className="p-1 hover:bg-neutral-100 rounded-lg text-neutral-400 hover:text-neutral-700 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="bg-neutral-50 p-3 rounded-xl border border-neutral-200 space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Macmiilka:</span>
                  <span className="font-bold text-neutral-900">{selectedOrderModal.customer_name}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Taleefanka:</span>
                  <span className="font-mono font-bold text-blue-600">{selectedOrderModal.customer_phone}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Degmada / Goobta:</span>
                  <span className="font-bold text-neutral-900">{selectedOrderModal.district}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Mootada (Courier):</span>
                  <span className="font-bold text-neutral-900">{selectedOrderModal.courier_name || 'Lama xil-saarin'}</span>
                </div>
              </div>

              <div className="bg-neutral-900 text-white p-3 rounded-xl flex items-center justify-between">
                <span className="text-neutral-400 font-medium">Handover Security PIN:</span>
                <span className="font-mono text-base font-black text-amber-400 tracking-widest">{selectedOrderModal.handover_pin}</span>
              </div>

              <div className="bg-neutral-50 p-3 rounded-xl border border-neutral-200 space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Subtotal:</span>
                  <span className="font-bold">${selectedOrderModal.subtotal.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Delivery Fee:</span>
                  <span className="font-bold">${selectedOrderModal.delivery_fee.toFixed(2)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-neutral-500 font-medium">Service Fee:</span>
                  <span className="font-bold">${selectedOrderModal.service_fee.toFixed(2)}</span>
                </div>
                <div className="border-t border-neutral-200 pt-1.5 flex justify-between text-sm">
                  <span className="font-black text-neutral-900">Wadarta Guud:</span>
                  <span className="font-black text-red-600">${selectedOrderModal.total.toFixed(2)}</span>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setSelectedOrderModal(null)}
                className="px-4 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-bold rounded-xl transition cursor-pointer"
              >
                Xir
              </button>
              {canManage && selectedOrderModal.status !== 'delivered' && (
                <button
                  onClick={() => {
                    const next = selectedOrderModal.status === 'pending' ? 'preparing' :
                                 selectedOrderModal.status === 'preparing' ? 'in_transit' : 'delivered';
                    handleUpdateStatus(selectedOrderModal.id, next);
                    setSelectedOrderModal({ ...selectedOrderModal, status: next });
                  }}
                  className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl transition cursor-pointer"
                >
                  U Gudbi Xaaladda Xigta →
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

