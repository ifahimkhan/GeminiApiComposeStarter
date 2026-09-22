package com.fahim.geminiApiComposeStarter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Custom Gemini colour schemes (fallback for pre-Android 12) ──

private val DarkColorScheme = darkColorScheme(
    primary          = GeminiBlueDark,
    secondary        = GeminiPurpleDark,
    tertiary         = GeminiTealDark,
    background       = NeutralDark,
    surface          = SurfaceDark,
    onPrimary        = NeutralDark,
    onSecondary      = NeutralDark,
    onBackground     = GeminiBlueDark,
    onSurface        = androidx.compose.ui.graphics.Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary          = GeminiBlue,
    secondary        = GeminiPurple,
    tertiary         = GeminiTeal,
    primaryContainer = GeminiBlueTonal,
    background       = NeutralLight,
    surface          = SurfaceLight,
)

/**
 * App theme supporting:
 *  - Material 3 dynamic colour on Android 12+ (uses wallpaper colours)
 *  - Custom Gemini-inspired fallback palette on older devices
 *  - Automatic dark / light mode via [isSystemInDarkTheme]
 */
@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,    // Material You — Android 12+
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar colour to match the surface colour
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}