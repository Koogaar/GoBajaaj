import React, { useState, useEffect } from 'react';
import { supabase } from '../supabase';
import { Truck, RefreshCw, Star, ShieldCheck, Phone, CheckCircle2, DollarSign } from 'lucide-react';

interface CourierItem {
  id: string;
  name: string;
  phone: string;
  plate_number: string;
  vehicle_type: string;
  is_online: boolean;
  today_earnings: number;
  completed_trips: number;
  rating: number;
  verification_status: string;
}

interface CouriersManagementProps {
  canManage: boolean;
  actorName: string;
}

export const CouriersManagement: React.FC<CouriersManagementProps> = ({ canManage, actorName }) => {
  const [couriers, setCouriers] = useState<CourierItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    fetchCouriers();
  }, []);

  const fetchCouriers = async () => {
    setIsLoading(true);
    try {
      const { data, error } = await supabase
        .from('courier_profiles')
        .select('*');

      if (error || !data || data.length === 0) {
        setCouriers([
          {
            id: 'c1',
            name: 'Maxamed Axmed Xasan',
            phone: '0618092060',
            plate_number: 'MOG-4920',
            vehicle_type: 'motorcycle',
            is_online: true,
            today_earnings: 49.00,
            completed_trips: 14,
            rating: 4.9,
            verification_status: 'verified'
          },
          {
            id: 'c2',
            name: 'Cali Xasan Cilmi',
            phone: '0615551234',
            plate_number: 'MOG-8119',
            vehicle_type: 'motorcycle',
            is_online: true,
            today_earnings: 32.50,
            completed_trips: 9,
            rating: 4.8,
            verification_status: 'verified'
          },
          {
            id: 'c3',
            name: 'Faarax Nuur Warsame',
            phone: '0615555678',
            plate_number: 'MOG-2045',
            vehicle_type: 'bajaj',
            is_online: false,
            today_earnings: 18.00,
            completed_trips: 5,
            rating: 4.7,
            verification_status: 'verified'
          }
        ]);
      } else {
        setCouriers(data as CourierItem[]);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white p-6 rounded-2xl border border-neutral-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-black text-neutral-900 flex items-center gap-2">
            <Truck className="w-5 h-5 text-blue-600" />
            Gaadiidka Delivery-ga (Mootooyinka & Couriers)
          </h2>
          <p className="text-xs text-neutral-500 mt-1">
            Liiska darawallada mootada ee qaada dalabyada iyo baakadaha: dakhliga maalinta, safarrada, iyo xaaladda shaqada.
          </p>
        </div>

        <button
          onClick={fetchCouriers}
          className="px-3.5 py-2 bg-neutral-100 hover:bg-neutral-200 text-neutral-700 text-xs font-bold rounded-xl transition flex items-center gap-1.5 cursor-pointer"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Couriers Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {couriers.map(courier => (
          <div key={courier.id} className="bg-white p-5 rounded-2xl border border-neutral-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-xl bg-blue-50 border border-blue-100 flex items-center justify-center text-blue-600 font-black text-lg">
                  🏍️
                </div>
                <div>
                  <h4 className="font-black text-sm text-neutral-900 flex items-center gap-1.5">
                    {courier.name}
                    <ShieldCheck className="w-4 h-4 text-blue-600" />
                  </h4>
                  <p className="text-xs font-mono text-neutral-400">{courier.plate_number} • {courier.phone}</p>
                </div>
              </div>
              <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${
                courier.is_online ? 'bg-emerald-100 text-emerald-800' : 'bg-neutral-100 text-neutral-500'
              }`}>
                {courier.is_online ? '● Online' : '○ Offline'}
              </span>
            </div>

            <div className="grid grid-cols-2 gap-2 pt-3 border-t border-neutral-100 text-center">
              <div className="bg-neutral-50 p-2.5 rounded-xl">
                <div className="text-[10px] font-bold text-neutral-400 uppercase">Dakhliga Maanta</div>
                <div className="text-sm font-black text-emerald-600 mt-0.5">${courier.today_earnings.toFixed(2)}</div>
              </div>
              <div className="bg-neutral-50 p-2.5 rounded-xl">
                <div className="text-[10px] font-bold text-neutral-400 uppercase">Safarrada</div>
                <div className="text-sm font-black text-neutral-900 mt-0.5">{courier.completed_trips} dalab</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

