import React from 'react';

interface ShiriLogoProps {
  variant?: 'full' | 'icon' | 'white';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  showSlogan?: boolean;
  sloganLanguage?: 'so' | 'en';
}

export const ShiriLogo: React.FC<ShiriLogoProps> = ({
  variant = 'full',
  size = 'md',
  showSlogan = true,
  sloganLanguage = 'so',
}) => {
  const isWhite = variant === 'white';
  const primaryColor = isWhite ? '#FFFFFF' : '#DC2626';
  const textColor = isWhite ? '#FFFFFF' : '#111827';
  const sloganColor = isWhite ? '#FCA5A5' : '#6B7280';

  const sizeClasses = {
    sm: { icon: 'w-7 h-7', text: 'text-base', slogan: 'text-[9px]' },
    md: { icon: 'w-9 h-9', text: 'text-xl', slogan: 'text-[11px]' },
    lg: { icon: 'w-12 h-12', text: 'text-2xl', slogan: 'text-xs' },
    xl: { icon: 'w-16 h-16', text: 'text-3xl', slogan: 'text-sm' },
  }[size];

  const iconSvg = (
    <svg
      viewBox="0 0 100 100"
      className={`${sizeClasses.icon} shrink-0`}
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Background circle badge with speed curves */}
      <circle cx="50" cy="50" r="48" fill={isWhite ? 'rgba(255,255,255,0.15)' : '#DC2626'} />
      {/* Dynamic S / Delivery wings motif matching design references */}
      <path
        d="M26 36C26 36 34 26 52 26C70 26 76 34 74 42C72 50 60 52 50 54C38 56 30 60 32 68C34 76 44 80 58 79C70 78 74 71 74 71"
        stroke={isWhite ? '#DC2626' : '#FFFFFF'}
        strokeWidth="10"
        strokeLinecap="round"
      />
      {/* Upper speed line */}
      <path
        d="M20 44L34 44"
        stroke={isWhite ? '#DC2626' : '#FFFFFF'}
        strokeWidth="6"
        strokeLinecap="round"
      />
      {/* Lower speed line */}
      <path
        d="M18 58L28 58"
        stroke={isWhite ? '#DC2626' : '#FFFFFF'}
        strokeWidth="6"
        strokeLinecap="round"
      />
    </svg>
  );

  if (variant === 'icon') {
    return iconSvg;
  }

  return (
    <div className="flex items-center gap-2.5 select-none">
      {iconSvg}
      <div className="flex flex-col">
        <span
          className={`font-black tracking-tight leading-none ${sizeClasses.text}`}
          style={{ color: textColor }}
        >
          Shiri <span style={{ color: primaryColor }}>Delivery</span>
        </span>
        {showSlogan && (
          <span
            className={`font-medium tracking-tight mt-0.5 ${sizeClasses.slogan}`}
            style={{ color: sloganColor }}
          >
            {sloganLanguage === 'so' ? 'Ku dhowaa, Ku gaara' : 'Your City, Our Priority'}
          </span>
        )}
      </div>
    </div>
  );
};
