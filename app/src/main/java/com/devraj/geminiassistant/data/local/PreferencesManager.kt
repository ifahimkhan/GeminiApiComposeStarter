package com.devraj.geminiassistant.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "devraj_user_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val SYSTEM_INSTRUCTION_KEY = stringPreferencesKey("system_instruction")
        val TEMPERATURE_KEY = floatPreferencesKey("temperature")
        val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model")

        const val DEFAULT_SYSTEM_INSTRUCTION = "You are an intelligent, concise, and helpful AI assistant."
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }

    val systemInstruction: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYSTEM_INSTRUCTION_KEY] ?: DEFAULT_SYSTEM_INSTRUCTION
    }

    val temperature: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[TEMPERATURE_KEY] ?: DEFAULT_TEMPERATURE
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MODEL_KEY] ?: DEFAULT_MODEL
    }

    suspend fun savePreferences(
        systemInstruction: String,
        temperature: Float,
        model: String = DEFAULT_MODEL
    ) {
        context.dataStore.edit { preferences ->
            preferences[SYSTEM_INSTRUCTION_KEY] = systemInstruction
            preferences[TEMPERATURE_KEY] = temperature
            preferences[SELECTED_MODEL_KEY] = model
        }
    }
}
