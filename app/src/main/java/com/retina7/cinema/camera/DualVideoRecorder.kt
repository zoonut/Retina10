package com.retina7.cinema.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
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

/**
 * DualVideoRecorder: Simultaneous concurrent recording for Xiaomi 12S Ultra
 * Dual Stream mode: Main (IMX989 1") + Ultra-Wide (IMX586) writing to two separate MP4 files.
 */
class DualVideoRecorder(
    private val context: Context,
    private val cameraManager: CameraManager
) {
    private val TAG = "DualVideoRecorder"

    private var primaryRecorder: MediaRecorder? = null
    private var secondaryRecorder: MediaRecorder? = null

    private var primaryCameraDevice: CameraDevice? = null
    private var secondaryCameraDevice: CameraDevice? = null

    private var primarySession: CameraCaptureSession? = null
    private var secondarySession: CameraCaptureSession? = null

    private var dualThread: HandlerThread? = null
    private var dualHandler: Handler? = null

    private val _isDualRecording = MutableStateFlow(false)
    val isDualRecording: StateFlow<Boolean> = _isDualRecording.asStateFlow()

    private var primaryOutputFile: File? = null
    private var secondaryOutputFile: File? = null

    init {
        dualThread = HandlerThread("DualCameraThread").apply {
            start()
            dualHandler = Handler(looper)
        }
    }

    /**
     * Checks if the device hardware supports concurrent dual streams
     */
    fun isConcurrentSupported(mainId: String, ultraWideId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        return try {
            val concurrentSets = cameraManager.concurrentCameraIds
            concurrentSets.any { set ->
                set.contains(mainId) && set.contains(ultraWideId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking concurrent camera IDs: ${e.message}")
            false
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun startDualRecording(
        mainCameraId: String,
        ultraWideCameraId: String,
        mainPreviewSurface: Surface,
        secondaryPreviewSurface: Surface,
        settings: CameraSettings,
        storageDir: File
    ): Pair<File, File>? = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file1 = File(storageDir, "CINEMA_${timeStamp}_MAIN_IMX989.mp4")
            val file2 = File(storageDir, "CINEMA_${timeStamp}_ULTRAWIDE_IMX586.mp4")

            primaryOutputFile = file1
            secondaryOutputFile = file2

            // 1. Configure Primary Recorder (IMX989 1")
            primaryRecorder = createRecorder(file1, settings, isSecondary = false)
            val primaryRecordSurface = primaryRecorder!!.surface

            // 2. Configure Secondary Recorder (IMX586 Ultra-Wide)
            secondaryRecorder = createRecorder(file2, settings, isSecondary = true)
            val secondaryRecordSurface = secondaryRecorder!!.surface

            // 3. Open Main Camera Device
            cameraManager.openCamera(mainCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    primaryCameraDevice = camera
                    val surfaces = listOf(mainPreviewSurface, primaryRecordSurface)
                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                        addTarget(mainPreviewSurface)
                        addTarget(primaryRecordSurface)
                        set(CaptureRequest.CONTROL_EDGE_MODE, CaptureRequest.CONTROL_EDGE_MODE_OFF)
                        set(CaptureRequest.CONTROL_NOISE_REDUCTION_MODE, CaptureRequest.CONTROL_NOISE_REDUCTION_MODE_OFF)
                        if (settings.torchEnabled) {
                            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
                        }
                    }
                    camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            primarySession = session
                            session.setRepeatingRequest(request.build(), null, dualHandler)
                            primaryRecorder?.start()
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Primary session failed")
                        }
                    }, dualHandler)
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) { camera.close() }
            }, dualHandler)

            // 4. Open Secondary Camera Device
            cameraManager.openCamera(ultraWideCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    secondaryCameraDevice = camera
                    val surfaces = listOf(secondaryPreviewSurface, secondaryRecordSurface)
                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                        addTarget(secondaryPreviewSurface)
                        addTarget(secondaryRecordSurface)
                        set(CaptureRequest.CONTROL_EDGE_MODE, CaptureRequest.CONTROL_EDGE_MODE_OFF)
                        set(CaptureRequest.CONTROL_NOISE_REDUCTION_MODE, CaptureRequest.CONTROL_NOISE_REDUCTION_MODE_OFF)
                    }
                    camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            secondarySession = session
                            session.setRepeatingRequest(request.build(), null, dualHandler)
                            secondaryRecorder?.start()
                            _isDualRecording.value = true
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(TAG, "Secondary session failed")
                        }
                    }, dualHandler)
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) { camera.close() }
            }, dualHandler)

            Pair(file1, file2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed dual recording initialization: ${e.message}", e)
            null
        }
    }

    private fun createRecorder(
        outputFile: File,
        settings: CameraSettings,
        isSecondary: Boolean
    ): MediaRecorder {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            if (!isSecondary) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(outputFile.absolutePath)

            val bitrate = if (isSecondary) {
                // Secondary stream at 40 Mbps to prevent overheating
                40 * 1_000_000
            } else {
                settings.bitrateMbps * 1_000_000
            }

            setVideoEncodingBitRate(bitrate)
            setVideoFrameRate(settings.fps)
            setVideoSize(
                if (isSecondary) 1920 else settings.resolution.width,
                if (isSecondary) 1080 else settings.resolution.height
            )
            setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)

            if (!isSecondary) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(384_000)
                setAudioSamplingRate(48_000)
            }
            prepare()
        }
        return recorder
    }

    fun stopDualRecording(): Pair<File?, File?> {
        try {
            primaryRecorder?.stop()
            primaryRecorder?.reset()
            primaryRecorder?.release()
            primaryRecorder = null

            secondaryRecorder?.stop()
            secondaryRecorder?.reset()
            secondaryRecorder?.release()
            secondaryRecorder = null

            primarySession?.close()
            primarySession = null
            secondarySession?.close()
            secondarySession = null

            primaryCameraDevice?.close()
            primaryCameraDevice = null
            secondaryCameraDevice?.close()
            secondaryCameraDevice = null

            _isDualRecording.value = false
            return Pair(primaryOutputFile, secondaryOutputFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping dual recording: ${e.message}")
            _isDualRecording.value = false
            return Pair(null, null)
        }
    }

    fun release() {
        dualThread?.quitSafely()
        try {
            dualThread?.join()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing dual thread", e)
        }
    }
}
