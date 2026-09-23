package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeThemePreferencesRepository(
    initialDarkMode: Boolean? = null,
) : ThemePreferencesRepository {

    private val _isDarkMode = MutableStateFlow<Boolean?>(initialDarkMode)
    override val isDarkMode: Flow<Boolean?> = _isDarkMode.asStateFlow()

    var setDarkModeCallCount: Int = 0
        private set

    override suspend fun setDarkMode(isDark: Boolean) {
        setDarkModeCallCount++
        _isDarkMode.value = isDark
    }
}
