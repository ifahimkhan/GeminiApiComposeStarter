package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_gemini_prefs")

interface ApiKeyProvider {
    suspend fun getApiKey(): String?
    suspend fun saveApiKey(apiKey: String)
    suspend fun hasApiKey(): Boolean
}

/**
 * Persists only the encrypted ciphertext and IV in Preferences DataStore.
 * Decrypts in memory only at the moment GenerativeModel requires the key.
 * Never logs, toasts, or displays the decrypted value.
 */
class SecureApiKeyStorage(
    private val context: Context,
    private val keystoreManager: KeystoreManager = KeystoreManager(),
    private val defaultFallbackKey: String = "",
) : ApiKeyProvider {

    companion object {
        private val KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        private val KEY_IV = stringPreferencesKey("gemini_api_key_iv")
    }

    /**
     * Retrieves the decrypted key in-memory. If no key is saved yet and a build-time
     * key exists, it encrypts and persists it for subsequent launches.
     */
    override suspend fun getApiKey(): String? {
        val preferences = context.secureDataStore.data.first()
        val ciphertext = preferences[KEY_CIPHERTEXT]
        val iv = preferences[KEY_IV]

        if (!ciphertext.isNullOrBlank() && !iv.isNullOrBlank()) {
            return try {
                keystoreManager.decrypt(EncryptedPayload(ivBase64 = iv, ciphertextBase64 = ciphertext))
            } catch (_: Exception) {
                null
            }
        }

        // First launch fallback: if BuildConfig key exists, encrypt and persist it
        if (defaultFallbackKey.isNotBlank()) {
            saveApiKey(defaultFallbackKey)
            return defaultFallbackKey
        }

        return null
    }

    override suspend fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        val encrypted = keystoreManager.encrypt(apiKey)
        context.secureDataStore.edit { prefs ->
            prefs[KEY_CIPHERTEXT] = encrypted.ciphertextBase64
            prefs[KEY_IV] = encrypted.ivBase64
        }
    }

    override suspend fun hasApiKey(): Boolean {
        val preferences = context.secureDataStore.data.first()
        val hasEncryptedKey = !preferences[KEY_CIPHERTEXT].isNullOrBlank()
        return hasEncryptedKey || defaultFallbackKey.isNotBlank()
    }
}
