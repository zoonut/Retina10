package com.retina7.cinema.data.model

import android.util.Size

/**
 * Leica Color Science Profiles for Xiaomi 12S Ultra
 */
enum class LeicaColorProfile(
    val id: String,
    val title: String,
    val description: String,
    val isLog: Boolean = false
) {
    LEICA_HLG_BT2020(
        id = "leica_hlg_bt2020",
        title = "Leica HLG 10-Bit BT.2020 Log",
        description = "DaVinci Resolve Flat Log. ITU-R BT.2100 Hybrid Log-Gamma with wide BT.2020 color space.",
        isLog = true
    ),
    LEICA_AUTHENTIC(
        id = "leica_authentic",
        title = "Leica Authentic (经典)",
        description = "Signature micro-contrast, organic shadow roll-off, authentic skin tones, warm reds/ambers."
    ),
    LEICA_VIBRANT(
        id = "leica_vibrant",
        title = "Leica Vibrant (生动)",
        description = "Elevated dynamic range, punchy saturated midtones, ideal for vivid scenes."
    ),
    LEICA_MONOCHROM(
        id = "leica_monochrom",
        title = "Leica Monochrom (黑白)",
        description = "Pure monochrome sensor rendering with smooth tonal gradation inspired by Leica M Monochrom."
    ),
    LEICA_MONOCHROM_HC(
        id = "leica_monochrom_hc",
        title = "Leica Monochrom High Contrast (黑白高反差)",
        description = "Deep blacks and crisp highlights for expressive cinema street recording."
    )
}

enum class ActiveSensorMode(val label: String, val zoomLabel: String) {
    MAIN_IMX989("Sony IMX989 1\" Main (23mm)", "1.0x"),
    ULTRAWIDE_IMX586("Sony IMX586 Ultra-Wide (13mm)", "0.5x"),
    TELEPHOTO_IMX586("Sony IMX586 Periscope (120mm)", "5.0x"),
    DUAL_CONCURRENT("Dual Concurrent (Main + Ultra-Wide)", "DUAL")
}

enum class DualRecordingLayout(val label: String) {
    TWO_FILES("Two Separate MP4 Files (Master Backup)"),
    SIDE_BY_SIDE("Side by Side Composite"),
    PIP("Picture-in-Picture")
}

data class CameraResolution(
    val width: Int,
    val height: Int,
    val label: String,
    val aspectRatio: String = "16:9"
) {
    val size: Size get() = Size(width, height)
    val displayLabel: String get() = "${width}x${height} ($label)"
}

data class CameraSettings(
    val bitrateMbps: Int = 60, // 40, 50, 60, 70, 80, 90, 100
    val resolution: CameraResolution = CameraResolution(3840, 2160, "4K UHD"),
    val fps: Int = 30, // 24, 30, 60
    val use10BitHlg: Boolean = true,
    val leicaProfile: LeicaColorProfile = LeicaColorProfile.LEICA_HLG_BT2020,
    val vignetteCompensation: Float = 0.35f, // 0.0 to 1.0 (anti-perimeter darkness under torch)
    val centerTorchFill: Float = 0.25f, // 0.0 to 1.0 (torch hotspot flattening)
    val edgeModeOff: Boolean = true, // CONTROL_EDGE_MODE = OFF
    val noiseReductionOff: Boolean = true, // CONTROL_NOISE_REDUCTION_MODE = OFF
    val activeSensor: ActiveSensorMode = ActiveSensorMode.MAIN_IMX989,
    val dualRecordingLayout: DualRecordingLayout = DualRecordingLayout.TWO_FILES,
    val torchEnabled: Boolean = false,
    val showHistogram: Boolean = true,
    val thermalThrottlingActive: Boolean = false,
    val thermalStatusText: String = "NORMAL",
    // Low-Light Optimization for Poorly Lit Rooms
    val nightBoostEnabled: Boolean = false,
    val shadowLift: Float = 0.40f, // 0.0 to 1.0 (smooth low-light shadow curve boost)
    val exposureCompensationEv: Float = 0.5f, // -2.0 to +2.0 EV
    val isoLimit: Int = 3200, // Auto/Max ISO: 800, 1600, 3200, 6400, 12800
    val hyperOis: Boolean = true, // Xiaomi 12S Ultra HyperOIS optical stabilization
    val lowLightShutterPriority: Boolean = true // Slow shutter 1/24s for max photon integration
)

data class HistogramData(
    val luma: FloatArray = FloatArray(64),
    val r: FloatArray = FloatArray(64),
    val g: FloatArray = FloatArray(64),
    val b: FloatArray = FloatArray(64),
    val avgLuma: Float = 0.45f,
    val highlightClipPct: Float = 0.02f,
    val shadowCrushPct: Float = 0.01f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return luma.contentEquals(other.luma)
    }

    override fun hashCode(): Int {
        return luma.contentHashCode()
    }
}

data class RecordedClip(
    val id: String,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val durationSeconds: Long,
    val resolution: String,
    val fps: Int,
    val bitrateMbps: Int,
    val profile: String,
    val lensLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHlg10Bit: Boolean = true,
    val isSecondaryStream: Boolean = false
)
