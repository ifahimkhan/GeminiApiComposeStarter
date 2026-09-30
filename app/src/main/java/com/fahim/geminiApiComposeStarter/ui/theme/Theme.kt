package com.fahim.geminiApiComposeStarter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    // Main accent / send button
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),

    // User message bubble
    primaryContainer = Color(0xFF2F5D8A),
    onPrimaryContainer = Color(0xFFF8FAFC),

    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0F172A),

    // Gemini message bubble
    secondaryContainer = Color(0xFF24272D),
    onSecondaryContainer = Color(0xFFF1F3F5),

    // Main app background
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF4F6F8),

    surface = Color(0xFF0F1115),
    onSurface = Color(0xFFF4F6F8),

    // Prompt field / secondary controls
    surfaceVariant = Color(0xFF1C2026),
    onSurfaceVariant = Color(0xFFAEB6C2),

    outline = Color(0xFF3B424C)
)

private val LightColorScheme = lightColorScheme(
    // Main accent / send button
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),

    // User message bubble
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF172033),

    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFFFFFFFF),

    // Gemini message bubble
    secondaryContainer = Color(0xFFE7EBF0),
    onSecondaryContainer = Color(0xFF22252A),

    // Main app background
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF20242B),

    surface = Color(0xFFF7F8FA),
    onSurface = Color(0xFF20242B),

    // Prompt field / secondary controls
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF667085),

    outline = Color(0xFFC8D0DA)
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),

    // Kept supported for the assignment, but disabled by default
    // so our custom blue/charcoal palette is actually visible.
    dynamicColor: Boolean = false,

    content: @Composable () -> Unit
) {
    val colorScheme = when {

        dynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme ->
            DarkColorScheme

        else ->
            LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}