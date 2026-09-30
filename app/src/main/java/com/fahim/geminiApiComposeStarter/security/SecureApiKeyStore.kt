package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.data.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SecureApiKeyStore(
    private val context: Context
) {

    private val encryptedApiKey =
        stringPreferencesKey("encrypted_gemini_api_key")

    suspend fun saveIfNeeded(
        plainKey: String
    ) = withContext(Dispatchers.IO) {

        if (plainKey.isBlank()) {
            return@withContext
        }

        val preferences =
            context.settingsDataStore.data.first()

        val existingKey =
            preferences[encryptedApiKey]

        if (existingKey == null) {

            val encrypted =
                KeystoreCipher.encrypt(plainKey)

            context.settingsDataStore.edit { prefs ->

                prefs[encryptedApiKey] = encrypted
            }
        }
    }

    suspend fun getApiKey(): String =
        withContext(Dispatchers.IO) {

            val preferences =
                context.settingsDataStore.data.first()

            val encrypted =
                preferences[encryptedApiKey]
                    ?: return@withContext ""

            KeystoreCipher.decrypt(encrypted)
        }
}