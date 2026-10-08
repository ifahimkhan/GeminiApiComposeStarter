package com.n149.geminichat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Deep Black / AMOLED colour tokens ─────────────────────────────────────────

object DeepBlack {
    val Black        = Color(0xFF000000)   // AMOLED true black background
    val Surface      = Color(0xFF0A0A0A)   // near-black surface
    val Card         = Color(0xFF111111)   // slightly lifted card bg
    val Elevated     = Color(0xFF1A1A1A)   // input / top-bar bg
    val Divider      = Color(0xFF1F1F1F)

    // Accent – electric blue-violet glow
    val Accent       = Color(0xFF6C63FF)   // primary accent
    val AccentDim    = Color(0xFF3D3880)   // user bubble bg
    val AccentGlow   = Color(0xFF9D97FF)   // accent text / icon

    // Gemini bubble
    val GeminiBg     = Color(0xFF161622)   // dark indigo bubble
    val GeminiText   = Color(0xFFE8E8FF)

    // Text
    val TextPrimary  = Color(0xFFF2F2F2)
    val TextSecondary= Color(0xFF9E9E9E)
    val TextMuted    = Color(0xFF5C5C5C)

    // Status
    val Error        = Color(0xFFFF4444)
    val ErrorBg      = Color(0xFF2D0A0A)
    val Success      = Color(0xFF00E676)
    val Mic          = Color(0xFF1C1C2E)
}

// ── Forced AMOLED dark colour scheme (dynamic colour disabled) ─────────────────

private val AmoledBlackScheme = darkColorScheme(
    primary              = DeepBlack.Accent,
    onPrimary            = Color.White,
    primaryContainer     = DeepBlack.AccentDim,
    onPrimaryContainer   = DeepBlack.AccentGlow,

    secondary            = DeepBlack.AccentGlow,
    onSecondary          = DeepBlack.Black,
    secondaryContainer   = DeepBlack.Mic,
    onSecondaryContainer = DeepBlack.AccentGlow,

    tertiary             = DeepBlack.Success,
    onTertiary           = DeepBlack.Black,

    background           = DeepBlack.Black,
    onBackground         = DeepBlack.TextPrimary,

    surface              = DeepBlack.Surface,
    onSurface            = DeepBlack.TextPrimary,
    surfaceVariant       = DeepBlack.GeminiBg,
    onSurfaceVariant     = DeepBlack.GeminiText,

    surfaceTint          = DeepBlack.Accent,

    outline              = DeepBlack.Divider,
    outlineVariant       = DeepBlack.TextMuted,

    error                = DeepBlack.Error,
    onError              = Color.White,
    errorContainer       = DeepBlack.ErrorBg,
    onErrorContainer     = DeepBlack.Error,

    inverseSurface       = DeepBlack.TextPrimary,
    inverseOnSurface     = DeepBlack.Black,
    inversePrimary       = DeepBlack.AccentDim,

    scrim                = Color(0xCC000000),
)

// ── Typography ─────────────────────────────────────────────────────────────────

private val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,   fontSize = 32.sp, lineHeight = 40.sp, color = DeepBlack.TextPrimary),
    headlineMedium= TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 20.sp, lineHeight = 28.sp, color = DeepBlack.TextPrimary),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 18.sp, lineHeight = 24.sp, color = DeepBlack.TextPrimary),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 15.sp, lineHeight = 23.sp, color = DeepBlack.TextPrimary),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 22.sp, color = DeepBlack.TextPrimary),
    bodySmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 18.sp, color = DeepBlack.TextSecondary),
    labelMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 12.sp, lineHeight = 16.sp, color = DeepBlack.TextSecondary),
    labelSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 10.sp, lineHeight = 14.sp, color = DeepBlack.TextMuted),
)

/**
 * Deep Black / AMOLED theme.
 * Dynamic colour is intentionally disabled so the pure-black palette
 * is always applied regardless of Android version or wallpaper.
 */
@Composable
fun GeminiChatTheme(
    // parameter kept for API compatibility; dark is always forced
    @Suppress("UNUSED_PARAMETER")
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledBlackScheme,  // always deep black
        typography  = AppTypography,
        content     = content
    )
}
