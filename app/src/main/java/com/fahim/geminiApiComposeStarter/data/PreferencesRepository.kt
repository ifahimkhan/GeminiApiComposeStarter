package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
