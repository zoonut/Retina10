/**
 * Persistent settings storage service for Retina 7 Pro Cinema Camera
 * Automatically loads on boot and saves upon any change or camera close.
 */
import { CameraSettings } from '../types/camera';

const STORAGE_KEY = 'retina7_pro_camera_settings_v2';

export const DEFAULT_SETTINGS: CameraSettings = {
  // Bitrate options required: 40, 50, 60, 70, 80, 90, 100 Mbps
  bitrateMbps: 60,
  resolution: { width: 3840, height: 2160, label: '4K UHD (3840×2160)' },
  fps: 30,
  codec: 'hevc_hlg_10bit',
  use10BitHlg: true,
  
  // Leica color science & low-light / perimeter anti-darkness
  leicaProfile: 'leica_hlg_bt2020', // Default to DaVinci Resolve HLG BT.2020 as requested
  vignetteCompensation: 65, // Significant boost for perimeter illumination
  centerTorchFill: 40, // Flattens phone torch hotspot
  exposureCompensation: 0.7, // Slightly brightened baseline for dark room shooting
  shadowLift: 50, // Low-light shadow detail lift
  edgeModeOff: true, // Reduced ISP processing for natural organic texture
  noiseReductionOff: true, // Minimized noise reduction to prevent mushy details

  // Poorly Lit Room & Night Low-Light Tuning (Sony IMX989 1" Sensor)
  nightBoostEnabled: true, // Enabled for superior low-light interior performance
  hyperOis: true, // HyperOIS optical stabilization for slow shutter handhold
  lowLightShutterPriority: true, // 24 FPS / 1/24s maximum photon integration
  
  // Hardware sensors
  activeSensor: 'main_imx989',
  dualRecordingLayout: 'two_files',
  torchEnabled: false,
  antiFlicker: '50hz', // Typical for halogen / European LED lights
  
  // HUD
  orientationMode: 'auto',
  showHistogram: true,
  showWaveform: false,
  showFocusPeaking: false,
  showZebraHighlights: false,
  gridType: 'rule_of_thirds',
  
  // Thermal
  thermalThrottlingActive: false,
  thermalStatus: 'NORMAL'
};

export function loadSavedSettings(): CameraSettings {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    const parsed = JSON.parse(raw);
    
    // Ensure bitrate is one of the valid options [40, 50, 60, 70, 80, 90, 100]
    const validBitrates = [40, 50, 60, 70, 80, 90, 100];
    if (!validBitrates.includes(parsed.bitrateMbps)) {
      parsed.bitrateMbps = 60;
    }
    
    return { ...DEFAULT_SETTINGS, ...parsed };
  } catch (err) {
    console.warn('Failed to load saved camera settings, using defaults', err);
    return DEFAULT_SETTINGS;
  }
}

export function saveSettings(settings: Partial<CameraSettings>): void {
  try {
    const current = loadSavedSettings();
    const updated = { ...current, ...settings };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
  } catch (err) {
    console.warn('Failed to save camera settings to localStorage', err);
  }
}
