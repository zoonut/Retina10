/**
 * Codec, Profile, and HDR Color Space Probe
 * Tests for HLG 10-bit, 8-bit, H.264, and HEVC/H.265 capabilities.
 */
import { CodecCapability } from '../types/camera';

export interface SystemCapabilities {
  supportsTorch: boolean;
  supportsDualCameras: boolean;
  availableDevices: MediaDeviceInfo[];
  codecs: CodecCapability[];
  bestHdrMimeType: string;
  bestStandardMimeType: string;
}

export function probeCodecSupport(): CodecCapability[] {
  const candidates: Array<{ mimeType: string; label: string; is10Bit: boolean; colorGamut: string }> = [
    {
      mimeType: 'video/mp4;codecs=hvc1.2.4.L153.B0',
      label: 'HEVC / H.265 Main10 (HLG 10-bit BT.2020)',
      is10Bit: true,
      colorGamut: 'BT.2020 (HLG)'
    },
    {
      mimeType: 'video/mp4;codecs=hvc1',
      label: 'HEVC / H.265 Standard (8-bit / 10-bit auto)',
      is10Bit: false,
      colorGamut: 'BT.709 / BT.2020'
    },
    {
      mimeType: 'video/webm;codecs=vp09.02.10.10.01',
      label: 'VP9 Profile 2 (10-bit HDR / HLG)',
      is10Bit: true,
      colorGamut: 'BT.2020 (HLG)'
    },
    {
      mimeType: 'video/mp4;codecs=avc1.640028',
      label: 'H.264 / AVC High Profile (Level 4.0/4.2)',
      is10Bit: false,
      colorGamut: 'BT.709 / Flat Log'
    },
    {
      mimeType: 'video/mp4;codecs=avc1',
      label: 'H.264 / AVC Standard MP4',
      is10Bit: false,
      colorGamut: 'BT.709'
    },
    {
      mimeType: 'video/webm;codecs=h264',
      label: 'H.264 in WebM Container',
      is10Bit: false,
      colorGamut: 'BT.709'
    },
    {
      mimeType: 'video/webm;codecs=vp9,opus',
      label: 'VP9 Studio Master (10-bit capable)',
      is10Bit: true,
      colorGamut: 'Wide Color'
    },
    {
      mimeType: 'video/webm;codecs=vp8,opus',
      label: 'VP8 Standard Container',
      is10Bit: false,
      colorGamut: 'BT.709'
    },
    {
      mimeType: 'video/webm',
      label: 'Standard WebM Default',
      is10Bit: false,
      colorGamut: 'Standard'
    }
  ];

  if (typeof window === 'undefined' || typeof MediaRecorder === 'undefined') {
    return candidates.map(c => ({ ...c, supported: false }));
  }

  return candidates.map(c => {
    let supported = false;
    try {
      supported = MediaRecorder.isTypeSupported(c.mimeType);
    } catch {
      supported = false;
    }
    return {
      ...c,
      supported
    };
  });
}

export function selectOptimalMimeType(prefer10BitHlg: boolean = true): { mimeType: string; is10Bit: boolean; container: string } {
  const capabilities = probeCodecSupport();
  
  if (prefer10BitHlg) {
    // Try 10-bit candidates first
    const hdrSupported = capabilities.find(c => c.supported && c.is10Bit);
    if (hdrSupported) {
      const isMp4 = hdrSupported.mimeType.includes('mp4');
      return {
        mimeType: hdrSupported.mimeType,
        is10Bit: true,
        container: isMp4 ? 'mp4' : 'webm'
      };
    }
  }

  // Fallback to highest quality H.264 or VP9
  const standardSupported = capabilities.find(c => c.supported && (c.mimeType.includes('avc1') || c.mimeType.includes('h264') || c.mimeType.includes('vp9')));
  if (standardSupported) {
    const isMp4 = standardSupported.mimeType.includes('mp4');
    return {
      mimeType: standardSupported.mimeType,
      is10Bit: false,
      container: isMp4 ? 'mp4' : 'webm'
    };
  }

  // Any supported
  const anySupported = capabilities.find(c => c.supported);
  if (anySupported) {
    const isMp4 = anySupported.mimeType.includes('mp4');
    return {
      mimeType: anySupported.mimeType,
      is10Bit: anySupported.is10Bit,
      container: isMp4 ? 'mp4' : 'webm'
    };
  }

  return {
    mimeType: '',
    is10Bit: false,
    container: 'webm'
  };
}
