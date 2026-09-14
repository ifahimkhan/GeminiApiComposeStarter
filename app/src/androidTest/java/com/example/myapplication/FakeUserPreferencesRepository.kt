package com.example.myapplication

import com.example.myapplication.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {

    private val _autoScroll = MutableStateFlow(true)
    override val autoScrollEnabled: Flow<Boolean> = _autoScroll.asStateFlow()

    private val _darkMode = MutableStateFlow<Boolean?>(null)
    override val darkModeOverride: Flow<Boolean?> = _darkMode.asStateFlow()

    override suspend fun setAutoScroll(enabled: Boolean) {
        _autoScroll.value = enabled
    }

    override suspend fun setDarkMode(enabled: Boolean?) {
        _darkMode.value = enabled
    }
}
