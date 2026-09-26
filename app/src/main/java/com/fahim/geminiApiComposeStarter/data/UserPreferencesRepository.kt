package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

/** Remembers the user's dark/light theme choice; null means "follow the system setting". */
interface UserPreferencesRepository {
    val darkModeOverride: Flow<Boolean?>
    suspend fun setDarkModeOverride(enabled: Boolean)
}
