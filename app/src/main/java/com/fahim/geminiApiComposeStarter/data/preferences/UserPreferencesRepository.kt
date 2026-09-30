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

private val Context.userStore: DataStore<Preferences> by preferencesDataStore(name = "tanay_settings")

class UserPreferencesRepository(private val context: Context) {

    val modelNameFlow: Flow<String> = context.userStore.data.map { prefs ->
        prefs[MODEL_KEY] ?: "gemini-2.5-flash"
    }

    val isVoiceEnabled: Flow<Boolean> = context.userStore.data.map { prefs ->
        prefs[VOICE_KEY] ?: true
    }

    suspend fun setModelName(model: String) {
        context.userStore.edit { prefs ->
            prefs[MODEL_KEY] = model
        }
    }

    suspend fun setVoiceSupport(enabled: Boolean) {
        context.userStore.edit { prefs ->
            prefs[VOICE_KEY] = enabled
        }
    }

    companion object {
        private val MODEL_KEY = stringPreferencesKey("selected_gemini_model")
        private val VOICE_KEY = booleanPreferencesKey("voice_feature_active")
    }
}
