package com.fahim.geminiApiComposeStarter.ui.theme

import androidx.compose.ui.graphics.Color

// Core brand palette — a cool blue-to-violet gradient evokes "AI" without borrowing any
// copyrighted logo or asset.
val BrandBlue = Color(0xFF4C7CFF)
val BrandViolet = Color(0xFF8C5CFF)
val BrandTeal = Color(0xFF17B4A6)

// Light theme
val LightPrimary = Color(0xFF3358E8)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFDCE3FF)
val LightOnPrimaryContainer = Color(0xFF0D1B4C)
val LightSecondary = Color(0xFF6B5CA5)
val LightBackground = Color(0xFFFAFAFE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEFF7)
val LightOnSurfaceVariant = Color(0xFF46464F)
val LightOutline = Color(0xFFD8DAE6)
val LightError = Color(0xFFBA1A1A)

// Dark theme
val DarkPrimary = Color(0xFFAEC0FF)
val DarkOnPrimary = Color(0xFF041247)
val DarkPrimaryContainer = Color(0xFF1D2E70)
val DarkOnPrimaryContainer = Color(0xFFDCE3FF)
val DarkSecondary = Color(0xFFC9BEEB)
val DarkBackground = Color(0xFF101018)
val DarkSurface = Color(0xFF17171F)
val DarkSurfaceVariant = Color(0xFF232330)
val DarkOnSurfaceVariant = Color(0xFFC7C6D6)
val DarkOutline = Color(0xFF3A3A47)
val DarkError = Color(0xFFFFB4AB)

// Chat-bubble specific colors (kept outside the Material scheme so bubbles stay legible and
// distinct from generic surfaces in both themes).
val UserBubbleLight = LightPrimary
val UserBubbleDark = Color(0xFF3A4DA8)
val AssistantBubbleLight = Color(0xFFF1F1F8)
val AssistantBubbleDark = Color(0xFF23232F)
val ErrorBubbleLight = Color(0xFFFFDAD6)
val ErrorBubbleDark = Color(0xFF3A2223)
