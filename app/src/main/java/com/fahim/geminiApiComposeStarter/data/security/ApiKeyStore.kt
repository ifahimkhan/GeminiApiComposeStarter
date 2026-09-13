package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import com.fahim.geminiApiComposeStarter.BuildConfig
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.apiKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class ApiKeyStore(
    private val context: Context
) {

    companion object {
        private val ENCRYPTED_KEY =
            stringPreferencesKey("encrypted_api_key")
    }

    suspend fun getApiKey(): String? {

        val preferences = context.apiKeyDataStore.data.first()

        val encryptedKey =
            preferences[ENCRYPTED_KEY]

        if (!encryptedKey.isNullOrBlank()) {
            return KeystoreCipher.decrypt(encryptedKey)
        }

        // First run: take key from BuildConfig,
        // encrypt it and store only ciphertext.
        val plainKey =
            BuildConfig.GEMINI_API_KEY.trim()

        if (plainKey.isBlank()) {
            return null
        }

        val encrypted =
            KeystoreCipher.encrypt(plainKey)

        context.apiKeyDataStore.edit {
            it[ENCRYPTED_KEY] = encrypted
        }

        return plainKey
    }
}