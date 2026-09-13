package com.example.c020_harsh_assignment1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

interface PreferencesRepository {
    val encryptedApiKey: Flow<String?>
    val responseStyle: Flow<String>
    suspend fun saveEncryptedApiKey(key: String)
    suspend fun saveResponseStyle(style: String)
}

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    private val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
    private val RESPONSE_STYLE = stringPreferencesKey("response_style")

    override val encryptedApiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ENCRYPTED_API_KEY]
        }

    override val responseStyle: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[RESPONSE_STYLE] ?: "Simple"
        }

    override suspend fun saveEncryptedApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[ENCRYPTED_API_KEY] = key
        }
    }

    override suspend fun saveResponseStyle(style: String) {
        context.dataStore.edit { preferences ->
            preferences[RESPONSE_STYLE] = style
        }
    }
}
