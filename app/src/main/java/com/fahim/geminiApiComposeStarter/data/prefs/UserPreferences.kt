package com.fahim.geminiApiComposeStarter.data.prefs

import androidx.compose.runtime.Immutable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Preferences that survive app restarts, persisted with Preferences DataStore. */
@Immutable
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColour: Boolean = true,
)
