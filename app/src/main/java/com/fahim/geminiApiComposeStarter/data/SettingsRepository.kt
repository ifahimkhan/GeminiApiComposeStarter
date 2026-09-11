package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val RESPONSE_LENGTH_KEY = stringPreferencesKey("response_length")

    val responseLength: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[RESPONSE_LENGTH_KEY] ?: "Normal"
        }

    suspend fun setResponseLength(length: String) {
        context.dataStore.edit { preferences ->
            preferences[RESPONSE_LENGTH_KEY] = length
        }
    }
}
