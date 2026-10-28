package com.retina7.cinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retina7.cinema.data.model.CameraResolution
import com.retina7.cinema.data.model.CameraSettings
import com.retina7.cinema.data.model.DualRecordingLayout
import com.retina7.cinema.data.model.LeicaColorProfile
import com.retina7.cinema.ui.theme.CinemaAmber
import com.retina7.cinema.ui.theme.CinemaRed
import com.retina7.cinema.ui.theme.CinemaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    settings: CameraSettings,
    availableResolutions: List<CameraResolution>,
    onUpdateSettings: (CameraSettings) -> Unit
) {
    if (!isOpen) return

    val bitrateOptions = listOf(40, 50, 60, 70, 80, 90, 100)
    val fpsOptions = listOf(24, 30, 60)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("settings_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Retina 7 Cinema Settings",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Calibrated for Xiaomi 12S Ultra · IMX989 & IMX586",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxHeight(0.85f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECTION: BITRATE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TARGET HEVC BITRATE",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bitrateOptions.forEach { mbps ->
                                val selected = settings.bitrateMbps == mbps
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) CinemaAmber else Color(0xFF26262B))
                                        .clickable { onUpdateSettings(settings.copy(bitrateMbps = mbps)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mbps}M",
                                        color = if (selected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: RESOLUTION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SUPPORTED SENSOR RESOLUTIONS",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableResolutions.forEach { res ->
                                val selected = settings.resolution == res
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF2E2616) else Color(0xFF202025))
                                        .border(
                                            1.dp,
                                            if (selected) CinemaAmber else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onUpdateSettings(settings.copy(resolution = res)) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${res.width} × ${res.height}",
                                        color = if (selected) CinemaAmber else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "${res.label} (${res.aspectRatio})",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: FPS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FRAME RATE",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fpsOptions.forEach { fps ->
                                val selected = settings.fps == fps
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) CinemaAmber else Color(0xFF26262B))
                                        .clickable { onUpdateSettings(settings.copy(fps = fps)) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$fps fps",
                                        color = if (selected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: LEICA COLOR SCIENCE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "LEICA COLOR PROFILES",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LeicaColorProfile.values().forEach { profile ->
                                val selected = settings.leicaProfile == profile
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFF2E2616) else Color(0xFF202025))
                                        .border(
                                            1.dp,
                                            if (selected) CinemaAmber else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onUpdateSettings(settings.copy(leicaProfile = profile)) }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = profile.title,
                                        color = if (selected) CinemaAmber else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = profile.description,
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION: LOW-LIGHT & POORLY LIT ROOM OPTIMIZATION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "POORLY LIT ROOM & LOW-LIGHT BOOST",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Night Boost Master Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (settings.nightBoostEnabled) Color(0xFF2E2616) else Color(0xFF202025))
                                .border(1.dp, if (settings.nightBoostEnabled) CinemaAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Night Low-Light Boost",
                                    color = if (settings.nightBoostEnabled) CinemaAmber else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Quad-Bayer 4-in-1 binning + EV bias for dimly lit interior scenes.",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.nightBoostEnabled,
                                onCheckedChange = { onUpdateSettings(settings.copy(nightBoostEnabled = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber, checkedTrackColor = CinemaAmber.copy(alpha = 0.5f))
                            )
                        }

                        // Shadow Detail Lift Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Low-Light Shadow Detail Recovery", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "${(settings.shadowLift * 100).toInt()}%",
                                    color = CinemaAmber,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = settings.shadowLift,
                                onValueChange = { onUpdateSettings(settings.copy(shadowLift = it)) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = CinemaAmber, activeTrackColor = CinemaAmber)
                            )
                        }

                        // Exposure Compensation Bias (EV)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "EXPOSURE BIAS (EV GAIN)",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.0f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { ev ->
                                    val isSelected = settings.exposureCompensationEv == ev
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) CinemaAmber else Color(0xFF26262B))
                                            .clickable { onUpdateSettings(settings.copy(exposureCompensationEv = ev)) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${ev}EV",
                                            color = if (isSelected) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // HyperOIS Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("HyperOIS Optical Stabilization", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "Steadies Sony IMX989 sensor to prevent blur during slow shutter low-light capture.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.hyperOis,
                                onCheckedChange = { onUpdateSettings(settings.copy(hyperOis = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber, checkedTrackColor = CinemaAmber.copy(alpha = 0.5f))
                            )
                        }

                        // Low Light 24 fps slow shutter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Slow Shutter 24 FPS Priority", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "Allows 1/24s exposure time (41.6ms) for 2.5x more light absorption per frame.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.lowLightShutterPriority,
                                onCheckedChange = { onUpdateSettings(settings.copy(lowLightShutterPriority = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber, checkedTrackColor = CinemaAmber.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // SECTION: OPTICAL & TORCH COMPENSATION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "OPTICAL & TORCH COMPENSATION",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Flat-Field Anti-Vignette", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "${(settings.vignetteCompensation * 100).toInt()}%",
                                    color = CinemaAmber,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = settings.vignetteCompensation,
                                onValueChange = { onUpdateSettings(settings.copy(vignetteCompensation = it)) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = CinemaAmber, activeTrackColor = CinemaAmber)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Center Torch Hotspot Flattening", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "${(settings.centerTorchFill * 100).toInt()}%",
                                    color = CinemaAmber,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = settings.centerTorchFill,
                                onValueChange = { onUpdateSettings(settings.copy(centerTorchFill = it)) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = CinemaAmber, activeTrackColor = CinemaAmber)
                            )
                        }
                    }
                }

                // SECTION: ISP CONTROLS (CONTROL_EDGE_MODE / CONTROL_NOISE_REDUCTION_MODE)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DAVINCI RESOLVE ISP OPTIMIZATIONS",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CONTROL_EDGE_MODE = OFF", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "Disables synthetic digital sharpening to preserve organic film grain.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.edgeModeOff,
                                onCheckedChange = { onUpdateSettings(settings.copy(edgeModeOff = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber, checkedTrackColor = CinemaAmber.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("CONTROL_NOISE_REDUCTION = OFF", color = Color.White, fontSize = 13.sp)
                                Text(
                                    "Prevents ISP smudging, lowers thermal load, enables post-denoising.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = settings.noiseReductionOff,
                                onCheckedChange = { onUpdateSettings(settings.copy(noiseReductionOff = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CinemaAmber, checkedTrackColor = CinemaAmber.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // SECTION: DUAL RECORDING LAYOUT
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "DUAL CONCURRENT STREAM MODE",
                            color = CinemaAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        DualRecordingLayout.values().forEach { layout ->
                            val selected = settings.dualRecordingLayout == layout
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0xFF2E2616) else Color(0xFF202025))
                                    .border(1.dp, if (selected) CinemaAmber else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { onUpdateSettings(settings.copy(dualRecordingLayout = layout)) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(layout.label, color = if (selected) CinemaAmber else Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
