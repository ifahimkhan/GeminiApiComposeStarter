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

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPurplePrimaryDark,
    onPrimary = IndigoPurpleOnPrimaryDark,
    primaryContainer = IndigoPurplePrimaryContainerDark,
    onPrimaryContainer = IndigoPurpleOnPrimaryContainerDark,
    secondary = IndigoPurpleSecondaryDark,
    onSecondary = IndigoPurpleOnSecondaryDark,
    secondaryContainer = IndigoPurpleSecondaryContainerDark,
    onSecondaryContainer = IndigoPurpleOnSecondaryContainerDark,
    tertiary = IndigoPurpleTertiaryDark,
    onTertiary = IndigoPurpleOnTertiaryDark,
    surface = IndigoPurpleSurfaceDark,
    onSurface = IndigoPurpleOnSurfaceDark,
    surfaceVariant = IndigoPurpleSurfaceVariantDark,
    onSurfaceVariant = IndigoPurpleOnSurfaceVariantDark,
    surfaceContainer = IndigoPurpleSurfaceContainerDark,
    surfaceContainerLow = IndigoPurpleSurfaceContainerLowDark,
    surfaceContainerHigh = IndigoPurpleSurfaceContainerHighDark,
    outline = IndigoPurpleOutlineDark,
    outlineVariant = IndigoPurpleOutlineVariantDark,
    error = IndigoPurpleErrorDark,
    errorContainer = IndigoPurpleErrorContainerDark,
    onErrorContainer = IndigoPurpleOnErrorContainerDark,
    background = IndigoPurpleSurfaceDark,
    onBackground = IndigoPurpleOnSurfaceDark,
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPurplePrimaryLight,
    onPrimary = IndigoPurpleOnPrimaryLight,
    primaryContainer = IndigoPurplePrimaryContainerLight,
    onPrimaryContainer = IndigoPurpleOnPrimaryContainerLight,
    secondary = IndigoPurpleSecondaryLight,
    onSecondary = IndigoPurpleOnSecondaryLight,
    secondaryContainer = IndigoPurpleSecondaryContainerLight,
    onSecondaryContainer = IndigoPurpleOnSecondaryContainerLight,
    tertiary = IndigoPurpleTertiaryLight,
    onTertiary = IndigoPurpleOnTertiaryLight,
    surface = IndigoPurpleSurfaceLight,
    onSurface = IndigoPurpleOnSurfaceLight,
    surfaceVariant = IndigoPurpleSurfaceVariantLight,
    onSurfaceVariant = IndigoPurpleOnSurfaceVariantLight,
    surfaceContainer = IndigoPurpleSurfaceContainerLight,
    surfaceContainerLow = IndigoPurpleSurfaceContainerLowLight,
    surfaceContainerHigh = IndigoPurpleSurfaceContainerHighLight,
    outline = IndigoPurpleOutlineLight,
    outlineVariant = IndigoPurpleOutlineVariantLight,
    error = IndigoPurpleErrorLight,
    errorContainer = IndigoPurpleErrorContainerLight,
    onErrorContainer = IndigoPurpleOnErrorContainerLight,
    background = IndigoPurpleSurfaceLight,
    onBackground = IndigoPurpleOnSurfaceLight,
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Default to false so our purple/indigo design remains consistent across devices
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