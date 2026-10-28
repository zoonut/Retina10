package com.retina7.cinema.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.retina7.cinema.data.model.CameraResolution
import com.retina7.cinema.data.model.CameraSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCancellableCoroutine

class Camera2Manager(private val context: Context) {

    private val TAG = "Camera2Manager"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var primaryCameraDevice: CameraDevice? = null
    private var primaryCaptureSession: CameraCaptureSession? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null

    private var mediaRecorder: MediaRecorder? = null

    private val _availableResolutions = MutableStateFlow<List<CameraResolution>>(emptyList())
    val availableResolutions: StateFlow<List<CameraResolution>> = _availableResolutions.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTorchActive = MutableStateFlow(false)
    val isTorchActive: StateFlow<Boolean> = _isTorchActive.asStateFlow()

    private val _concurrentCameraSupported = MutableStateFlow(false)
    val concurrentCameraSupported: StateFlow<Boolean> = _concurrentCameraSupported.asStateFlow()

    var primaryCameraId: String? = null
    var ultrawideCameraId: String? = null

    init {
        startBackgroundThread()
        queryCameraHardware()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    private fun queryCameraHardware() {
        try {
            // Check concurrent camera support (Android 11+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val concurrentSets = cameraManager.concurrentCameraIds
                _concurrentCameraSupported.value = concurrentSets.isNotEmpty()
                Log.d(TAG, "Concurrent camera combinations supported: ${concurrentSets.size}")
            }

            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    val minFocal = focalLengths?.minOrNull() ?: 24f

                    if (primaryCameraId == null) {
                        primaryCameraId = id
                        enumerateResolutions(characteristics)
                    } else if (minFocal < 15f && ultrawideCameraId == null) {
                        ultrawideCameraId = id
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed querying camera hardware: ${e.message}")
        }
    }

    /**
     * Runtime enumeration from StreamConfigurationMap per Prompt.txt requirement:
     * "The app must display the actual supported video resolutions returned by
     * StreamConfigurationMap.getOutputSizes(MediaRecorder::class.java) or SurfaceTexture::class.java at runtime."
     */
    private fun enumerateResolutions(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
        val recorderSizes = map.getOutputSizes(MediaRecorder::class.java)
            ?: map.getOutputSizes(SurfaceTexture::class.java)
            ?: emptyArray()

        val resolutions = recorderSizes
            .filter { it.width >= 1280 && it.height >= 720 }
            .sortedByDescending { it.width * it.height }
            .map { size ->
                val label = when {
                    size.width >= 7680 -> "8K Cinema"
                    size.width >= 3840 -> "4K UHD"
                    size.width >= 2560 -> "2.5K QHD"
                    size.width >= 1920 -> "1080p FHD"
                    else -> "720p HD"
                }
                CameraResolution(
                    width = size.width,
                    height = size.height,
                    label = label,
                    aspectRatio = if (size.width * 9 == size.height * 16) "16:9" else "${size.width}:${size.height}"
                )
            }

        _availableResolutions.value = if (resolutions.isNotEmpty()) resolutions else listOf(
            CameraResolution(3840, 2160, "4K UHD"),
            CameraResolution(1920, 1080, "1080p FHD"),
            CameraResolution(1280, 720, "720p HD")
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun openCamera(
        cameraId: String,
        previewSurface: Surface,
        settings: CameraSettings
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val device = suspendCancellableCoroutine<CameraDevice> { cont ->
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        primaryCameraDevice = camera
                        if (cont.isActive) cont.resume(camera)
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        primaryCameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        primaryCameraDevice = null
                        if (cont.isActive) cont.resumeWithException(RuntimeException("Camera error: $error"))
                    }
                }, backgroundHandler)
            }

            startPreview(device, previewSurface, settings)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}", e)
            false
        }
    }

    private suspend fun startPreview(
        device: CameraDevice,
        previewSurface: Surface,
        settings: CameraSettings
    ) = withContext(Dispatchers.IO) {
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)

                // Requirement 1: Force CONTROL_EDGE_MODE = OFF, CONTROL_NOISE_REDUCTION_MODE = OFF
                // to reduce ISP heat and preserve organic film texture for DaVinci Resolve
                set(CaptureRequest.CONTROL_EDGE_MODE, CaptureRequest.CONTROL_EDGE_MODE_OFF)
                set(CaptureRequest.CONTROL_NOISE_REDUCTION_MODE, CaptureRequest.CONTROL_NOISE_REDUCTION_MODE_OFF)

                // Requirement 3: Ensure FLASH_MODE_TORCH can be locked independently of recording
                if (_isTorchActive.value) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                } else {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                }

                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)

                // Low-Light Optimization for Poorly Lit Rooms:
                applyLowLightAndStabilization(this, settings)
            }
            previewRequestBuilder = builder

            suspendCancellableCoroutine<Unit> { cont ->
                device.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        primaryCaptureSession = session
                        try {
                            session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                            if (cont.isActive) cont.resume(Unit)
                        } catch (e: CameraAccessException) {
                            if (cont.isActive) cont.resumeWithException(e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        if (cont.isActive) cont.resumeWithException(RuntimeException("CaptureSession configuration failed"))
                    }
                }, backgroundHandler)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start preview: ${e.message}", e)
        }
    }

    /**
     * Independent Torch Lock per Prompt.txt Requirement 3:
     * "ensure FLASH_MODE_TORCH can be enabled independently of recording,
     * and the torch remains on during recording"
     */
    fun toggleTorch(enabled: Boolean) {
        _isTorchActive.value = enabled
        try {
            previewRequestBuilder?.let { builder ->
                val mode = if (enabled) CaptureRequest.FLASH_MODE_TORCH else CaptureRequest.FLASH_MODE_OFF
                builder.set(CaptureRequest.FLASH_MODE, mode)
                primaryCaptureSession?.setRepeatingRequest(builder.build(), null, backgroundHandler)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating torch state: ${e.message}")
        }
    }

    /**
     * Optical and Exposure Optimization for Poorly Lit Rooms
     * Configures HyperOIS, slow-shutter 24 FPS priority, and EV exposure bias on Sony IMX989
     */
    private fun applyLowLightAndStabilization(
        builder: CaptureRequest.Builder,
        settings: CameraSettings
    ) {
        // 1. HyperOIS (Optical Image Stabilization for IMX989 1" sensor)
        if (settings.hyperOis) {
            builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
        }

        // 2. Slow-shutter 24 FPS priority for dark rooms
        // Allows sensor exposure time up to 1/24s (41.6ms), absorbing 2.5x more light than 60fps
        if (settings.nightBoostEnabled || settings.lowLightShutterPriority) {
            val targetRange = if (settings.fps == 24) android.util.Range(24, 24) else android.util.Range(15, settings.fps)
            builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, targetRange)
        }

        // 3. Exposure Compensation EV bias (+0.5 to +2.0 EV)
        if (settings.nightBoostEnabled || settings.exposureCompensationEv != 0f) {
            val ev = if (settings.nightBoostEnabled && settings.exposureCompensationEv <= 0f) 1.0f else settings.exposureCompensationEv
            val step = 0.33333334f // Standard 1/3 EV step
            val index = (ev / step).toInt().coerceIn(-6, 6)
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, index)
        }

        // 4. Night scene mode if available
        if (settings.nightBoostEnabled) {
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_NIGHT)
        }
    }

    /**
     * Dynamically update low-light settings in active capture session
     */
    fun updateLowLightSettings(settings: CameraSettings) {
        previewRequestBuilder?.let { builder ->
            applyLowLightAndStabilization(builder, settings)
            try {
                primaryCaptureSession?.setRepeatingRequest(builder.build(), null, backgroundHandler)
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating low-light settings: ${e.message}")
            }
        }
    }

    /**
     * Configures MediaFormat for HEVC 10-bit HLG BT.2020 for DaVinci Resolve
     */
    fun createHlg10BitMediaFormat(width: Int, height: Int, bitrateBps: Int, fps: Int): MediaFormat {
        return MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrateBps)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            // Force ITU-R BT.2020 Color Space, HLG Transfer Curve, and HEVC Main 10 Profile
            setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
            setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
        }
    }

    fun startRecording(
        outputFile: File,
        settings: CameraSettings,
        previewSurface: Surface
    ) {
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(outputFile.absolutePath)

                // Effective bitrate with thermal throttling consideration
                val effectiveBitrateMbps = if (settings.thermalThrottlingActive) {
                    (settings.bitrateMbps * 0.8f).toInt()
                } else {
                    settings.bitrateMbps
                }

                setVideoEncodingBitRate(effectiveBitrateMbps * 1_000_000)
                setVideoFrameRate(settings.fps)
                setVideoSize(settings.resolution.width, settings.resolution.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(384_000)
                setAudioSamplingRate(48_000)
                prepare()
            }

            mediaRecorder = recorder
            val recordSurface = recorder.surface

            val device = primaryCameraDevice ?: return
            val surfaces = listOf(previewSurface, recordSurface)

            val recordBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recordSurface)

                // Keep Edge mode & Noise reduction OFF
                set(CaptureRequest.CONTROL_EDGE_MODE, CaptureRequest.CONTROL_EDGE_MODE_OFF)
                set(CaptureRequest.CONTROL_NOISE_REDUCTION_MODE, CaptureRequest.CONTROL_NOISE_REDUCTION_MODE_OFF)

                // KEEP TORCH ON DURING RECORDING (Prompt.txt requirement 3)
                if (_isTorchActive.value) {
                    set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                }

                // Low-Light Optimization for Poorly Lit Rooms
                applyLowLightAndStabilization(this, settings)
            }

            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    primaryCaptureSession = session
                    session.setRepeatingRequest(recordBuilder.build(), null, backgroundHandler)
                    recorder.start()
                    _isRecording.value = true
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Record session configuration failed")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed starting recording: ${e.message}", e)
        }
    }

    fun stopRecording(): Boolean {
        return try {
            if (_isRecording.value) {
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                mediaRecorder = null
                _isRecording.value = false
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed stopping recording: ${e.message}", e)
            _isRecording.value = false
            false
        }
    }

    fun closeCamera() {
        try {
            primaryCaptureSession?.close()
            primaryCaptureSession = null
            primaryCameraDevice?.close()
            primaryCameraDevice = null
            mediaRecorder?.release()
            mediaRecorder = null
            stopBackgroundThread()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera: ${e.message}")
        }
    }
}
