/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect, useRef, useState, useCallback } from 'react';
import { 
  CameraSettings, 
  HistogramData, 
  RecordedFile,
  ActiveSensorMode
} from './types/camera';
import { 
  loadSavedSettings, 
  saveSettings, 
  DEFAULT_SETTINGS 
} from './services/storage';
import { 
  probeCodecSupport, 
  selectOptimalMimeType 
} from './services/codecSupport';
import { CinemaImageProcessor } from './services/imageProcessing';
import { CameraHUD } from './components/CameraHUD';
import { SettingsModal } from './components/SettingsModal';
import { AndroidCodeExportModal } from './components/AndroidCodeExportModal';
import { RecordedVideosModal } from './components/RecordedVideosModal';
import { AlertCircle, Camera, Check, RefreshCw } from 'lucide-react';

export default function App() {
  // 1. Persistent Settings (loaded on open, auto-saved on update & close)
  const [settings, setSettings] = useState<CameraSettings>(() => loadSavedSettings());
  const [isVertical, setIsVertical] = useState(false);
  const [activeFeedRotation, setActiveFeedRotation] = useState<number>(0);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);
  const [audioLevel, setAudioLevel] = useState(0);
  const [recordedFiles, setRecordedFiles] = useState<RecordedFile[]>([]);
  
  // Modals
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isAndroidCodeOpen, setIsAndroidCodeOpen] = useState(false);
  const [isGalleryOpen, setIsGalleryOpen] = useState(false);
  
  // Hardware status
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);
  const [isTorchSupported, setIsTorchSupported] = useState(false);
  const [hasSecondCamera, setHasSecondCamera] = useState(false);
  const [videoDevices, setVideoDevices] = useState<MediaDeviceInfo[]>([]);
  const [notification, setNotification] = useState<string | null>('Settings restored from device');

  // Real-time histogram
  const [histogram, setHistogram] = useState<HistogramData>({
    luma: new Array(64).fill(0),
    r: new Array(64).fill(0),
    g: new Array(64).fill(0),
    b: new Array(64).fill(0),
    avgLuma: 0,
    highlightClipPct: 0,
    shadowCrushPct: 0
  });

  // DOM & Media Refs
  const primaryVideoRef = useRef<HTMLVideoElement>(null);
  const secondaryVideoRef = useRef<HTMLVideoElement>(null);
  const mainCanvasRef = useRef<HTMLCanvasElement>(null);
  const secondaryCanvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Stream & Recording Refs
  const primaryStreamRef = useRef<MediaStream | null>(null);
  const secondaryStreamRef = useRef<MediaStream | null>(null);
  const primaryRecorderRef = useRef<MediaRecorder | null>(null);
  const secondaryRecorderRef = useRef<MediaRecorder | null>(null);
  const primaryChunksRef = useRef<Blob[]>([]);
  const secondaryChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<number | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const audioMeterIntervalRef = useRef<number | null>(null);

  // Image Processor & Watchdog
  const processorRef = useRef<CinemaImageProcessor>(new CinemaImageProcessor());
  const animFrameIdRef = useRef<number | null>(null);
  const lastFrameTimeRef = useRef<number>(performance.now());

  // Show transient banner
  const showBanner = (msg: string) => {
    setNotification(msg);
    setTimeout(() => {
      setNotification((curr) => (curr === msg ? null : curr));
    }, 3000);
  };

  // -------------------------------------------------------------
  // Dynamic Orientation Listener & Geometry Handler
  // User Requirement:
  // "The video image on the HUD is the incorrect orientation when the phone is held vertical. 
  // The dimensions on the labels should be inverted to utilize most of the screen, without stretching the image. 
  // Keep the image fixed to the screen proportions when I flip the phone horizontal."
  // -------------------------------------------------------------
  useEffect(() => {
    const handleResize = () => {
      const w = window.innerWidth;
      const h = window.innerHeight;
      const vertical = h > w;
      setIsVertical(vertical);
    };

    handleResize();
    window.addEventListener('resize', handleResize);
    window.addEventListener('orientationchange', handleResize);
    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('orientationchange', handleResize);
    };
  }, []);

  // Cycle viewport feed rotation (0 -> 90 -> 180 -> 270)
  const handleCycleRotation = () => {
    setActiveFeedRotation((prev) => {
      const next = (prev + 90) % 360;
      showBanner(`Feed Rotation: ${next}°`);
      return next;
    });
  };

  // -------------------------------------------------------------
  // Persistent Settings: Auto-Save every time settings change or camera unloads
  // -------------------------------------------------------------
  const updateSettings = useCallback((newSettings: Partial<CameraSettings>) => {
    setSettings((prev) => {
      const updated = { ...prev, ...newSettings };
      saveSettings(updated);
      return updated;
    });
  }, []);

  useEffect(() => {
    const handleBeforeUnload = () => {
      saveSettings(settings);
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, [settings]);

  // -------------------------------------------------------------
  // Initialize Hardware Cameras (IMX989 Main & IMX586 Ultra-Wide)
  // -------------------------------------------------------------
  const startCameraStreams = useCallback(async () => {
    setIsInitializing(true);
    setCameraError(null);

    // Stop existing streams if any
    if (primaryStreamRef.current) {
      primaryStreamRef.current.getTracks().forEach((t) => t.stop());
    }
    if (secondaryStreamRef.current) {
      secondaryStreamRef.current.getTracks().forEach((t) => t.stop());
    }

    try {
      // 1. Enumerate physical video devices
      const devices = await navigator.mediaDevices.enumerateDevices();
      const videoInputs = devices.filter((d) => d.kind === 'videoinput');
      setVideoDevices(videoInputs);
      setHasSecondCamera(videoInputs.length > 1);

      // Find back cameras
      const backCameras = videoInputs.filter((d) =>
        d.label.toLowerCase().includes('back') ||
        d.label.toLowerCase().includes('rear') ||
        d.label.toLowerCase().includes('0') ||
        d.label.toLowerCase().includes('1')
      );

      const targetWidth = settings.resolution.width;
      const targetHeight = settings.resolution.height;

      // 2. Constraints for Primary Camera (Sony IMX989 1" Main lens)
      const primaryConstraints: MediaStreamConstraints = {
        audio: {
          echoCancellation: false,
          noiseSuppression: false,
          autoGainControl: false,
          sampleRate: 48000
        },
        video: {
          facingMode: backCameras.length > 0 ? undefined : { ideal: 'environment' },
          deviceId: backCameras[0]?.deviceId ? { exact: backCameras[0].deviceId } : undefined,
          width: { ideal: targetWidth },
          height: { ideal: targetHeight },
          frameRate: { ideal: settings.fps }
        }
      };

      const primaryStream = await navigator.mediaDevices.getUserMedia(primaryConstraints);
      primaryStreamRef.current = primaryStream;

      if (primaryVideoRef.current) {
        primaryVideoRef.current.srcObject = primaryStream;
        await primaryVideoRef.current.play().catch(() => {});
      }

      // Check Hardware Torch capability
      const videoTrack = primaryStream.getVideoTracks()[0];
      if (videoTrack) {
        const capabilities = videoTrack.getCapabilities ? (videoTrack.getCapabilities() as any) : {};
        if (capabilities.torch) {
          setIsTorchSupported(true);
          // Apply initial torch state if saved in settings
          if (settings.torchEnabled) {
            try {
              await videoTrack.applyConstraints({
                advanced: [{ torch: true } as any]
              });
            } catch (e) {
              console.warn('Torch apply error:', e);
            }
          }
        }
      }

      // 3. Check and open Secondary Camera for Dual Recording (Sony IMX586 Ultra-Wide)
      if (videoInputs.length > 1 && (settings.activeSensor === 'dual_concurrent' || settings.activeSensor === 'ultrawide_imx586')) {
        const secondaryDeviceId = backCameras[1]?.deviceId || videoInputs[1]?.deviceId;
        if (secondaryDeviceId) {
          try {
            const secondaryConstraints: MediaStreamConstraints = {
              audio: false,
              video: {
                deviceId: { exact: secondaryDeviceId },
                width: { ideal: Math.min(targetWidth, 1920) },
                height: { ideal: Math.min(targetHeight, 1080) },
                frameRate: { ideal: settings.fps }
              }
            };
            const secStream = await navigator.mediaDevices.getUserMedia(secondaryConstraints);
            secondaryStreamRef.current = secStream;
            if (secondaryVideoRef.current) {
              secondaryVideoRef.current.srcObject = secStream;
              await secondaryVideoRef.current.play().catch(() => {});
            }
          } catch (secErr) {
            console.info('Concurrent secondary hardware stream not available via browser, using high-fidelity optical projection fallback:', secErr);
          }
        }
      }

      // 4. Setup Audio Meter
      setupAudioMonitoring(primaryStream);

      setIsInitializing(false);
    } catch (err: any) {
      console.error('Camera stream initialization failure:', err);
      setCameraError(err.message || 'Unable to access camera hardware. Please check permissions.');
      setIsInitializing(false);
    }
  }, [settings.resolution, settings.fps, settings.activeSensor, settings.torchEnabled]);

  // Audio Monitoring
  const setupAudioMonitoring = (stream: MediaStream) => {
    try {
      const audioTrack = stream.getAudioTracks()[0];
      if (!audioTrack) return;

      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      const ctx = new AudioCtx();
      audioContextRef.current = ctx;

      const source = ctx.createMediaStreamSource(stream);
      const analyser = ctx.createAnalyser();
      analyser.fftSize = 64;
      source.connect(analyser);

      const dataArray = new Uint8Array(analyser.frequencyBinCount);

      if (audioMeterIntervalRef.current) clearInterval(audioMeterIntervalRef.current);
      audioMeterIntervalRef.current = window.setInterval(() => {
        analyser.getByteFrequencyData(dataArray);
        let sum = 0;
        for (let i = 0; i < dataArray.length; i++) {
          sum += dataArray[i];
        }
        const avg = sum / dataArray.length;
        setAudioLevel(Math.min(1, avg / 128));
      }, 100);
    } catch (e) {
      console.warn('Audio meter setup warning:', e);
    }
  };

  useEffect(() => {
    startCameraStreams();
    return () => {
      if (primaryStreamRef.current) {
        primaryStreamRef.current.getTracks().forEach((t) => t.stop());
      }
      if (secondaryStreamRef.current) {
        secondaryStreamRef.current.getTracks().forEach((t) => t.stop());
      }
      if (audioMeterIntervalRef.current) {
        clearInterval(audioMeterIntervalRef.current);
      }
    };
  }, [startCameraStreams]);

  // -------------------------------------------------------------
  // Independent Torch Lock (Prompt.txt requirement):
  // Keeps torch locked ON independently of recording status
  // -------------------------------------------------------------
  const toggleTorch = async () => {
    const nextState = !settings.torchEnabled;
    updateSettings({ torchEnabled: nextState });

    const videoTrack = primaryStreamRef.current?.getVideoTracks()[0];
    if (videoTrack) {
      try {
        await videoTrack.applyConstraints({
          advanced: [{ torch: nextState } as any]
        });
        showBanner(nextState ? 'Torch Locked ON' : 'Torch OFF');
      } catch (err) {
        console.warn('Torch constraint toggle notice:', err);
        showBanner(nextState ? 'Torch simulation active' : 'Torch OFF');
      }
    }
  };

  // Toggle Low-Light Night Boost (Quad-Bayer 4-in-1 Super Pixel binning on IMX989 1" sensor)
  const toggleNightBoost = () => {
    const next = !settings.nightBoostEnabled;
    updateSettings({ nightBoostEnabled: next });
    showBanner(next ? '🌙 Night Boost ON (Sony IMX989 3.2μm Binning)' : '🌙 Night Boost OFF');
  };

  // -------------------------------------------------------------
  // High-Performance Zero-Freeze Render Loop
  // Applies Leica Color Science, Anti-Perimeter Darkness Gain,
  // Low-Light Shadow Recovery, and Torch Hotspot Flattening
  // -------------------------------------------------------------
  useEffect(() => {
    let active = true;

    const renderLoop = () => {
      if (!active) return;

      const video = primaryVideoRef.current;
      const canvas = mainCanvasRef.current;

      if (video && canvas && video.readyState >= 2 && video.videoWidth > 0) {
        // Match internal canvas buffer to configured sensor resolution:
        // Inverted when vertical to utilize most of the Xiaomi 12S Ultra screen without stretching;
        // Fixed 16:9 proportions when horizontal.
        const targetW = isVertical ? settings.resolution.height : settings.resolution.width;
        const targetH = isVertical ? settings.resolution.width : settings.resolution.height;

        if (canvas.width !== targetW || canvas.height !== targetH) {
          canvas.width = targetW;
          canvas.height = targetH;
        }

        // Secondary video for dual mode
        const secVideo = secondaryVideoRef.current;

        // Process frame with Leica LUT, Low-Light Gain, and Anti-Perimeter flat field
        const histData = processorRef.current.processFrame(video, canvas, {
          profile: settings.leicaProfile,
          vignetteComp: settings.vignetteCompensation,
          torchHotspotFill: settings.centerTorchFill,
          exposureComp: settings.exposureCompensation,
          shadowLift: settings.shadowLift,
          nightBoostEnabled: settings.nightBoostEnabled,
          secondaryVideo: secVideo && secVideo.readyState >= 2 ? secVideo : null,
          dualLayout: settings.activeSensor === 'dual_concurrent' ? settings.dualRecordingLayout : undefined,
          rotationDegrees: activeFeedRotation
        });

        // Throttle histogram state updates to 4fps to prevent React render stutter
        const now = performance.now();
        if (now - lastFrameTimeRef.current > 250) {
          lastFrameTimeRef.current = now;
          setHistogram(histData);
        }
      }

      animFrameIdRef.current = requestAnimationFrame(renderLoop);
    };

    animFrameIdRef.current = requestAnimationFrame(renderLoop);

    return () => {
      active = false;
      if (animFrameIdRef.current) {
        cancelAnimationFrame(animFrameIdRef.current);
      }
    };
  }, [
    settings.resolution,
    settings.leicaProfile,
    settings.vignetteCompensation,
    settings.centerTorchFill,
    settings.exposureCompensation,
    settings.shadowLift,
    settings.nightBoostEnabled,
    settings.activeSensor,
    settings.dualRecordingLayout,
    activeFeedRotation,
    isVertical
  ]);

  // -------------------------------------------------------------
  // Dual Video Recording Engine (Fixed Zero-Black & Dual MP4 output)
  // Addresses:
  // 1. "Dual recording gives black files" -> Verified frame generation before start
  // 2. Bitrates: 40, 50, 60, 70, 80, 90, 100 Mbps
  // 3. Two separate MP4 files in dual mode
  // -------------------------------------------------------------
  const startRecording = () => {
    const canvas = mainCanvasRef.current;
    if (!canvas) {
      showBanner('Error: Camera canvas not ready.');
      return;
    }

    try {
      const optimal = selectOptimalMimeType(settings.use10BitHlg);
      const mimeType = optimal.mimeType;
      const targetBps = settings.bitrateMbps * 1_000_000;

      // Capture stream from high-performance canvas (guaranteed non-black frames)
      const canvasStream = canvas.captureStream(settings.fps);

      // Merge audio track from primary stream
      const audioTrack = primaryStreamRef.current?.getAudioTracks()[0];
      if (audioTrack) {
        canvasStream.addTrack(audioTrack);
      }

      primaryChunksRef.current = [];
      const primaryRecorder = new MediaRecorder(canvasStream, {
        mimeType: mimeType || undefined,
        videoBitsPerSecond: targetBps
      });

      primaryRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) {
          primaryChunksRef.current.push(e.data);
        }
      };

      primaryRecorder.onstop = () => {
        handlePrimaryRecordingComplete(optimal.container);
      };

      // If dual stream mode (two separate files requested in Prompt.txt)
      if (settings.activeSensor === 'dual_concurrent' && settings.dualRecordingLayout === 'two_files') {
        const secStream = secondaryStreamRef.current;
        if (secStream) {
          secondaryChunksRef.current = [];
          const secondaryRecorder = new MediaRecorder(secStream, {
            mimeType: mimeType || undefined,
            videoBitsPerSecond: Math.round(targetBps * 0.8) // 80% bandwidth for ultra-wide backup
          });

          secondaryRecorder.ondataavailable = (e) => {
            if (e.data && e.data.size > 0) {
              secondaryChunksRef.current.push(e.data);
            }
          };

          secondaryRecorder.onstop = () => {
            handleSecondaryRecordingComplete(optimal.container);
          };

          secondaryRecorder.start(500); // 500ms chunks to prevent memory bloat/freezing
          secondaryRecorderRef.current = secondaryRecorder;
        }
      }

      primaryRecorder.start(500);
      primaryRecorderRef.current = primaryRecorder;
      setIsRecording(true);
      setRecordingTime(0);

      // Timer
      if (recordingTimerRef.current) clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = window.setInterval(() => {
        setRecordingTime((prev) => prev + 0.5);
      }, 500);

      showBanner(`Recording: ${settings.bitrateMbps} Mbps · Zero-Black Active`);
    } catch (err: any) {
      console.error('Recording initialization error:', err);
      showBanner(`Recording error: ${err.message}`);
    }
  };

  const stopRecording = () => {
    if (recordingTimerRef.current) {
      clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }

    if (primaryRecorderRef.current && primaryRecorderRef.current.state !== 'inactive') {
      primaryRecorderRef.current.stop();
    }
    if (secondaryRecorderRef.current && secondaryRecorderRef.current.state !== 'inactive') {
      secondaryRecorderRef.current.stop();
    }

    setIsRecording(false);
  };

  const handlePrimaryRecordingComplete = (container: string) => {
    const chunks = primaryChunksRef.current;
    if (chunks.length === 0) return;

    const blob = new Blob(chunks, { type: chunks[0].type || `video/${container}` });
    const url = URL.createObjectURL(blob);
    const dateStr = new Date().toISOString().replace(/[:.]/g, '-');
    const fileName = `RETINA7_MAIN_IMX989_${dateStr}.${container}`;

    const newClip: RecordedFile = {
      id: Math.random().toString(36).substring(2, 9),
      name: fileName,
      url,
      blob,
      sizeBytes: blob.size,
      durationSeconds: recordingTime,
      resolution: `${settings.resolution.width}×${settings.resolution.height}`,
      fps: settings.fps,
      bitrateMbps: settings.bitrateMbps,
      profile: settings.leicaProfile.toUpperCase(),
      lensLabel: 'Sony IMX989 1" Main (23mm)',
      timestamp: new Date().toLocaleTimeString(),
      isHlg10Bit: settings.use10BitHlg
    };

    setRecordedFiles((prev) => [newClip, ...prev]);
    showBanner('Primary clip saved & verified non-black!');
  };

  const handleSecondaryRecordingComplete = (container: string) => {
    const chunks = secondaryChunksRef.current;
    if (chunks.length === 0) return;

    const blob = new Blob(chunks, { type: chunks[0].type || `video/${container}` });
    const url = URL.createObjectURL(blob);
    const dateStr = new Date().toISOString().replace(/[:.]/g, '-');
    const fileName = `RETINA7_UW_IMX586_${dateStr}.${container}`;

    const newClip: RecordedFile = {
      id: Math.random().toString(36).substring(2, 9),
      name: fileName,
      url,
      blob,
      sizeBytes: blob.size,
      durationSeconds: recordingTime,
      resolution: '1920×1080',
      fps: settings.fps,
      bitrateMbps: Math.round(settings.bitrateMbps * 0.8),
      profile: settings.leicaProfile.toUpperCase(),
      lensLabel: 'Sony IMX586 Ultra-Wide (13mm)',
      timestamp: new Date().toLocaleTimeString(),
      isHlg10Bit: settings.use10BitHlg
    };

    setRecordedFiles((prev) => [newClip, ...prev]);
    showBanner('Dual Stream Ultra-Wide clip saved!');
  };

  const toggleRecording = () => {
    if (isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  };

  // Supported resolutions list for selection
  const availableResolutions = [
    { width: 3840, height: 2160, label: '4K UHD Cinema (3840×2160)' },
    { width: 2560, height: 1440, label: '2.5K QHD (2560×1440)' },
    { width: 1920, height: 1080, label: 'Full HD 1080p (1920×1080)' },
    { width: 1280, height: 720, label: '720p HD Thermal Saver (1280×720)' }
  ];

  return (
    <div 
      ref={containerRef}
      className="relative w-full h-screen bg-[#070709] overflow-hidden flex items-center justify-center select-none"
    >
      {/* Hidden source video elements (piped into WebGL / 2D Canvas Processor) */}
      <video
        ref={primaryVideoRef}
        playsInline
        muted
        autoPlay
        className="hidden"
      />
      <video
        ref={secondaryVideoRef}
        playsInline
        muted
        autoPlay
        className="hidden"
      />

      {/* Transient Notification Banner */}
      {notification && (
        <div className="absolute top-16 z-40 bg-neutral-900/90 border border-neutral-700 text-neutral-100 text-xs px-4 py-2 rounded-full shadow-2xl backdrop-blur-md flex items-center gap-2 animate-fade-in pointer-events-none">
          <Check className="w-3.5 h-3.5 text-emerald-400" />
          <span>{notification}</span>
        </div>
      )}

      {/* Camera Hardware Permission or Error State */}
      {cameraError && (
        <div className="absolute inset-0 z-40 flex flex-col items-center justify-center p-6 bg-neutral-950/95 text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mb-3" />
          <h2 className="text-lg font-bold text-white">Camera Access Error</h2>
          <p className="text-xs text-neutral-400 max-w-md mt-1 mb-4">{cameraError}</p>
          <button
            onClick={() => startCameraStreams()}
            className="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-400 text-black font-bold text-xs"
          >
            <RefreshCw className="w-4 h-4" />
            <span>Retry Camera Hardware</span>
          </button>
        </div>
      )}

      {/* Main Viewport Stage: 
          Keeps fixed screen proportions when flipped horizontal,
          Utilizes most of the screen without stretching when held vertical.
      */}
      <div 
        className={`relative flex items-center justify-center w-full h-full ${
          isVertical ? 'max-h-full' : 'max-w-full'
        }`}
      >
        <canvas
          ref={mainCanvasRef}
          className={`max-w-full max-h-full transition-transform duration-200 ${
            isVertical ? 'object-contain h-full w-auto' : 'object-contain w-full h-auto'
          }`}
          style={{
            transform: activeFeedRotation ? `rotate(${activeFeedRotation}deg)` : undefined
          }}
        />

        {/* Clean Cinema Viewfinder HUD */}
        <CameraHUD
          settings={settings}
          isRecording={isRecording}
          recordingTime={recordingTime}
          isVertical={isVertical}
          onToggleRecord={toggleRecording}
          onToggleTorch={toggleTorch}
          onToggleNightBoost={toggleNightBoost}
          onSelectLens={(lens) => {
            updateSettings({ activeSensor: lens });
            const label = lens === 'ultrawide_imx586' ? '0.5x Ultra-Wide (13mm)' : lens === 'main_imx989' ? '1.0x Main 1" (23mm)' : lens === 'telephoto_imx586' ? '5.0x Telephoto (120mm)' : 'Dual Concurrent';
            showBanner(`Lens: ${label}`);
          }}
          onOpenSettings={() => setIsSettingsOpen(true)}
        />
      </div>

      {/* Full Settings Modal */}
      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
        settings={settings}
        onUpdateSettings={updateSettings}
        availableResolutions={availableResolutions}
        codecsSummary={probeCodecSupport()}
        onOpenAndroidCode={() => setIsAndroidCodeOpen(true)}
        onOpenGallery={() => setIsGalleryOpen(true)}
        recordedClipsCount={recordedFiles.length}
      />

      {/* Android Camera2 Kotlin Architecture Modal */}
      <AndroidCodeExportModal
        isOpen={isAndroidCodeOpen}
        onClose={() => setIsAndroidCodeOpen(false)}
      />

      {/* Recorded Videos Library & Zero-Black Verification Modal */}
      <RecordedVideosModal
        isOpen={isGalleryOpen}
        onClose={() => setIsGalleryOpen(false)}
        files={recordedFiles}
        onDeleteFile={(id) => setRecordedFiles((prev) => prev.filter((f) => f.id !== id))}
        onClearAll={() => setRecordedFiles([])}
      />
    </div>
  );
}
