package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
    }

    val autoScrollEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_SCROLL] ?: true
    }

    val darkModeEnabled: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE]
    }

    suspend fun setAutoScroll(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SCROLL] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }
}
