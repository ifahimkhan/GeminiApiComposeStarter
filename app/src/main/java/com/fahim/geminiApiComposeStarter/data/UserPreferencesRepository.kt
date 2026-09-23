package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.prefsStore by preferencesDataStore(name = "user_prefs")

/** Stores simple user settings. A null dark mode means "follow the system". */
class UserPreferencesRepository(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")

    val darkMode: Flow<Boolean?> = context.prefsStore.data.map { it[darkModeKey] }

    suspend fun setDarkMode(enabled: Boolean) {
        context.prefsStore.edit { it[darkModeKey] = enabled }
    }
}