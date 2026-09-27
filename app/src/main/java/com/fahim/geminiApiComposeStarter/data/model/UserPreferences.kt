package com.fahim.geminiApiComposeStarter.data.model

import androidx.compose.runtime.Immutable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How Gemini should answer; sent to the model as a system instruction. */
enum class ResponseStyle(val instruction: String) {
    CONCISE("Answer briefly, in at most three short sentences or bullet points."),
    BALANCED("Answer clearly with a moderate amount of detail."),
    DETAILED("Answer thoroughly with explanations, steps and examples where useful."),
}

@Immutable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val responseStyle: ResponseStyle = ResponseStyle.BALANCED,
)
