package com.example.myapplication.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_key_prefs")

class SecureKeyStorage(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) {

    private val encryptedApiKeyPref = stringPreferencesKey("encrypted_gemini_api_key")

    suspend fun getDecryptedApiKey(): String {
        val encryptedData = context.secureDataStore.data.map { preferences ->
            preferences[encryptedApiKeyPref]
        }.first()

        return if (!encryptedData.isNullOrEmpty()) {
            try {
                val parts = encryptedData.split(":")
                if (parts.size == 2) {
                    val iv = Base64.decode(parts[0], Base64.NO_WRAP)
                    val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
                    val decryptedBytes = cryptoManager.decrypt(iv, ciphertext)
                    String(decryptedBytes, Charsets.UTF_8)
                } else {
                    BuildConfig.GEMINI_API_KEY
                }
            } catch (e: Exception) {
                BuildConfig.GEMINI_API_KEY
            }
        } else {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isNotEmpty()) {
                saveApiKey(apiKey)
            }
            apiKey
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        if (apiKey.isEmpty()) return
        try {
            val (iv, ciphertext) = cryptoManager.encrypt(apiKey.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            val combined = "$ivBase64:$ciphertextBase64"

            context.secureDataStore.edit { preferences ->
                preferences[encryptedApiKeyPref] = combined
            }
        } catch (e: Exception) {
            // Ignore error
        }
    }
}
