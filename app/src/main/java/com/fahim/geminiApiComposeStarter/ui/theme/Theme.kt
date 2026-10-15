package com.fahim.geminiApiComposeStarter.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = SlateGrey80,
    tertiary = Sky80,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = SlateGrey80,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = SlateGrey40,
    tertiary = Sky40,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = SlateGrey40,
)

@Composable
fun GeminiApiComposeStarterTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    },
    // Disable dynamic color so our new minimal palette is always applied
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}