package com.fahim.geminiApiComposeStarter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = CarbonBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceHigh,
    primary = PetronasTeal,
    onPrimary = OnPetronasTeal,
    secondary = SilverSecondary,
    onSecondary = CarbonBlack,
    outline = HairlineOutline,
    error = F1Red,
    onError = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun GeminiApiComposeStarterTheme(
    content: @Composable () -> Unit
) {
    // Force dark scheme, disable dynamic color as per spec
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}