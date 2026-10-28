package com.retina7.cinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
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
import com.retina7.cinema.data.model.RecordedClip
import com.retina7.cinema.ui.theme.CinemaAmber
import com.retina7.cinema.ui.theme.CinemaRed
import com.retina7.cinema.ui.theme.CinemaSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GallerySheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    clips: List<RecordedClip>,
    onDeleteClip: (String) -> Unit
) {
    if (!isOpen) return

    var selectedClip by remember(clips) { mutableStateOf(clips.firstOrNull()) }

    fun formatBytes(bytes: Long): String {
        val mb = bytes / (1024 * 1024.0)
        return "%.1f MB".format(mb)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("gallery_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = CinemaAmber)
                    Column {
                        Text(
                            text = "Recorded Cinema Clips",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${clips.size} clips · DaVinci Resolve Master Exports",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded clips yet.\nTap the red shutter button to record with HLG 10-bit.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.8f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        val isSelected = selectedClip?.id == clip.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF2E2616) else Color(0xFF1E1E23))
                                .border(
                                    1.dp,
                                    if (isSelected) CinemaAmber else Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedClip = clip }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = clip.fileName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (clip.isSecondaryStream) {
                                        Text(
                                            text = "BACKUP",
                                            color = CinemaAmber,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = clip.resolution,
                                        color = CinemaAmber,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("•", color = Color.Gray, fontSize = 9.sp)
                                    Text(
                                        text = "${clip.fps} fps",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("•", color = Color.Gray, fontSize = 9.sp)
                                    Text(
                                        text = "${clip.bitrateMbps} Mbps",
                                        color = CinemaRed,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text("•", color = Color.Gray, fontSize = 9.sp)
                                    Text(
                                        text = formatBytes(clip.sizeBytes),
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "${clip.lensLabel} · ${clip.profile}",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }

                            IconButton(onClick = { onDeleteClip(clip.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
