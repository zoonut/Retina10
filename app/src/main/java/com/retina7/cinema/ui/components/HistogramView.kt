package com.retina7.cinema.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.retina7.cinema.data.model.HistogramData

@Composable
fun HistogramView(
    data: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("cinema_histogram_view")
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                val w = size.width
                val h = size.height
                val bins = data.luma.size
                val step = w / bins.toFloat()

                // Draw Luma curve
                val lumaPath = Path().apply {
                    moveTo(0f, h)
                    for (i in 0 until bins) {
                        val v = data.luma[i].coerceIn(0f, 1f)
                        val y = h - (v * h)
                        lineTo(i * step, y)
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(lumaPath, Color.White.copy(alpha = 0.35f))

                // Draw RGB curves
                val rPath = Path().apply {
                    moveTo(0f, h)
                    for (i in 0 until bins) {
                        val y = h - (data.r[i].coerceIn(0f, 1f) * h)
                        lineTo(i * step, y)
                    }
                }
                drawPath(rPath, Color.Red.copy(alpha = 0.5f))

                val gPath = Path().apply {
                    moveTo(0f, h)
                    for (i in 0 until bins) {
                        val y = h - (data.g[i].coerceIn(0f, 1f) * h)
                        lineTo(i * step, y)
                    }
                }
                drawPath(gPath, Color.Green.copy(alpha = 0.5f))

                val bPath = Path().apply {
                    moveTo(0f, h)
                    for (i in 0 until bins) {
                        val y = h - (data.b[i].coerceIn(0f, 1f) * h)
                        lineTo(i * step, y)
                    }
                }
                drawPath(bPath, Color.Cyan.copy(alpha = 0.5f))
            }

            // Clip statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRUSH: ${(data.shadowCrushPct * 100).toInt()}%",
                    color = if (data.shadowCrushPct > 0.05f) Color(0xFFEF4444) else Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "AVG LUMA: ${(data.avgLuma * 100).toInt()}%",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "CLIP: ${(data.highlightClipPct * 100).toInt()}%",
                    color = if (data.highlightClipPct > 0.05f) Color(0xFFEF4444) else Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
