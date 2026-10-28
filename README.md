# Retina 7 Pro Cinema Camera (Xiaomi 12S Ultra)

Professional cinema recording application designed and calibrated specifically for the **Xiaomi 12S Ultra** (Snapdragon 8+ Gen 1), featuring Sony IMX989 1-inch Main sensor science, Sony IMX586 Ultra-Wide concurrent dual recording, Leica color profiles, and a flat HLG 10-bit BT.2020 pipeline engineered for DaVinci Resolve color grading.

---

## Key Features & Architecture

### 1. Camera2 & Dynamic Resolution Enumeration
- **Runtime Stream Configuration**: Queries `StreamConfigurationMap.getOutputSizes(MediaRecorder::class.java)` and `SurfaceTexture::class.java` dynamically rather than hardcoding sensor aspect ratios or stock camera resolutions.
- **HEVC / H.265 10-Bit HLG Grading Pipeline**:
  - `KEY_COLOR_STANDARD = COLOR_STANDARD_BT2020`
  - `KEY_COLOR_TRANSFER = COLOR_TRANSFER_HLG` (ITU-R BT.2100 Hybrid Log-Gamma)
  - `KEY_PROFILE = HEVCProfileMain10`
- **Organic ISP Texture Preservation**:
  - `CaptureRequest.CONTROL_EDGE_MODE = CONTROL_EDGE_MODE_OFF` (eliminates digital edge haloing)
  - `CaptureRequest.CONTROL_NOISE_REDUCTION_MODE = CONTROL_NOISE_REDUCTION_MODE_OFF` (preserves micro-grain, reduces ISP thermal dissipation, enables noise reduction in DaVinci Resolve Studio)

### 2. Dual-Lens Concurrent Recording
- **Hardware Detection**: Probes `CameraManager.concurrentCameraIds` for main (IMX989) + ultra-wide (IMX586) simultaneous capture.
- **Dual Stream Architecture**: Spawns two independent `MediaCodec` / `MediaRecorder` pipelines writing to two separate MP4 files (e.g. `CINEMA_xxx_MAIN_IMX989.mp4` and `CINEMA_xxx_ULTRAWIDE_IMX586.mp4`) for instant dual-angle safety backup.

### 3. Thermal Throttling & Low-Light Flashlight Lock
- **PowerManager Thermal Monitoring**: `PowerManager.OnThermalStatusChangedListener` actively tracks SoC thermal states. When status reaches `THERMAL_STATUS_MODERATE` or higher:
  - Video encoder bitrate is automatically attenuated by 20% to prevent dropped frames and thermal shutdown.
  - HUD renders a persistent amber/red `THERMAL THROTTLING ACTIVE (-20% BITRATE)` status alert.
- **Independent Torch Lock**: `FLASH_MODE_TORCH` is locked independently of camera recording modes. Switching to `TEMPLATE_RECORD` preserves torch state.
- **Optical Vignette & Torch Hotspot Compensation**: Anti-vignette peripheral gain curves distribute flashlight beams evenly across the full sensor field of view.

### 4. Leica Color Science Profiles
1. **Leica HLG 10-Bit BT.2020 Log**: Wide color gamut log curve preserving shadow and highlight latitude.
2. **Leica Authentic (经典)**: Signature micro-contrast, organic shadow roll-off, authentic skin tones.
3. **Leica Vibrant (生动)**: Expanded dynamic range with punchy saturated midtones.
4. **Leica Monochrom (黑白)**: Pure monochrome tonal gradation inspired by the Leica M Monochrom.
5. **Leica Monochrom High Contrast (黑白高反差)**: Deep blacks and crisp highlights for dramatic street cinema.

---

## Project Structure

```
├── app/
│   ├── build.gradle.kts           # minSdk 35, compileSdk 35, Jetpack Compose, Camera2
│   └── src/main/
│       ├── AndroidManifest.xml    # CAMERA, RECORD_AUDIO, FLASHLIGHT permissions
│       ├── java/com/retina7/cinema/
│       │   ├── MainActivity.kt
│       │   ├── camera/
│       │   │   ├── Camera2Manager.kt      # StreamConfigurationMap, HLG 10-bit BT.2020, Torch lock
│       │   │   └── DualVideoRecorder.kt   # Concurrent dual camera capture to 2 MP4 files
│       │   ├── data/model/
│       │   │   └── CameraModels.kt        # Leica profiles, resolutions, settings, clips
│       │   ├── processing/
│       │   │   └── LeicaColorPipeline.kt  # HLG, S-Curve, anti-vignette peripheral gain
│       │   ├── thermal/
│       │   │   └── ThermalMonitor.kt      # PowerManager thermal throttling listener
│       │   └── ui/
│       │       ├── screens/CameraScreen.kt
│       │       ├── components/CameraHUD.kt
│       │       ├── components/HistogramView.kt
│       │       ├── components/SettingsSheet.kt
│       │       ├── components/GallerySheet.kt
│       │       ├── theme/ (Color.kt, Theme.kt, Type.kt)
│       │       └── viewmodel/CameraViewModel.kt
│       └── res/
│           ├── drawable/ (ic_launcher_background.xml, ic_launcher_foreground.xml)
│           ├── mipmap-anydpi-v26/ (ic_launcher.xml, ic_launcher_round.xml)
│           └── values/ (strings.xml, colors.xml, themes.xml)
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Build & Requirements
- **Target OS**: Android 15 (API 35+)
- **Build System**: Gradle 8.7+ / 9.x (Kotlin DSL), AGP 8.7+
- **JDK**: Java 21
