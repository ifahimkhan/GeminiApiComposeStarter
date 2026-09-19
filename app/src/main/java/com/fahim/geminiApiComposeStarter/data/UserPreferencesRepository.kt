package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val DARK_MODE_PREF = booleanPreferencesKey("dark_mode_pref")
        val ENCRYPTED_API_KEY_PREF = stringPreferencesKey("encrypted_api_key_pref")
    }

    private val keystoreCrypto = KeystoreCrypto()

    val darkModeFlow: Flow<Boolean?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DARK_MODE_PREF] }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_PREF] = enabled
        }
    }

    suspend fun saveEncryptedApiKey(apiKey: String) {
        val encrypted = keystoreCrypto.encrypt(apiKey)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_API_KEY_PREF] = encrypted
        }
    }

    val decryptedApiKeyFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_API_KEY_PREF]?.let { encrypted ->
                try {
                    keystoreCrypto.decrypt(encrypted)
                } catch (e: Exception) {
                    null
                }
            }
        }
}
