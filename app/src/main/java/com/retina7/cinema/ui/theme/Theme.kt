package com.retina7.cinema.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CinemaAmber,
    secondary = CinemaRed,
    tertiary = CinemaEmerald,
    background = CinemaBlack,
    surface = CinemaSurface,
    surfaceVariant = CinemaSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = CinemaBorder
)

@Composable
fun Retina7CinemaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
