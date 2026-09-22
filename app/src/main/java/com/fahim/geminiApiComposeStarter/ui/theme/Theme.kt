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
    primary = Purple80,
    onPrimary = Purple30,
    primaryContainer = PurpleContainerDark,
    onPrimaryContainer = Purple90,
    secondary = Pink80,
    onSecondary = Pink30,
    secondaryContainer = PinkContainerDark,
    onSecondaryContainer = Pink90,
    tertiary = Orchid80,
    background = PlumBackground,
    onBackground = LilacOnSurface,
    surface = PlumBackground,
    onSurface = LilacOnSurface,
    surfaceVariant = PlumSurfaceVariant,
    onSurfaceVariant = LilacOnSurfaceVariant,
    outline = LilacOutline,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    secondary = Pink40,
    onSecondary = Color.White,
    secondaryContainer = Pink90,
    onSecondaryContainer = Pink10,
    tertiary = Orchid40,
    background = BlushBackground,
    onBackground = PlumOnSurface,
    surface = BlushBackground,
    onSurface = PlumOnSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onSurfaceVariant = PlumOnSurface,
    outline = PlumOutline,
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
    // Off by default so the app keeps its pink/purple identity; set true for Material You wallpaper colours (Android 12+).
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
