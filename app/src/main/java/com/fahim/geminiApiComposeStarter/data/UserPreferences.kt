package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferences(private val context: Context) {

    private val isVoiceInputEnabledKey = booleanPreferencesKey("voice_input_enabled")

    val isVoiceInputEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[isVoiceInputEnabledKey] ?: true
        }

    suspend fun setVoiceInputEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isVoiceInputEnabledKey] = enabled
        }
    }
}
