package com.fahim.geminiApiComposeStarter.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColour(enabled: Boolean)
}

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            // A corrupt file should not crash the app; fall back to defaults.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { stored ->
            UserPreferences(
                themeMode = stored[KEY_THEME_MODE]
                    ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                dynamicColour = stored[KEY_DYNAMIC_COLOUR] ?: true,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColour(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOUR] = enabled }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOUR = booleanPreferencesKey("dynamic_colour")
    }
}
