package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    val selectedModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_MODEL_NAME] ?: DEFAULT_MODEL
    }

    val isVoiceInputEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_VOICE_INPUT] ?: true
    }

    suspend fun updateSelectedModel(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL_NAME] = modelName
        }
    }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_VOICE_INPUT] = enabled
        }
    }

    companion object {
        private val KEY_MODEL_NAME = stringPreferencesKey("selected_model_name")
        private val KEY_VOICE_INPUT = booleanPreferencesKey("voice_input_enabled")
        const val DEFAULT_MODEL = "gemini-1.5-flash"
    }
}
