package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.apiKeyDataStore by preferencesDataStore(
    name = "secure_api_key_storage"
)

class SecureApiKeyStorage(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager = ApiKeyManager()
) {

    companion object {
        private val ENCRYPTED_KEY = stringPreferencesKey("encrypted_api_key")
        private val IV_KEY = stringPreferencesKey("api_key_iv")
    }

    suspend fun saveApiKey(apiKey: String) {
        val (encryptedKey, iv) = apiKeyManager.encrypt(apiKey)

        context.apiKeyDataStore.edit { preferences ->
            preferences[ENCRYPTED_KEY] = encryptedKey
            preferences[IV_KEY] = iv
        }
    }

    suspend fun getApiKey(): String? {
        val preferences = context.apiKeyDataStore.data.first()

        val encryptedKey = preferences[ENCRYPTED_KEY]
        val iv = preferences[IV_KEY]

        if (encryptedKey.isNullOrBlank() || iv.isNullOrBlank()) {
            return null
        }

        return apiKeyManager.decrypt(
            encryptedText = encryptedKey,
            ivText = iv
        )
    }

    suspend fun hasStoredApiKey(): Boolean {
        val preferences = context.apiKeyDataStore.data.first()

        return !preferences[ENCRYPTED_KEY].isNullOrBlank() &&
                !preferences[IV_KEY].isNullOrBlank()
    }
}