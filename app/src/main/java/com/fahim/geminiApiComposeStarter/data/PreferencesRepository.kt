package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.preferencesDataStore by preferencesDataStore(
    name = "app_preferences"
)

class PreferencesRepository(
    private val context: Context,
) {

    companion object {
        private val DARK_MODE =
            booleanPreferencesKey("dark_mode")

        private val DYNAMIC_COLORS =
            booleanPreferencesKey("dynamic_colors")
    }

    val darkMode: Flow<Boolean> =
        context.preferencesDataStore.data.map { preferences ->
            preferences[DARK_MODE] ?: false
        }

    val dynamicColors: Flow<Boolean> =
        context.preferencesDataStore.data.map { preferences ->
            preferences[DYNAMIC_COLORS] ?: true
        }

    suspend fun setDarkMode(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.preferencesDataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }
}