package com.fahim.geminiApiComposeStarter.data.preferences

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val autoScroll: Boolean = true,
)
