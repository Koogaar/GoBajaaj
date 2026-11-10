import React from 'react';

interface QrCodeSvgProps {
  value: string;
  size?: number;
  className?: string;
  addressCode?: string;
}

export const QrCodeSvg: React.FC<QrCodeSvgProps> = ({
  value,
  size = 180,
  className = '',
  addressCode,
}) => {
  // Deterministic matrix generator based on input string
  const gridSize = 25;
  const hash = Array.from(value).reduce((acc, char, i) => acc + char.charCodeAt(0) * (i + 1), 0);

  const isDark = (row: number, col: number) => {
    // 3 Corner Finder Patterns (7x7)
    // Top-Left
    if (row < 7 && col < 7) {
      if (row === 0 || row === 6 || col === 0 || col === 6) return true;
      if (row >= 2 && row <= 4 && col >= 2 && col <= 4) return true;
      return false;
    }
    // Top-Right
    if (row < 7 && col >= gridSize - 7) {
      const c = col - (gridSize - 7);
      if (row === 0 || row === 6 || c === 0 || c === 6) return true;
      if (row >= 2 && row <= 4 && c >= 2 && c <= 4) return true;
      return false;
    }
    // Bottom-Left
    if (row >= gridSize - 7 && col < 7) {
      const r = row - (gridSize - 7);
      if (r === 0 || r === 6 || col === 0 || col === 6) return true;
      if (r >= 2 && r <= 4 && col >= 2 && col <= 4) return true;
      return false;
    }

    // Center logo cutout
    if (row >= 10 && row <= 14 && col >= 10 && col <= 14) {
      return false;
    }

    // Alignment pattern
    if (row >= 16 && row <= 20 && col >= 16 && col <= 20) {
      if (row === 16 || row === 20 || col === 16 || col === 20) return true;
      if (row === 18 && col === 18) return true;
      return false;
    }

    // Timing lines
    if (row === 6 || col === 6) return (row + col) % 2 === 0;

    // Pseudo-random data bits deterministic from hash and coords
    const val = (row * 37 + col * 19 + hash) % 100;
    return val > 44;
  };

  const moduleSize = size / gridSize;

  return (
    <div className={`flex flex-col items-center justify-center p-3 bg-white rounded-2xl shadow-sm border border-slate-100 ${className}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="rounded-lg">
        <rect width={size} height={size} fill="#FFFFFF" />
        {Array.from({ length: gridSize }).map((_, r) =>
          Array.from({ length: gridSize }).map((_, c) => {
            if (isDark(r, c)) {
              return (
                <rect
                  key={`${r}-${c}`}
                  x={c * moduleSize}
                  y={r * moduleSize}
                  width={moduleSize}
                  height={moduleSize}
                  fill="#111827"
                  rx={moduleSize * 0.2}
                />
              );
            }
            return null;
          })
        )}
        {/* Center Shiri icon badge */}
        <circle cx={size / 2} cy={size / 2} r={moduleSize * 2.2} fill="#DC2626" />
        <text
          x={size / 2}
          y={size / 2 + 4}
          textAnchor="middle"
          fill="#FFFFFF"
          fontSize={moduleSize * 2}
          fontWeight="bold"
          fontFamily="system-ui"
        >
          S
        </text>
      </svg>
      {addressCode && (
        <span className="mt-2 text-xs font-mono font-bold tracking-wider text-slate-800 bg-slate-50 px-2 py-0.5 rounded border border-slate-200">
          {addressCode}
        </span>
      )}
    </div>
  );
};
