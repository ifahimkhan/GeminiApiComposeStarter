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

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_preferences")

class UserPreferencesDataStore(private val context: Context) {

    val preferredModel: Flow<String> = context.appDataStore.data.map { prefs ->
        prefs[MODEL_KEY] ?: DEFAULT_GEMINI_MODEL
    }

    val voiceEnabled: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[VOICE_ENABLED_KEY] ?: true
    }

    suspend fun setPreferredModel(model: String) {
        context.appDataStore.edit { prefs ->
            prefs[MODEL_KEY] = model
        }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.appDataStore.edit { prefs ->
            prefs[VOICE_ENABLED_KEY] = enabled
        }
    }

    companion object {
        private val MODEL_KEY = stringPreferencesKey("preferred_gemini_model")
        private val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_input_enabled")
        const val DEFAULT_GEMINI_MODEL = "gemini-3.6-flash"
    }
}
