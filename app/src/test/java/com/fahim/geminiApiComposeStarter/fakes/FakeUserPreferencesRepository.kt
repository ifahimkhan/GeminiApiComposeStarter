package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferences
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _userPreferences = MutableStateFlow(UserPreferences())
    override val userPreferences: Flow<UserPreferences> = _userPreferences.asStateFlow()

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        _userPreferences.value = _userPreferences.value.copy(themeMode = themeMode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        _userPreferences.value = _userPreferences.value.copy(dynamicColor = enabled)
    }
}
