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

// ── Dark palette ─────────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF4F4F4),
    onPrimary = Color.Black,
    secondary = Color(0xFFD4D4D4),
    tertiary = Color(0xFFBDBDBD),
    background = Color.Black,
    onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF191919),
    onSurface = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFF1B1B1B),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFF333333),
    primaryContainer = Color(0xFF253D56),
    onPrimaryContainer = Color(0xFFE8F0FE),
    errorContainer = Color(0xFF321C1C),
    onErrorContainer = Color(0xFFFFDAD6),
)

// ── Light palette ────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color.White,
    secondary = Color(0xFF555555),
    tertiary = Color(0xFF777777),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1F1F1F),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F),
    surfaceContainer = Color(0xFFF1F3F4),
    surfaceContainerHigh = Color(0xFFE8EAED),
    surfaceContainerHighest = Color(0xFFDEE2E6),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFE0E0E0),
    primaryContainer = Color(0xFFE3F2FD),
    onPrimaryContainer = Color(0xFF0D1B2A),
    errorContainer = Color(0xFFFFEDED),
    onErrorContainer = Color(0xFF8B0000),
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
