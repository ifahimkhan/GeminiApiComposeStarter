package com.fahim.geminiApiComposeStarter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Paper,
    onPrimary = JetInk,
    primaryContainer = Slate,
    onPrimaryContainer = Paper,
    secondary = Pewter,
    onSecondary = JetInk,
    secondaryContainer = Charcoal,
    onSecondaryContainer = Paper,
    tertiary = Sunbeam,
    background = JetInk,
    onBackground = Cream,
    surface = Charcoal,
    onSurface = Cream,
    surfaceVariant = Slate,
    onSurfaceVariant = Dove,
    outline = Steel,
    error = Ember,
    errorContainer = Charcoal,
    onErrorContainer = Ember,
)

private val LightColorScheme = lightColorScheme(
    primary = JetInk,
    onPrimary = Paper,
    primaryContainer = JetInk,
    onPrimaryContainer = Paper,
    secondary = Fog,
    onSecondary = Paper,
    secondaryContainer = Cream,
    onSecondaryContainer = Steel,
    tertiary = Sunbeam,
    background = Paper,
    onBackground = JetInk,
    surface = Paper,
    onSurface = JetInk,
    surfaceVariant = Cream,
    onSurfaceVariant = Steel,
    outline = Dove,
    error = Ember,
    errorContainer = Sand,
    onErrorContainer = JetInk,
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
