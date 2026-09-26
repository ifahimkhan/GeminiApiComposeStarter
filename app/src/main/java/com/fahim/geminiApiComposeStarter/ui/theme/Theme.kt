package com.fahim.geminiApiComposeStarter.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ElectricBlue = Color(0xFF087BFF)
val MutedText = Color(0xFF949BAE)
val DockColor = Color(0xFF1B2236)
private val ReferenceColors = darkColorScheme(
    primary = ElectricBlue, onPrimary = Color.White,
    background = Color(0xFF020509), onBackground = Color(0xFFF5F7FF),
    surface = DockColor, onSurface = Color(0xFFF5F7FF),
    surfaceVariant = Color(0xFF2B334C), onSurfaceVariant = MutedText,
    error = Color(0xFFFFA8AC), outline = Color(0xFF535D71)
)
/** Fixed palette intentionally preserves the supplied video on all wallpaper/theme settings. */
@Composable
fun GeminiApiComposeStarterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ReferenceColors, typography = Typography(), content = content)
}
