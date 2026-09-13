package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val compactBubbles: Flow<Boolean>
    val themeMode: Flow<ThemeMode>

    suspend fun setCompactBubbles(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
}