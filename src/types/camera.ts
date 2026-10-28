/**
 * Types for Retina 7 Pro Cinema Camera
 * Specialized for Xiaomi 12S Ultra (Sony IMX989 1" Main + Sony IMX586 Ultra-Wide)
 */

export type LeicaColorProfile = 
  | 'leica_authentic'
  | 'leica_vibrant'
  | 'leica_monochrom'
  | 'leica_monochrom_hc'
  | 'leica_hlg_bt2020'; // DaVinci Resolve Flat Log with BT.2020 gamut

export type ActiveSensorMode = 'main_imx989' | 'ultrawide_imx586' | 'telephoto_imx586' | 'dual_concurrent';

export type DualRecordingLayout = 'two_files' | 'side_by_side' | 'pip';

export type OrientationMode = 'auto' | 'vertical' | 'horizontal';

export type AntiFlickerMode = 'off' | '50hz' | '60hz';

export interface CameraResolution {
  width: number;
  height: number;
  label: string;
  aspectRatio: string;
  maxFps: number;
  isCustom?: boolean;
}

export interface CameraSettings {
  // Video & Codec
  bitrateMbps: number; // 40, 50, 60, 70, 80, 90, 100
  resolution: { width: number; height: number; label: string };
  fps: number; // 24, 30, 60
  codec: 'hevc_hlg_10bit' | 'hevc_main' | 'h264_high' | 'vp9_10bit';
  use10BitHlg: boolean;
  
  // Imaging & Leica Science
  leicaProfile: LeicaColorProfile;
  vignetteCompensation: number; // 0 to 100% (anti-perimeter darkness)
  centerTorchFill: number; // 0 to 100% (torch hotspot flattening)
  exposureCompensation: number; // -3.0 to +3.0 EV
  shadowLift: number; // 0 to 100% (low-light boost)
  edgeModeOff: boolean; // OFF for organic film texture (Prompt.txt)
  noiseReductionOff: boolean; // OFF for DaVinci Resolve post-denoise (Prompt.txt)

  // Poorly Lit Rooms & Night Low-Light Tuning (IMX989 1" Sensor)
  nightBoostEnabled: boolean;
  hyperOis: boolean;
  lowLightShutterPriority: boolean;
  
  // Sensors & Dual Recording
  activeSensor: ActiveSensorMode;
  dualRecordingLayout: DualRecordingLayout;
  torchEnabled: boolean;
  antiFlicker: AntiFlickerMode;
  
  // HUD & Display
  orientationMode: OrientationMode;
  showHistogram: boolean;
  showWaveform: boolean;
  showFocusPeaking: boolean;
  showZebraHighlights: boolean;
  gridType: 'none' | 'rule_of_thirds' | 'crosshair' | 'cinema_239';
  
  // Thermal & Protection
  thermalThrottlingActive: boolean;
  thermalStatus: 'NORMAL' | 'MODERATE' | 'SEVERE' | 'CRITICAL';
}

export interface HistogramData {
  luma: number[];
  r: number[];
  g: number[];
  b: number[];
  avgLuma: number;
  highlightClipPct: number;
  shadowCrushPct: number;
}

export interface CodecCapability {
  mimeType: string;
  label: string;
  supported: boolean;
  is10Bit: boolean;
  colorGamut: string;
}

export interface RecordedFile {
  id: string;
  url: string;
  blob: Blob;
  name: string;
  sizeBytes: number;
  durationSeconds: number;
  resolution: string;
  fps: number;
  bitrateMbps: number;
  profile: string;
  lensLabel: string;
  timestamp: string;
  isHlg10Bit: boolean;
}
