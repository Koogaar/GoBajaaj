import React, { useState, useEffect } from 'react';
import { History, RefreshCw, AlertCircle, FileText } from 'lucide-react';
import { AuditLog } from '../types';
import { supabase } from '../supabase';

export const AuditLogsView: React.FC = () => {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const fetchAuditLogs = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const { data, error } = await supabase
        .from('audit_logs')
        .select('*')
        .order('created_at', { ascending: false });

      if (error) {
        console.error('Fetch audit_logs error:', error.message);
        setErrorMsg('Cilad ayaa ka dhacday soo qaadista xogta audit logs.');
        setLogs([]);
      } else if (data) {
        setLogs(data as AuditLog[]);
      }
    } catch (err: any) {
      setErrorMsg('Cilad ayaa dhacday.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAuditLogs();
  }, []);

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm p-4 sm:p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <History className="w-6 h-6 text-[#E50914] shrink-0" />
            <h2 className="text-lg sm:text-xl font-black text-black">Audit Logs (Xogta Dhaqdhaqaaqa Maamulka)</h2>
          </div>
          <p className="text-xs text-neutral-600 mt-1">
            Diiwaanka beddellada, ansixinta lacagaha, iyo maamulka xilalka ee rasmiga ah.
          </p>
        </div>

        <button 
          onClick={fetchAuditLogs}
          disabled={isLoading}
          className="p-2.5 border border-neutral-200 rounded-xl hover:bg-neutral-50 transition text-neutral-700 cursor-pointer shrink-0"
          title="Cusbooneysii"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin text-[#E50914]' : ''}`} />
        </button>
      </div>

      {errorMsg && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-xl text-red-800 text-xs font-semibold flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 text-red-600 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Logs Table */}
      <div className="bg-white rounded-2xl border border-neutral-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-neutral-500">
            <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-3 text-[#E50914]" />
            <p className="text-sm font-semibold">Soo qaadaya audit logs...</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="p-12 text-center">
            <FileText className="w-12 h-12 text-neutral-300 mx-auto mb-3" />
            <h3 className="text-base font-bold text-black">Xog lama helin</h3>
            <p className="text-xs text-neutral-500 mt-1">Ma jiro log audit ah hadda.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm min-w-[700px]">
              <thead className="bg-neutral-100 border-b border-neutral-200 text-black">
                <tr>
                  <th className="p-4 font-bold">Maamulaha (Actor)</th>
                  <th className="p-4 font-bold">Ficilka (Action)</th>
                  <th className="p-4 font-bold">Bartilmaameedka (Target)</th>
                  <th className="p-4 font-bold">Faahfaahin (Metadata)</th>
                  <th className="p-4 font-bold">Waqtiga</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-neutral-50 transition">
                    <td className="p-4 font-bold text-black">{log.actor_name}</td>
                    <td className="p-4">
                      <span className="px-2.5 py-1 rounded-lg text-xs font-mono font-bold bg-neutral-900 text-white">
                        {log.action}
                      </span>
                    </td>
                    <td className="p-4 text-xs font-mono text-neutral-700">
                      {log.target_type} ({log.target_id || 'N/A'})
                    </td>
                    <td className="p-4 text-xs font-mono text-neutral-500 max-w-xs truncate">
                      {log.metadata ? JSON.stringify(log.metadata) : '-'}
                    </td>
                    <td className="p-4 text-xs font-mono text-neutral-500">{log.created_at?.replace('T', ' ')?.slice(0, 19)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
