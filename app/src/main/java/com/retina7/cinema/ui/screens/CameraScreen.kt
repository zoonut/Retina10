package com.retina7.cinema.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retina7.cinema.data.model.ActiveSensorMode
import com.retina7.cinema.ui.components.CameraHUD
import com.retina7.cinema.ui.components.GallerySheet
import com.retina7.cinema.ui.components.HistogramView
import com.retina7.cinema.ui.components.SettingsSheet
import com.retina7.cinema.ui.theme.CinemaAmber
import com.retina7.cinema.ui.theme.CinemaBlack
import com.retina7.cinema.ui.theme.CinemaRed
import com.retina7.cinema.ui.viewmodel.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isVertical = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val settings by viewModel.settings.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingTime by viewModel.recordingTime.collectAsState()
    val histogramData by viewModel.histogramData.collectAsState()
    val recordedClips by viewModel.recordedClips.collectAsState()
    val availableResolutions by viewModel.cameraManager.availableResolutions.collectAsState()

    var isSettingsOpen by remember { mutableStateOf(false) }
    var isGalleryOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .testTag("camera_screen_root")
    ) {
        // VIEWPORT PREVIEW (Aspect-Ratio preserved without distortion)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isVertical) PaddingValues(top = 48.dp, bottom = 120.dp) else PaddingValues(horizontal = 48.dp, vertical = 24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Main Sensor Frame
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(if (isVertical) 9f / 16f else 16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF121216))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Viewfinder Center Reticle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = settings.activeSensor.label,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = settings.leicaProfile.title,
                        color = CinemaAmber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ISP: EDGE OFF · NR OFF · BT.2020 HLG 10-BIT",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Dual Camera PiP Window when DUAL_CONCURRENT is selected
                if (settings.activeSensor == ActiveSensorMode.DUAL_CONCURRENT) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(width = 140.dp, height = 80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E1E24))
                            .border(1.5.dp, CinemaRed, RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = "IMX586 (0.5x)\nSEC STREAM",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // FLOATING HISTOGRAM (Top Left below status bar)
        if (settings.showHistogram) {
            HistogramView(
                data = histogramData,
                modifier = Modifier
                    .padding(top = 70.dp, start = 16.dp)
                    .width(180.dp)
                    .align(Alignment.TopStart)
            )
        }

        // FULL HUD OVERLAY
        CameraHUD(
            settings = settings,
            isRecording = isRecording,
            recordingTimeSeconds = recordingTime,
            isVertical = isVertical,
            onToggleRecord = { viewModel.toggleRecording() },
            onToggleTorch = { viewModel.toggleTorch() },
            onToggleNightBoost = { viewModel.toggleNightBoost() },
            onSelectLens = { viewModel.selectLens(it) },
            onOpenSettings = { isSettingsOpen = true },
            onOpenGallery = { isGalleryOpen = true }
        )

        // SETTINGS BOTTOM SHEET
        SettingsSheet(
            isOpen = isSettingsOpen,
            onDismiss = { isSettingsOpen = false },
            settings = settings,
            availableResolutions = availableResolutions,
            onUpdateSettings = { viewModel.updateSettings(it) }
        )

        // CLIPS GALLERY BOTTOM SHEET
        GallerySheet(
            isOpen = isGalleryOpen,
            onDismiss = { isGalleryOpen = false },
            clips = recordedClips,
            onDeleteClip = { viewModel.deleteClip(it) }
        )
    }
}
