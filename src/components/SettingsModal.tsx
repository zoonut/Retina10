import React from 'react';
import { CameraSettings, LeicaColorProfile } from '../types/camera';
import { 
  X, 
  Check, 
  Flame, 
  Sliders, 
  Cpu, 
  Sun, 
  Video, 
  Eye, 
  Zap, 
  ShieldCheck, 
  RotateCw,
  Moon 
} from 'lucide-react';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
  settings: CameraSettings;
  onUpdateSettings: (newSettings: Partial<CameraSettings>) => void;
  availableResolutions: Array<{ width: number; height: number; label: string }>;
  codecsSummary: Array<{ label: string; supported: boolean; is10Bit: boolean; colorGamut: string }>;
  onOpenAndroidCode?: () => void;
  onOpenGallery?: () => void;
  recordedClipsCount?: number;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose,
  settings,
  onUpdateSettings,
  availableResolutions,
  codecsSummary,
  onOpenAndroidCode,
  onOpenGallery,
  recordedClipsCount = 0
}) => {
  if (!isOpen) return null;

  // The exact required bitrates: 40, 50, 60, 70, 80, 90, 100 Mbps
  const bitrateOptions = [40, 50, 60, 70, 80, 90, 100];

  const leicaProfiles: Array<{ id: LeicaColorProfile; name: string; desc: string }> = [
    {
      id: 'leica_hlg_bt2020',
      name: 'Leica HLG 10-Bit BT.2020 Log (DaVinci Resolve)',
      desc: 'ITU-R BT.2100 Hybrid Log-Gamma with wide BT.2020 gamut. Flat curve preserves highlight latitude & shadow details.'
    },
    {
      id: 'leica_authentic',
      name: 'Leica Authentic (Leica 经典)',
      desc: 'Signature micro-contrast, organic shadow roll-off, authentic skin tones, warm reds/ambers.'
    },
    {
      id: 'leica_vibrant',
      name: 'Leica Vibrant (Leica 生动)',
      desc: 'Elevated dynamic range, punchy saturated midtones, ideal for bright scenes.'
    },
    {
      id: 'leica_monochrom',
      name: 'Leica Monochrom (Leica 黑白)',
      desc: 'Pure monochrome sensor rendering with smooth tonal gradation inspired by Leica M Monochrom.'
    },
    {
      id: 'leica_monochrom_hc',
      name: 'Leica Monochrom High Contrast (黑白高反差)',
      desc: 'Deep blacks and crisp highlights for expressive street photography.'
    }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/80 backdrop-blur-md">
      <div className="bg-neutral-900 border border-neutral-800 text-neutral-100 rounded-2xl w-full max-w-2xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden font-sans">
        
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-neutral-800 bg-neutral-950/80">
          <div className="flex items-center gap-2.5">
            <Sliders className="w-5 h-5 text-amber-400" />
            <div>
              <h2 className="text-base font-bold tracking-wide">Retina 7 Pro Cinema Settings</h2>
              <p className="text-xs text-neutral-400">Calibrated for Xiaomi 12S Ultra · Sony IMX989 & IMX586</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-300 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6 text-sm divide-y divide-neutral-800/60">
          
          {/* SECTION 1: BITRATE OPTIONS (40, 50, 60, 70, 80, 90, 100 Mbps) */}
          <div className="space-y-3 pt-2">
            <div className="flex items-center justify-between">
              <div>
                <label className="font-semibold text-neutral-200">Video Encoding Bitrate (Mbps)</label>
                <p className="text-xs text-neutral-400">Higher bitrates prevent macroblocking in dark rooms under torchlight</p>
              </div>
              <span className="text-xs font-mono font-bold text-amber-400 bg-amber-400/10 px-2.5 py-1 rounded border border-amber-400/30">
                {settings.bitrateMbps} Mbps
              </span>
            </div>
            <div className="grid grid-cols-4 sm:grid-cols-7 gap-2">
              {bitrateOptions.map((mbps) => (
                <button
                  key={mbps}
                  onClick={() => onUpdateSettings({ bitrateMbps: mbps })}
                  className={`py-2 px-1 text-center font-mono rounded-lg border text-xs font-bold transition-all ${
                    settings.bitrateMbps === mbps
                      ? 'bg-amber-400 text-neutral-950 border-amber-300 shadow-lg shadow-amber-400/20'
                      : 'bg-neutral-800/70 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
                  }`}
                >
                  {mbps}M
                </button>
              ))}
            </div>
          </div>

          {/* SECTION 2: RESOLUTION & FRAME RATE */}
          <div className="space-y-3 pt-4">
            <label className="font-semibold text-neutral-200">Sensor Output Resolution</label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {availableResolutions.map((res) => {
                const isSelected = settings.resolution.width === res.width && settings.resolution.height === res.height;
                return (
                  <button
                    key={`${res.width}x${res.height}`}
                    onClick={() => onUpdateSettings({ resolution: res })}
                    className={`flex items-center justify-between p-3 rounded-xl border text-left transition-all ${
                      isSelected
                        ? 'bg-neutral-800 border-amber-400/80 shadow-md'
                        : 'bg-neutral-950/60 border-neutral-800 hover:bg-neutral-800/60'
                    }`}
                  >
                    <div>
                      <div className="font-bold text-xs text-white">{res.label}</div>
                      <div className="text-[11px] font-mono text-neutral-400">{res.width} × {res.height}</div>
                    </div>
                    {isSelected && <Check className="w-4 h-4 text-amber-400" />}
                  </button>
                );
              })}
            </div>

            {/* Frame Rate */}
            <div className="flex items-center justify-between pt-2">
              <span className="text-xs text-neutral-300">Target Frame Rate</span>
              <div className="flex gap-2">
                {[24, 30, 60].map((fpsVal) => (
                  <button
                    key={fpsVal}
                    onClick={() => onUpdateSettings({ fps: fpsVal })}
                    className={`px-3 py-1 rounded-lg text-xs font-mono font-bold border transition-all ${
                      settings.fps === fpsVal
                        ? 'bg-white text-black border-white'
                        : 'bg-neutral-800 text-neutral-400 border-neutral-700'
                    }`}
                  >
                    {fpsVal} FPS
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* SECTION 3: LEICA COLOR SCIENCE & DAVINCI RESOLVE HLG */}
          <div className="space-y-3 pt-4">
            <div>
              <label className="font-semibold text-neutral-200">Leica Color Science & DaVinci Grading Mode</label>
              <p className="text-xs text-neutral-400">Co-engineered color profiles and high-dynamic-range curve</p>
            </div>
            <div className="space-y-2">
              {leicaProfiles.map((p) => {
                const isSelected = settings.leicaProfile === p.id;
                return (
                  <button
                    key={p.id}
                    onClick={() => onUpdateSettings({ leicaProfile: p.id })}
                    className={`w-full p-3 rounded-xl border text-left transition-all flex items-start gap-3 ${
                      isSelected
                        ? 'bg-neutral-800 border-red-500 shadow-md'
                        : 'bg-neutral-950/60 border-neutral-800 hover:bg-neutral-800/40'
                    }`}
                  >
                    <div className="mt-0.5">
                      <div className={`w-3.5 h-3.5 rounded-full border flex items-center justify-center ${isSelected ? 'border-red-500 bg-red-500' : 'border-neutral-600'}`}>
                        {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                      </div>
                    </div>
                    <div className="flex-1">
                      <div className="font-bold text-xs text-neutral-100 flex items-center gap-2">
                        {p.name}
                        {p.id === 'leica_hlg_bt2020' && (
                          <span className="bg-red-500/20 text-red-400 text-[10px] px-1.5 py-0.5 rounded font-mono">
                            RECOMMENDED
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-neutral-400 mt-0.5">{p.desc}</p>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* SECTION 4: POORLY LIT ROOM & LOW-LIGHT BOOST (SONY IMX989 1" SENSOR) */}
          <div className="space-y-4 pt-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Moon className="w-4 h-4 text-amber-400" />
                <label className="font-semibold text-neutral-200">Poorly Lit Room & Low-Light Performance</label>
              </div>
              <span className={`text-[11px] font-mono font-bold px-2 py-0.5 rounded border ${
                settings.nightBoostEnabled 
                  ? 'bg-amber-400/20 text-amber-300 border-amber-400/40' 
                  : 'bg-neutral-800 text-neutral-400 border-neutral-700'
              }`}>
                {settings.nightBoostEnabled ? 'IMX989 3.2μm BINNING ACTIVE' : 'STANDARD GAIN'}
              </span>
            </div>
            <p className="text-xs text-neutral-400">
              Calibrated for dim interiors, ambient lamps, and poorly lit rooms using the Sony IMX989 1-inch physical sensor area.
            </p>

            {/* Master Night Boost Switch */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/70 border border-neutral-800">
              <div>
                <div className="font-bold text-xs text-neutral-100 flex items-center gap-1.5">
                  <span>Night Low-Light Boost</span>
                  <span className="text-[10px] text-amber-400 font-mono bg-amber-400/10 px-1.5 py-0.2 rounded border border-amber-400/20">4-in-1 Quad-Bayer</span>
                </div>
                <div className="text-[11px] text-neutral-400 mt-0.5">
                  Combines 4 adjacent pixels into 3.2μm super-pixels to quadrupled photon sensitivity
                </div>
              </div>
              <input
                type="checkbox"
                checked={settings.nightBoostEnabled}
                onChange={(e) => onUpdateSettings({ nightBoostEnabled: e.target.checked })}
                className="w-4 h-4 accent-amber-400 rounded cursor-pointer"
              />
            </div>

            {/* Low-Light Shutter Priority (24 FPS) */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/70 border border-neutral-800">
              <div>
                <div className="font-bold text-xs text-neutral-100 flex items-center gap-1.5">
                  <span>24 FPS Slow Shutter Priority (1/24s Exposure)</span>
                  <span className="text-[10px] text-emerald-400 font-mono bg-emerald-400/10 px-1.5 py-0.2 rounded border border-emerald-400/20">+60% Light</span>
                </div>
                <div className="text-[11px] text-neutral-400 mt-0.5">
                  Locks exposure window to a full 41.6ms integration per frame for noise-free dark scenes
                </div>
              </div>
              <input
                type="checkbox"
                checked={settings.lowLightShutterPriority}
                onChange={(e) => onUpdateSettings({ 
                  lowLightShutterPriority: e.target.checked,
                  fps: e.target.checked ? 24 : settings.fps
                })}
                className="w-4 h-4 accent-amber-400 rounded cursor-pointer"
              />
            </div>

            {/* HyperOIS Stabilization */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/70 border border-neutral-800">
              <div>
                <div className="font-bold text-xs text-neutral-100">Xiaomi HyperOIS Optical Stabilization</div>
                <div className="text-[11px] text-neutral-400 mt-0.5">
                  Stabilizes camera sensor during longer exposure times to prevent motion blur
                </div>
              </div>
              <input
                type="checkbox"
                checked={settings.hyperOis}
                onChange={(e) => onUpdateSettings({ hyperOis: e.target.checked })}
                className="w-4 h-4 accent-amber-400 rounded cursor-pointer"
              />
            </div>

            {/* Exposure Compensation (EV Bias) */}
            <div className="space-y-1.5 bg-neutral-950/70 p-3 rounded-xl border border-neutral-800">
              <div className="flex justify-between text-xs">
                <span className="text-neutral-300 font-medium">Camera Exposure Bias (EV Gain)</span>
                <span className="font-mono text-amber-400 font-bold">
                  {settings.exposureCompensation > 0 ? `+${settings.exposureCompensation.toFixed(1)}` : settings.exposureCompensation.toFixed(1)} EV
                </span>
              </div>
              <div className="grid grid-cols-6 gap-1.5 pt-1">
                {[-1.0, 0.0, 0.5, 0.7, 1.0, 1.5].map((ev) => (
                  <button
                    key={ev}
                    onClick={() => onUpdateSettings({ exposureCompensation: ev })}
                    className={`py-1.5 px-1 rounded text-center font-mono text-[11px] font-bold border transition-all ${
                      Math.abs(settings.exposureCompensation - ev) < 0.05
                        ? 'bg-amber-400 text-black border-amber-300 shadow'
                        : 'bg-neutral-800 text-neutral-300 border-neutral-700 hover:bg-neutral-700'
                    }`}
                  >
                    {ev > 0 ? `+${ev}` : ev}
                  </button>
                ))}
              </div>
              <p className="text-[11px] text-neutral-400 mt-1">Directly pushes sensor exposure target to brighten shadowy corners</p>
            </div>
          </div>

          {/* SECTION 5: ANTI-PERIMETER DARKNESS & TORCH FLAT-FIELD */}
          <div className="space-y-4 pt-4">
            <div>
              <label className="font-semibold text-neutral-200">Low-Light Torch & Perimeter Optical Tuning</label>
              <p className="text-xs text-neutral-400">Fixes the dark perimeter / vignette and flattens flashlight hotspots</p>
            </div>

            {/* Vignette / Perimeter Gain Slider */}
            <div className="space-y-1.5 bg-neutral-950/60 p-3 rounded-xl border border-neutral-800">
              <div className="flex justify-between text-xs">
                <span className="text-neutral-300 font-medium">Anti-Perimeter Darkness Gain</span>
                <span className="font-mono text-cyan-400 font-bold">+{settings.vignetteCompensation}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={settings.vignetteCompensation}
                onChange={(e) => onUpdateSettings({ vignetteCompensation: Number(e.target.value) })}
                className="w-full accent-cyan-400 cursor-pointer"
              />
              <p className="text-[11px] text-neutral-400">Compensates cos⁴ lens falloff and illuminates outer edges evenly</p>
            </div>

            {/* Torch Hotspot Flattening */}
            <div className="space-y-1.5 bg-neutral-950/60 p-3 rounded-xl border border-neutral-800">
              <div className="flex justify-between text-xs">
                <span className="text-neutral-300 font-medium">Torch Hotspot Flattening</span>
                <span className="font-mono text-amber-400 font-bold">{settings.centerTorchFill}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={settings.centerTorchFill}
                onChange={(e) => onUpdateSettings({ centerTorchFill: Number(e.target.value) })}
                className="w-full accent-amber-400 cursor-pointer"
              />
              <p className="text-[11px] text-neutral-400">Smooths the phone flashlight center beam into a diffuse wash</p>
            </div>

            {/* Low-Light Shadow Lift */}
            <div className="space-y-1.5 bg-neutral-950/60 p-3 rounded-xl border border-neutral-800">
              <div className="flex justify-between text-xs">
                <span className="text-neutral-300 font-medium">Low-Light Shadow Lift</span>
                <span className="font-mono text-emerald-400 font-bold">+{settings.shadowLift}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={settings.shadowLift}
                onChange={(e) => onUpdateSettings({ shadowLift: Number(e.target.value) })}
                className="w-full accent-emerald-400 cursor-pointer"
              />
              <p className="text-[11px] text-neutral-400">Lifts dark room shadows before encoding for clean grading in DaVinci</p>
            </div>
          </div>

          {/* SECTION 5: CAMERA2 ISP PROCESSING (Prompt.txt specs) */}
          <div className="space-y-3 pt-4">
            <div>
              <label className="font-semibold text-neutral-200">Camera2 ISP Capture Controls (Prompt.txt)</label>
              <p className="text-xs text-neutral-400">Reduces hardware heat generation and preserves organic texture for post-production</p>
            </div>

            <div className="space-y-2">
              <div className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/60 border border-neutral-800">
                <div>
                  <div className="font-bold text-xs text-neutral-200">Force CONTROL_EDGE_MODE = OFF</div>
                  <div className="text-[11px] text-neutral-400">Disables harsh digital artificial edge sharpening</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.edgeModeOff}
                  onChange={(e) => onUpdateSettings({ edgeModeOff: e.target.checked })}
                  className="w-4 h-4 accent-amber-400 rounded"
                />
              </div>

              <div className="flex items-center justify-between p-3 rounded-xl bg-neutral-950/60 border border-neutral-800">
                <div>
                  <div className="font-bold text-xs text-neutral-200">Force CONTROL_NOISE_REDUCTION_MODE = OFF</div>
                  <div className="text-[11px] text-neutral-400">Avoids plastic mushy skin; preserves organic film grain</div>
                </div>
                <input
                  type="checkbox"
                  checked={settings.noiseReductionOff}
                  onChange={(e) => onUpdateSettings({ noiseReductionOff: e.target.checked })}
                  className="w-4 h-4 accent-amber-400 rounded"
                />
              </div>
            </div>
          </div>

          {/* SECTION 6: DUAL RECORDING LAYOUT */}
          <div className="space-y-3 pt-4">
            <label className="font-semibold text-neutral-200">Dual Recording Configuration</label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              {[
                { id: 'two_files', label: 'Two Separate MP4s', desc: 'Simultaneous independent files for Main & UW' },
                { id: 'side_by_side', label: 'Side-by-Side (50/50)', desc: 'Split screen cinema composite' },
                { id: 'pip', label: 'Picture-in-Picture', desc: 'Main full with UW corner preview' }
              ].map((item) => (
                <button
                  key={item.id}
                  onClick={() => onUpdateSettings({ dualRecordingLayout: item.id as any })}
                  className={`p-3 rounded-xl border text-left transition-all ${
                    settings.dualRecordingLayout === item.id
                      ? 'bg-neutral-800 border-amber-400'
                      : 'bg-neutral-950/60 border-neutral-800 hover:bg-neutral-800/40'
                  }`}
                >
                  <div className="font-bold text-xs text-white">{item.label}</div>
                  <div className="text-[10px] text-neutral-400 mt-0.5">{item.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* SECTION 7: ANTI-FLICKER (Halogen / LED) */}
          <div className="space-y-3 pt-4">
            <label className="font-semibold text-neutral-200">Anti-Flicker Filter (Halogen / LED Rooms)</label>
            <div className="grid grid-cols-3 gap-2">
              {[
                { id: '50hz', label: '50 Hz (Europe / Halogen)' },
                { id: '60hz', label: '60 Hz (US / Standard LED)' },
                { id: 'off', label: 'Auto / Off' }
              ].map((flicker) => (
                <button
                  key={flicker.id}
                  onClick={() => onUpdateSettings({ antiFlicker: flicker.id as any })}
                  className={`p-2.5 rounded-lg border text-center text-xs font-medium transition-all ${
                    settings.antiFlicker === flicker.id
                      ? 'bg-neutral-200 text-black border-neutral-100 font-bold'
                      : 'bg-neutral-800 text-neutral-300 border-neutral-700'
                  }`}
                >
                  {flicker.label}
                </button>
              ))}
            </div>
          </div>

          {/* SECTION 8: SYSTEM CODECS & 10-BIT HLG STATUS */}
          <div className="space-y-3 pt-4">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <label className="font-semibold text-neutral-200">Device Hardware Codec & HLG Probe</label>
            </div>
            <div className="bg-neutral-950/80 p-3 rounded-xl border border-neutral-800 font-mono text-[11px] space-y-1.5">
              {codecsSummary.map((c, i) => (
                <div key={i} className="flex items-center justify-between">
                  <span className="text-neutral-300 truncate max-w-[70%]">{c.label}</span>
                  <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                    c.supported ? 'bg-emerald-500/20 text-emerald-400' : 'bg-neutral-800 text-neutral-500'
                  }`}>
                    {c.supported ? 'SUPPORTED' : 'UNAVAILABLE'}
                  </span>
                </div>
              ))}
            </div>
          </div>

        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-neutral-800 bg-neutral-950 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            {onOpenGallery && (
              <button
                onClick={() => {
                  onClose();
                  onOpenGallery();
                }}
                className="px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-neutral-200 text-xs font-mono font-medium transition-colors"
              >
                Clips Library ({recordedClipsCount})
              </button>
            )}
            {onOpenAndroidCode && (
              <button
                onClick={() => {
                  onClose();
                  onOpenAndroidCode();
                }}
                className="px-3 py-1.5 rounded-lg bg-red-950/60 border border-red-800/60 hover:bg-red-900/60 text-red-300 text-xs font-mono font-medium transition-colors"
              >
                Camera2 Kotlin Code
              </button>
            )}
          </div>
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-amber-400 hover:bg-amber-300 text-neutral-950 font-bold text-xs transition-colors shadow-lg shadow-amber-400/20"
          >
            Apply & Return to Camera
          </button>
        </div>

      </div>
    </div>
  );
};
