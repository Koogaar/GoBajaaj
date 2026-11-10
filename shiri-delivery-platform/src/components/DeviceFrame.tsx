import React, { useState } from 'react';
import { Smartphone, Maximize2, Minimize2, Battery, Wifi, Signal } from 'lucide-react';

interface DeviceFrameProps {
  children: React.ReactNode;
  appName: string;
  roleBadge: string;
}

export const DeviceFrame: React.FC<DeviceFrameProps> = ({
  children,
  appName,
  roleBadge,
}) => {
  const [deviceModel, setDeviceModel] = useState<'iphone' | 'android'>('iphone');
  const [isFullscreen, setIsFullscreen] = useState<boolean>(false);

  const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  if (isFullscreen) {
    return (
      <div className="w-full min-h-screen bg-slate-50 flex flex-col">
        {/* Minimal exit fullscreen toolbar */}
        <div className="sticky top-0 z-50 flex items-center justify-between px-4 py-2 bg-slate-900 text-white text-xs">
          <div className="flex items-center gap-2">
            <span className="font-bold">{appName}</span>
            <span className="bg-red-600 px-2 py-0.5 rounded text-[10px] font-semibold">{roleBadge}</span>
          </div>
          <button
            onClick={() => setIsFullscreen(false)}
            className="flex items-center gap-1 bg-slate-800 hover:bg-slate-700 px-2.5 py-1 rounded text-xs transition-colors"
          >
            <Minimize2 className="w-3.5 h-3.5" />
            <span>Exit Fullscreen</span>
          </button>
        </div>
        <div className="flex-1 w-full max-w-md mx-auto bg-white min-h-screen shadow-lg">
          {children}
        </div>
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col items-center justify-center py-2 sm:py-6 px-0 sm:px-4">
      {/* Device Toolbar Controls */}
      <div className="flex items-center justify-between w-full max-w-[420px] mb-2 px-3 sm:px-1 text-xs text-slate-400">
        <div className="flex items-center gap-2 min-w-0">
          <Smartphone className="w-4 h-4 text-red-500 shrink-0" />
          <span className="font-semibold text-slate-200 truncate">{appName}</span>
          <span className="bg-red-950 text-red-300 border border-red-800 font-bold px-2 py-0.5 rounded-full text-[10px] shrink-0">
            {roleBadge}
          </span>
        </div>

        <div className="flex items-center gap-1.5 shrink-0">
          {/* OS Switcher */}
          <div className="flex items-center bg-slate-800 p-0.5 rounded-lg text-[11px]">
            <button
              onClick={() => setDeviceModel('iphone')}
              className={`px-2 py-0.5 rounded-md font-medium transition-colors ${
                deviceModel === 'iphone' ? 'bg-slate-700 text-white shadow-xs' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              iOS
            </button>
            <button
              onClick={() => setDeviceModel('android')}
              className={`px-2 py-0.5 rounded-md font-medium transition-colors ${
                deviceModel === 'android' ? 'bg-slate-700 text-white shadow-xs' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Android
            </button>
          </div>

          <button
            onClick={() => setIsFullscreen(true)}
            className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
            title="Full mobile screen"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>

      {/* Realistic Smartphone Chassis - Responsive from 320px up */}
      <div
        className={`relative w-full max-w-[420px] h-[calc(100vh-140px)] min-h-[580px] max-h-[860px] sm:h-[830px] bg-slate-950 sm:rounded-[48px] p-0 sm:p-3 shadow-2xl sm:border-4 ${
          deviceModel === 'iphone' ? 'sm:border-slate-800 sm:ring-1 sm:ring-slate-700/50' : 'sm:border-slate-800 sm:rounded-[36px]'
        }`}
      >
        {/* Dynamic Island / Camera Notch (visible on larger screens) */}
        {deviceModel === 'iphone' ? (
          <div className="hidden sm:flex absolute top-5 left-1/2 -translate-x-1/2 w-28 h-5 bg-black rounded-full z-40 items-center justify-end px-3">
            <div className="w-2.5 h-2.5 rounded-full bg-slate-900 border border-slate-800" />
          </div>
        ) : (
          <div className="hidden sm:block absolute top-5 left-1/2 -translate-x-1/2 w-3.5 h-3.5 bg-black rounded-full z-40 border border-slate-800" />
        )}

        {/* Hardware side buttons - desktop only */}
        <div className="hidden sm:block absolute -left-[7px] top-28 w-[3px] h-9 bg-slate-700 rounded-l-md" />
        <div className="hidden sm:block absolute -left-[7px] top-40 w-[3px] h-9 bg-slate-700 rounded-l-md" />
        <div className="hidden sm:block absolute -right-[7px] top-32 w-[3px] h-14 bg-slate-700 rounded-r-md" />

        {/* Screen Display Container */}
        <div className="relative w-full h-full bg-white sm:rounded-[38px] overflow-hidden flex flex-col">
          {/* Status Bar */}
          <div className="h-9 sm:h-10 pt-1 sm:pt-2 px-4 sm:px-6 flex items-center justify-between text-xs font-semibold text-slate-900 z-30 select-none bg-white/95 backdrop-blur-xs border-b border-slate-100">
            <span className="font-mono text-[11px] sm:text-xs">{currentTime}</span>
            <div className="flex items-center gap-1.5 text-slate-700">
              <Signal className="w-3.5 h-3.5" />
              <Wifi className="w-3.5 h-3.5" />
              <Battery className="w-4 h-4" />
            </div>
          </div>

          {/* App Content with Smooth Touch Scrolling */}
          <div className="flex-1 overflow-y-auto overflow-x-hidden flex flex-col relative select-text">
            {children}
          </div>

          {/* Home Indicator Bar */}
          <div className="h-3 sm:h-4 w-full flex items-center justify-center bg-white select-none shrink-0 border-t border-slate-100/50">
            <div className="w-28 sm:w-32 h-1 bg-slate-300 rounded-full" />
          </div>
        </div>
      </div>
    </div>
  );
};
