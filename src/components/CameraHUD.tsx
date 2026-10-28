import React from 'react';
import { CameraSettings } from '../types/camera';
import { Zap, ZapOff, Settings as SettingsIcon, Moon } from 'lucide-react';

interface CameraHUDProps {
  settings: CameraSettings;
  isRecording: boolean;
  recordingTime: number; // in seconds
  isVertical: boolean;
  onToggleRecord: () => void;
  onToggleTorch: () => void;
  onToggleNightBoost?: () => void;
  onSelectLens: (sensor: CameraSettings['activeSensor']) => void;
  onOpenSettings: () => void;
}

export const CameraHUD: React.FC<CameraHUDProps> = ({
  settings,
  isRecording,
  recordingTime,
  isVertical,
  onToggleRecord,
  onToggleTorch,
  onToggleNightBoost,
  onSelectLens,
  onOpenSettings
}) => {
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  // Dimensions on the labels inverted in vertical orientation per user requirement:
  // "The dimensions on the labels should be inverted to utilize most of the screen, without stretching the image."
  const rawW = settings.resolution.width;
  const rawH = settings.resolution.height;
  const displayedWidth = isVertical ? rawH : rawW;
  const displayedHeight = isVertical ? rawW : rawH;
  const aspectLabel = isVertical ? '9:16' : '16:9';

  return (
    <div className="absolute inset-0 pointer-events-none flex flex-col justify-between p-4 select-none z-20 font-mono text-xs text-white">
      
      {/* Top Status Bar: Clean, minimal HUD with Low Light indication */}
      <div className="flex items-center justify-between pointer-events-auto">
        <div className="flex items-center gap-2.5 bg-black/50 backdrop-blur-sm px-3 py-1.5 rounded-full border border-white/10 text-neutral-200">
          <span className="font-bold text-amber-400">
            {displayedWidth}×{displayedHeight}
          </span>
          <span className="text-white/30">|</span>
          <span className="text-white/80">{aspectLabel}</span>
          <span className="text-white/30">|</span>
          <span className="text-emerald-400">{settings.fps}P</span>
          <span className="text-white/30">|</span>
          <span className="text-red-400">{settings.bitrateMbps}M</span>
          <span className="text-white/30">|</span>
          <span className="text-blue-300">
            {settings.use10BitHlg ? 'HLG 10b' : 'H.264'}
          </span>
          {settings.nightBoostEnabled && (
            <>
              <span className="text-white/30">|</span>
              <span className="text-amber-300 flex items-center gap-1 font-bold animate-pulse">
                <Moon className="w-3 h-3 fill-amber-300" />
                <span>NIGHT BOOST</span>
              </span>
            </>
          )}
        </div>

        {/* Center Timer (when recording) */}
        {isRecording && (
          <div className="flex items-center gap-2 bg-red-600/90 text-white px-3 py-1 rounded-full font-bold shadow-lg animate-pulse">
            <div className="w-2 h-2 rounded-full bg-white" />
            <span>REC {formatTime(recordingTime)}</span>
          </div>
        )}

        {/* Right Tools: Night Boost, Torch & Settings */}
        <div className="flex items-center gap-2">
          {onToggleNightBoost && (
            <button
              onClick={onToggleNightBoost}
              className={`p-2 rounded-full backdrop-blur-sm border transition-all ${
                settings.nightBoostEnabled
                  ? 'bg-amber-400 text-black border-amber-300 shadow-lg shadow-amber-400/30'
                  : 'bg-black/50 text-white border-white/10 hover:bg-black/70'
              }`}
              title="Toggle Low-Light Night Boost (Sony IMX989 1-Inch)"
            >
              <Moon className="w-4 h-4" />
            </button>
          )}

          <button
            onClick={onToggleTorch}
            className={`p-2 rounded-full backdrop-blur-sm border transition-all ${
              settings.torchEnabled
                ? 'bg-amber-400 text-black border-amber-300 shadow-lg'
                : 'bg-black/50 text-white border-white/10 hover:bg-black/70'
            }`}
            title="Toggle Torch"
          >
            {settings.torchEnabled ? <Zap className="w-4 h-4 fill-current" /> : <ZapOff className="w-4 h-4" />}
          </button>

          <button
            onClick={onOpenSettings}
            className="p-2 rounded-full bg-black/50 backdrop-blur-sm text-white border border-white/10 hover:bg-black/70 transition-all"
            title="Settings"
          >
            <SettingsIcon className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Middle Viewport is completely unobstructed */}
      <div className="flex-1 pointer-events-none" />

      {/* Bottom Controls: Lens selection & Cinema Record Trigger */}
      <div className="flex flex-col items-center gap-3 pointer-events-auto pb-2">
        
        {/* Lens Switcher */}
        <div className="flex items-center gap-1 bg-black/50 backdrop-blur-sm p-1 rounded-full border border-white/10">
          <button
            onClick={() => onSelectLens('ultrawide_imx586')}
            className={`px-3 py-1 rounded-full text-[11px] font-bold transition-all ${
              settings.activeSensor === 'ultrawide_imx586'
                ? 'bg-white text-black shadow'
                : 'text-neutral-300 hover:text-white'
            }`}
          >
            0.5x
          </button>

          <button
            onClick={() => onSelectLens('main_imx989')}
            className={`px-3 py-1 rounded-full text-[11px] font-bold transition-all ${
              settings.activeSensor === 'main_imx989'
                ? 'bg-amber-400 text-black shadow'
                : 'text-neutral-300 hover:text-white'
            }`}
          >
            1.0x
          </button>

          <button
            onClick={() => onSelectLens('telephoto_imx586')}
            className={`px-3 py-1 rounded-full text-[11px] font-bold transition-all ${
              settings.activeSensor === 'telephoto_imx586'
                ? 'bg-amber-400 text-black shadow'
                : 'text-neutral-300 hover:text-white'
            }`}
          >
            5.0x
          </button>

          <button
            onClick={() => onSelectLens('dual_concurrent')}
            className={`px-3 py-1 rounded-full text-[11px] font-bold transition-all ${
              settings.activeSensor === 'dual_concurrent'
                ? 'bg-red-500 text-white shadow'
                : 'text-neutral-300 hover:text-white'
            }`}
          >
            DUAL
          </button>
        </div>

        {/* Shutter Button */}
        <div className="flex items-center justify-center">
          <button
            onClick={onToggleRecord}
            className={`relative flex items-center justify-center w-18 h-18 rounded-full border-4 transition-all active:scale-95 ${
              isRecording
                ? 'border-red-500 bg-red-950/70 shadow-red-500/40'
                : 'border-white bg-black/30 hover:border-red-500'
            }`}
          >
            <div
              className={`transition-all duration-200 ${
                isRecording
                  ? 'w-7 h-7 rounded-sm bg-red-500'
                  : 'w-12 h-12 rounded-full bg-red-600'
              }`}
            />
          </button>
        </div>

      </div>

    </div>
  );
};
