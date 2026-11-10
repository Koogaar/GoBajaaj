import React from 'react';

interface PhoneInputProps {
  value: string;
  onChange: (val: string) => void;
  placeholder?: string;
  className?: string;
  required?: boolean;
}

export const PhoneInput: React.FC<PhoneInputProps> = ({
  value,
  onChange,
  placeholder = '61 234 5678',
  className = '',
  required = true,
}) => {
  // Strip country code if entered inside local part
  const cleanNumber = value.replace(/^\+252\s*/, '');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let input = e.target.value.replace(/[^0-9]/g, '');
    if (input.length > 9) input = input.slice(0, 9);
    onChange(`+252 ${input}`);
  };

  return (
    <div className={`relative flex items-center rounded-xl border border-slate-300 bg-white shadow-sm focus-within:border-red-600 focus-within:ring-2 focus-within:ring-red-100 transition-all ${className}`}>
      {/* Country prefix button */}
      <div className="flex items-center gap-1.5 px-3 py-2.5 bg-slate-50 border-r border-slate-200 rounded-l-xl text-slate-700 font-medium text-sm select-none shrink-0">
        <span className="text-base" role="img" aria-label="Somalia flag">🇸🇴</span>
        <span>+252</span>
      </div>
      <input
        type="tel"
        value={cleanNumber}
        onChange={handleChange}
        placeholder={placeholder}
        required={required}
        className="w-full px-3 py-2.5 text-sm text-slate-900 placeholder:text-slate-400 bg-transparent outline-none font-mono"
      />
    </div>
  );
};
