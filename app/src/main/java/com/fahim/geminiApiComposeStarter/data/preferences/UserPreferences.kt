package com.fahim.geminiApiComposeStarter.data.preferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)
