import React from 'react';
import { AlertTriangle, X } from 'lucide-react';

interface ConfirmationModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDanger?: boolean;
  isLoading?: boolean;
  children?: React.ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = 'Xaqiiji',
  cancelLabel = 'Jooji',
  isDanger = true,
  isLoading = false,
  children,
  onConfirm,
  onCancel
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full border border-neutral-200 overflow-hidden animate-in fade-in zoom-in duration-150">
        <div className="p-6 border-b border-neutral-200 flex items-center justify-between bg-neutral-50">
          <div className="flex items-center space-x-2">
            <AlertTriangle className={`w-5 h-5 ${isDanger ? 'text-[#E50914]' : 'text-amber-500'}`} />
            <h3 className="font-black text-lg text-black">{title}</h3>
          </div>
          <button 
            onClick={onCancel}
            disabled={isLoading}
            className="text-neutral-400 hover:text-black p-1 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          <p className="text-sm text-neutral-700 leading-relaxed whitespace-pre-line">{message}</p>
          {children}
        </div>

        <div className="p-4 bg-neutral-50 border-t border-neutral-200 flex justify-end space-x-3">
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="px-4 py-2 text-sm font-bold text-neutral-600 hover:text-black transition"
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            disabled={isLoading}
            className={`px-5 py-2 text-white text-sm font-bold rounded-xl shadow transition flex items-center space-x-2 ${
              isDanger ? 'bg-[#E50914] hover:bg-[#B91C1C]' : 'bg-emerald-600 hover:bg-emerald-700'
            }`}
          >
            {isLoading && <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>}
            <span>{confirmLabel}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
