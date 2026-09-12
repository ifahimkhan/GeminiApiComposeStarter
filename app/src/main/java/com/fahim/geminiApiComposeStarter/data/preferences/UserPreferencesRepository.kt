package com.fahim.geminiApiComposeStarter.data.preferences

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val displayName: Flow<String>
    suspend fun setDisplayName(name: String)
}
