package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.security.EncryptedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val autoScrollEnabled: Flow<Boolean>
    val isDarkMode: Flow<Boolean?>
    val encryptedApiKeyData: Flow<EncryptedData?>

    suspend fun setAutoScrollEnabled(enabled: Boolean)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun saveEncryptedApiKey(encryptedData: EncryptedData)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private object PreferencesKeys {
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ENCRYPTED_KEY = stringPreferencesKey("encrypted_api_key")
        val ENCRYPTED_IV = stringPreferencesKey("encrypted_api_key_iv")
    }

    override val autoScrollEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SCROLL] ?: true
    }

    override val isDarkMode: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DARK_MODE]
    }

    override val encryptedApiKeyData: Flow<EncryptedData?> = context.dataStore.data.map { preferences ->
        val key = preferences[PreferencesKeys.ENCRYPTED_KEY]
        val iv = preferences[PreferencesKeys.ENCRYPTED_IV]
        if (!key.isNullOrBlank() && !iv.isNullOrBlank()) {
            EncryptedData(ciphertextBase64 = key, ivBase64 = iv)
        } else {
            null
        }
    }

    override suspend fun setAutoScrollEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCROLL] = enabled
        }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
        }
    }

    override suspend fun saveEncryptedApiKey(encryptedData: EncryptedData) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_KEY] = encryptedData.ciphertextBase64
            preferences[PreferencesKeys.ENCRYPTED_IV] = encryptedData.ivBase64
        }
    }
}
