import React from 'react';

interface DeviceFrameProps {
  children: React.ReactNode;
  appName?: string;
  roleBadge?: string;
}

export const DeviceFrame: React.FC<DeviceFrameProps> = ({
  children,
}) => {
  return (
    <div className="w-full min-h-screen bg-slate-100 flex flex-col justify-start items-center">
      <div className="w-full max-w-md bg-white min-h-screen shadow-xl flex flex-col border-x border-slate-200">
        {children}
      </div>
    </div>
  );
};
