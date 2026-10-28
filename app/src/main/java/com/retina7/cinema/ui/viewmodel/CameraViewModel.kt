package com.retina7.cinema.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.retina7.cinema.camera.Camera2Manager
import com.retina7.cinema.camera.DualVideoRecorder
import com.retina7.cinema.data.model.*
import com.retina7.cinema.thermal.ThermalMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraManager = Camera2Manager(application)
    val thermalMonitor = ThermalMonitor(application)

    private val _settings = MutableStateFlow(CameraSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingTime = MutableStateFlow(0L)
    val recordingTime: StateFlow<Long> = _recordingTime.asStateFlow()

    private val _histogramData = MutableStateFlow(HistogramData())
    val histogramData: StateFlow<HistogramData> = _histogramData.asStateFlow()

    private val _recordedClips = MutableStateFlow<List<RecordedClip>>(emptyList())
    val recordedClips: StateFlow<List<RecordedClip>> = _recordedClips.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var simulatedHistogramJob: Job? = null

    init {
        thermalMonitor.startMonitoring()

        // Sync thermal state to settings
        viewModelScope.launch {
            thermalMonitor.thermalState.collect { thermal ->
                _settings.update {
                    it.copy(
                        thermalThrottlingActive = thermal.isThrottling,
                        thermalStatusText = thermal.statusLabel
                    )
                }
            }
        }

        // Generate dynamic histogram updates based on active Leica profile and vignette
        startHistogramSimulation()
    }

    private fun startHistogramSimulation() {
        simulatedHistogramJob = viewModelScope.launch {
            while (true) {
                delay(150)
                val profile = _settings.value.leicaProfile
                val luma = FloatArray(64)
                val r = FloatArray(64)
                val g = FloatArray(64)
                val b = FloatArray(64)

                val center = when (profile) {
                    LeicaColorProfile.LEICA_HLG_BT2020 -> 32 // Flatter midtone curve
                    LeicaColorProfile.LEICA_AUTHENTIC -> 28 // Organic shadow-biased
                    LeicaColorProfile.LEICA_VIBRANT -> 38 // Elevated midtones
                    LeicaColorProfile.LEICA_MONOCHROM -> 30
                    LeicaColorProfile.LEICA_MONOCHROM_HC -> 42 // Bimodal high contrast
                }
                val isNight = _settings.value.nightBoostEnabled
                val shadowLiftVal = if (isNight) _settings.value.shadowLift else 0f
                val effectiveCenter = (center + (if (isNight) 10 else 0)).coerceIn(10, 54)

                for (i in 0 until 64) {
                    val dist = kotlin.math.abs(i - effectiveCenter)
                    val value = kotlin.math.max(0.05f, 1.0f - (dist / 22.0f))
                    // Lift low-mids when night boost is on
                    val lowEndBoost = if (isNight && i < 30) (1.0f - i / 30f) * shadowLiftVal * 0.4f else 0f
                    luma[i] = (value + lowEndBoost) * (0.8f + (Math.random().toFloat() * 0.2f))
                    r[i] = luma[i] * 0.95f
                    g[i] = luma[i] * 1.0f
                    b[i] = luma[i] * 0.85f
                }

                _histogramData.value = HistogramData(
                    luma = luma,
                    r = r,
                    g = g,
                    b = b,
                    avgLuma = effectiveCenter / 64f,
                    highlightClipPct = if (profile == LeicaColorProfile.LEICA_HLG_BT2020) 0.01f else 0.03f,
                    shadowCrushPct = if (isNight) 0.005f else if (profile == LeicaColorProfile.LEICA_MONOCHROM_HC) 0.08f else 0.03f
                )
            }
        }
    }

    fun updateSettings(newSettings: CameraSettings) {
        _settings.value = newSettings
        cameraManager.updateLowLightSettings(newSettings)
    }

    fun toggleNightBoost() {
        val next = !_settings.value.nightBoostEnabled
        val updated = _settings.value.copy(
            nightBoostEnabled = next,
            exposureCompensationEv = if (next) 1.0f else 0.0f
        )
        _settings.value = updated
        cameraManager.updateLowLightSettings(updated)
    }

    fun toggleTorch() {
        val next = !_settings.value.torchEnabled
        _settings.update { it.copy(torchEnabled = next) }
        cameraManager.toggleTorch(next)
    }

    fun selectLens(mode: ActiveSensorMode) {
        _settings.update { it.copy(activeSensor = mode) }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        _isRecording.value = true
        _recordingTime.value = 0L

        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(1000)
                _recordingTime.update { it + 1 }
            }
        }
    }

    private fun stopRecording() {
        val duration = _recordingTime.value
        _isRecording.value = false
        recordingTimerJob?.cancel()

        val current = _settings.value
        val clipId = "clip_${System.currentTimeMillis()}"
        val primaryClip = RecordedClip(
            id = clipId,
            fileName = "CINEMA_${clipId.takeLast(6)}_MAIN_IMX989.mp4",
            filePath = "/storage/emulated/0/DCIM/Retina7/${clipId}_MAIN.mp4",
            sizeBytes = (current.bitrateMbps * 1_000_000L * duration) / 8,
            durationSeconds = duration,
            resolution = "${current.resolution.width}x${current.resolution.height}",
            fps = current.fps,
            bitrateMbps = if (current.thermalThrottlingActive) (current.bitrateMbps * 0.8f).toInt() else current.bitrateMbps,
            profile = current.leicaProfile.title,
            lensLabel = current.activeSensor.label,
            isHlg10Bit = current.use10BitHlg
        )

        val newClips = mutableListOf(primaryClip)

        // If in Dual Concurrent mode, also add the secondary backup file
        if (current.activeSensor == ActiveSensorMode.DUAL_CONCURRENT) {
            val secondaryClip = RecordedClip(
                id = "${clipId}_sec",
                fileName = "CINEMA_${clipId.takeLast(6)}_ULTRAWIDE_IMX586.mp4",
                filePath = "/storage/emulated/0/DCIM/Retina7/${clipId}_ULTRAWIDE.mp4",
                sizeBytes = (40 * 1_000_000L * duration) / 8,
                durationSeconds = duration,
                resolution = "1920x1080",
                fps = current.fps,
                bitrateMbps = 40,
                profile = "Backup Log",
                lensLabel = "Sony IMX586 Ultra-Wide",
                isHlg10Bit = true,
                isSecondaryStream = true
            )
            newClips.add(secondaryClip)
        }

        _recordedClips.update { newClips + it }
    }

    fun deleteClip(id: String) {
        _recordedClips.update { list -> list.filterNot { it.id == id } }
    }

    override fun onCleared() {
        super.onCleared()
        thermalMonitor.stopMonitoring()
        cameraManager.closeCamera()
        recordingTimerJob?.cancel()
        simulatedHistogramJob?.cancel()
    }
}
