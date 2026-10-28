package com.retina7.cinema.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retina7.cinema.data.model.ActiveSensorMode
import com.retina7.cinema.data.model.CameraSettings
import com.retina7.cinema.ui.theme.CinemaAmber
import com.retina7.cinema.ui.theme.CinemaRed

@Composable
fun CameraHUD(
    settings: CameraSettings,
    isRecording: Boolean,
    recordingTimeSeconds: Long,
    isVertical: Boolean,
    onToggleRecord: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleNightBoost: () -> Unit = {},
    onSelectLens: (ActiveSensorMode) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Invert displayed dimension labels when phone is held vertical per requirement
    val rawW = settings.resolution.width
    val rawH = settings.resolution.height
    val displayedWidth = if (isVertical) rawH else rawW
    val displayedHeight = if (isVertical) rawW else rawH
    val aspectLabel = if (isVertical) "9:16" else "16:9"

    fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%02d:%02d".format(m, s)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // TOP STATUS BAR
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolution & Codec Badges
                Row(
                    modifier = Modifier
                        .testTag("status_badge_row")
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayedWidth}×$displayedHeight",
                        color = CinemaAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text(aspectLabel, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text("${settings.fps}P", color = Color(0xFF10B981), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        "${settings.bitrateMbps}M",
                        color = if (settings.thermalThrottlingActive) Color.Yellow else CinemaRed,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("•", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        if (settings.use10BitHlg) "HLG 10b" else "HEVC",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (settings.nightBoostEnabled) {
                        Text("•", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            "NIGHT +${settings.exposureCompensationEv}EV",
                            color = CinemaAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Top Right: Night Boost, Torch, Gallery, Settings
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleNightBoost,
                        modifier = Modifier
                            .testTag("night_boost_button")
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (settings.nightBoostEnabled) CinemaAmber else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, if (settings.nightBoostEnabled) CinemaAmber else Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Night Low Light Boost",
                            tint = if (settings.nightBoostEnabled) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleTorch,
                        modifier = Modifier
                            .testTag("torch_toggle_button")
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (settings.torchEnabled) CinemaAmber else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (settings.torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch lock",
                            tint = if (settings.torchEnabled) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenGallery,
                        modifier = Modifier
                            .testTag("gallery_button")
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Clips Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .testTag("settings_button")
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cinema Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Recording Timer Badge
            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .testTag("recording_timer_badge")
                        .clip(RoundedCornerShape(16.dp))
                        .background(CinemaRed.copy(alpha = 0.9f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Text(
                        text = "REC ${formatTime(recordingTimeSeconds)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Thermal Warning Banner
            AnimatedVisibility(
                visible = settings.thermalThrottlingActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .testTag("thermal_warning_banner")
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Thermal Warning",
                        tint = Color.Yellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "THERMAL THROTTLING (-20% BITRATE)",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // BOTTOM CONTROLS: Lens selection & Shutter Trigger
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Lens Switcher Pill
            Row(
                modifier = Modifier
                    .testTag("lens_switcher_row")
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActiveSensorMode.values().forEach { mode ->
                    val isSelected = settings.activeSensor == mode
                    Box(
                        modifier = Modifier
                            .testTag("lens_option_${mode.name.lowercase()}")
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    isSelected && mode == ActiveSensorMode.DUAL_CONCURRENT -> CinemaRed
                                    isSelected -> CinemaAmber
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelectLens(mode) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode.zoomLabel,
                            color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Cinema Shutter Button
            Box(
                modifier = Modifier
                    .testTag("shutter_record_button")
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(4.dp, if (isRecording) CinemaRed else Color.White, CircleShape)
                    .clickable { onToggleRecord() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isRecording) 28.dp else 52.dp)
                        .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                        .background(CinemaRed)
                )
            }
        }
    }
}
