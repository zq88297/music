package com.example.rearlyrics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RearLyricsColors = darkColorScheme(
    primary = Color(0xFFE9587B),
    secondary = Color(0xFF58C8E9),
    tertiary = Color(0xFFF9B44D),
    surface = Color(0xFFF9F7FC),
    onSurface = Color(0xFF201926),
    onSurfaceVariant = Color(0xFF655C70),
)

@Composable
fun RearLyricsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RearLyricsColors,
        content = content,
    )
}
