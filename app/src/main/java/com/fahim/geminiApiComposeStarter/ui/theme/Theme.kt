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
import com.fahim.geminiApiComposeStarter.data.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = BlueContainerDark,
    onPrimaryContainer = Blue90,
    secondary = SkyBlue80,
    onSecondary = SkyBlue20,
    secondaryContainer = DeepBlueContainerDark,
    onSecondaryContainer = Blue90,
    tertiary = Cyan80,
    background = NightBackground,
    onBackground = MistOnSurface,
    surface = NightBackground,
    onSurface = MistOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = MistOnSurfaceVariant,
    outline = FogOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = DeepBlue40,
    onSecondary = Color.White,
    secondaryContainer = SkyBlue90,
    onSecondaryContainer = SkyBlue10,
    tertiary = Cyan40,
    background = CloudBackground,
    onBackground = InkOnSurface,
    surface = CloudBackground,
    onSurface = InkOnSurface,
    surfaceVariant = CloudSurfaceVariant,
    onSurfaceVariant = InkOnSurfaceVariant,
    outline = SlateOutline,
)

/** Resolves the user's [ThemeMode] choice to a concrete light/dark decision. */
@Composable
fun ThemeMode.useDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GeminiApiComposeStarterTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Off by default so the app keeps its blue identity; set true for Material You wallpaper colours (Android 12+).
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.useDarkTheme()
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
