package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val dynamicColor = MutableStateFlow(true)

    override val useDynamicColor: StateFlow<Boolean> = dynamicColor

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        dynamicColor.value = enabled
    }
}
