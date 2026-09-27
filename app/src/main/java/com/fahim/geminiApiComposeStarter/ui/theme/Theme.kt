package com.fahim.geminiApiComposeStarter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Dynamic color (Material You) is intentionally NOT used: it would make the "premium, cohesive
// brand" look the assignment asks for depend on the user's wallpaper. A fixed, designed palette
// is used instead, with dedicated light/dark variants — not a simple inversion.
private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    background = LightBackground,
    onBackground = LightOnPrimaryContainer,
    surface = LightSurface,
    onSurface = LightOnPrimaryContainer,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    background = DarkBackground,
    onBackground = DarkOnPrimaryContainer,
    surface = DarkSurface,
    onSurface = DarkOnPrimaryContainer,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
)

/** Colors that aren't part of the Material color scheme but are used consistently across the
 *  chat UI (bubble backgrounds). Exposed via a small holder so screens don't hardcode branches. */
data class ChatExtraColors(
    val userBubble: Color,
    val assistantBubble: Color,
    val errorBubble: Color,
)

@Composable
fun GeminiApiComposeStarterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extraColors = if (darkTheme) {
        ChatExtraColors(UserBubbleDark, AssistantBubbleDark, ErrorBubbleDark)
    } else {
        ChatExtraColors(UserBubbleLight, AssistantBubbleLight, ErrorBubbleLight)
    }

    CompositionLocalProvider(LocalChatExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

val LocalChatExtraColors = staticCompositionLocalOf {
    ChatExtraColors(UserBubbleLight, AssistantBubbleLight, ErrorBubbleLight)
}
