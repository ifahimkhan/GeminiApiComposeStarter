package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.apiKeyDataStore by preferencesDataStore(
    name = "secure_api_key_storage"
)

class ApiKeyStorage(
    private val context: Context
) {

    companion object {
        private val CIPHER_TEXT_KEY =
            stringPreferencesKey("encrypted_api_key")

        private val IV_KEY =
            stringPreferencesKey("api_key_iv")
    }

    suspend fun save(encryptedData: EncryptedData) {
        context.apiKeyDataStore.edit { preferences ->
            preferences[CIPHER_TEXT_KEY] = encryptedData.cipherText
            preferences[IV_KEY] = encryptedData.iv
        }
    }

    suspend fun read(): EncryptedData? {
        val preferences = context.apiKeyDataStore.data.first()

        val cipherText = preferences[CIPHER_TEXT_KEY]
        val iv = preferences[IV_KEY]

        if (cipherText == null || iv == null) {
            return null
        }

        return EncryptedData(
            cipherText = cipherText,
            iv = iv
        )
    }

    suspend fun hasStoredKey(): Boolean {
        return read() != null
    }
}