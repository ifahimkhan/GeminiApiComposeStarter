package com.fahim.geminiApiComposeStarter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OrbitDarkColorScheme = darkColorScheme(
    primary = OrbitCyan,
    onPrimary = OrbitVoid,
    primaryContainer = OrbitSurfaceElevated,
    onPrimaryContainer = OrbitCyan,
    secondary = OrbitUltraviolet,
    onSecondary = OrbitTextPrimary,
    background = OrbitVoid,
    onBackground = OrbitTextPrimary,
    surface = OrbitSurface,
    onSurface = OrbitTextPrimary,
    surfaceVariant = OrbitSurfaceElevated,
    onSurfaceVariant = OrbitTextSecondary,
    outline = OrbitBorder
)

private val OrbitLightColorScheme = lightColorScheme(
    primary = OrbitLightPrimary,
    onPrimary = OrbitLightSurface,
    primaryContainer = OrbitLightElevated,
    onPrimaryContainer = OrbitLightPrimary,
    secondary = OrbitLightSecondary,
    onSecondary = OrbitLightSurface,
    background = OrbitLightBackground,
    onBackground = OrbitLightTextPrimary,
    surface = OrbitLightSurface,
    onSurface = OrbitLightTextPrimary,
    surfaceVariant = OrbitLightElevated,
    onSurfaceVariant = OrbitLightTextSecondary,
    outline = OrbitLightBorder
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = true, // Default to stunning Orbit dark void mode
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) OrbitDarkColorScheme else OrbitLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}