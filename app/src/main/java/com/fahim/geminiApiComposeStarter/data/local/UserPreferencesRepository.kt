package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Manages user preferences stored in Jetpack Preferences DataStore.
 */
class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE_OVERRIDE = stringPreferencesKey("dark_mode_override") // "system", "light", "dark"
        val USER_NAME = stringPreferencesKey("user_name")
        val SYSTEM_INSTRUCTION = stringPreferencesKey("system_instruction")
    }

    val darkModePreference: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_MODE_OVERRIDE] ?: "system"
    }

    val userName: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_NAME] ?: "Student"
    }

    suspend fun setDarkModePreference(mode: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_MODE_OVERRIDE] = mode
        }
    }

    suspend fun setUserName(name: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_NAME] = name
        }
    }
}
