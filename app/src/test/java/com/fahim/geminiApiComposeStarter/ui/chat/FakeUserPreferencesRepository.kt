package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _userName = MutableStateFlow("User")
    override val userName: Flow<String> = _userName

    override suspend fun setUserName(name: String) {
        _userName.value = name
    }
}