package com.example.n075nishantassgn1.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.secureKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class SecureApiKeyRepository(
    private val context: Context,
    private val cryptoManager: ApiKeyCryptoManager = ApiKeyCryptoManager()
) {

    private val encryptedKey =
        stringPreferencesKey("encrypted_gemini_api_key")

    suspend fun saveApiKey(apiKey: String) {
        val encrypted = cryptoManager.encrypt(apiKey)

        context.secureKeyDataStore.edit { preferences ->
            preferences[encryptedKey] = encrypted
        }
    }

    suspend fun getApiKey(): String? {
        val preferences = context.secureKeyDataStore.data.first()

        val encrypted = preferences[encryptedKey]
            ?: return null

        return cryptoManager.decrypt(encrypted)
    }
}