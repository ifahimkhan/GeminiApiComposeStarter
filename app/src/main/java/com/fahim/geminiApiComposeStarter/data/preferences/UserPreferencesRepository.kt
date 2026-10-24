package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val temperature: Float = 0.7f,
    val dynamicColorEnabled: Boolean = true,
    val systemInstruction: String = "",
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val TEMPERATURE = floatPreferencesKey("temperature")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val SYSTEM_INSTRUCTION = stringPreferencesKey("system_instruction")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                temperature = preferences[PreferencesKeys.TEMPERATURE] ?: 0.7f,
                dynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true,
                systemInstruction = preferences[PreferencesKeys.SYSTEM_INSTRUCTION].orEmpty(),
            )
        }

    suspend fun updateTemperature(temperature: Float) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.TEMPERATURE] = temperature
        }
    }

    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateSystemInstruction(instruction: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_INSTRUCTION] = instruction
        }
    }
}
