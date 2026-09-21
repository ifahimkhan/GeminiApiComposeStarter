package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_api_key_prefs")

/**
 * Manages secure persistence of the encrypted Gemini API key.
 * Only the encrypted ciphertext and initialization vector (IV) are persisted.
 */
class SecureApiKeyStorage(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager
) {
    companion object {
        private val KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        private val KEY_IV = stringPreferencesKey("gemini_api_key_iv")
    }

    /**
     * Emits true if an encrypted API key is currently stored.
     */
    val hasApiKeyFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        !preferences[KEY_CIPHERTEXT].isNullOrBlank() && !preferences[KEY_IV].isNullOrBlank()
    }

    /**
     * Initializes key from default source (e.g. BuildConfig) on first run if not already set.
     */
    suspend fun initializeKey(defaultKey: String) {
        val currentEncrypted = dataStore.data.first()[KEY_CIPHERTEXT]
        if (currentEncrypted.isNullOrBlank() && defaultKey.isNotBlank()) {
            saveApiKey(defaultKey.trim())
        }
    }

    /**
     * Encrypts the plain text API key using Keystore AES-256-GCM and persists only the ciphertext and IV.
     */
    suspend fun saveApiKey(plainKey: String) {
        if (plainKey.isBlank()) return
        val encryptedPayload = cryptoManager.encrypt(plainKey.trim())
        dataStore.edit { preferences ->
            preferences[KEY_CIPHERTEXT] = encryptedPayload.ciphertextBase64
            preferences[KEY_IV] = encryptedPayload.ivBase64
        }
    }

    /**
     * Decrypts the API key in-memory on demand for GenerativeModel instantiation.
     * Never logs, prints, or exposes the returned key.
     */
    suspend fun getDecryptedApiKey(): String? {
        val preferences = dataStore.data.first()
        val ciphertext = preferences[KEY_CIPHERTEXT]
        val iv = preferences[KEY_IV]
        if (ciphertext.isNullOrBlank() || iv.isNullOrBlank()) {
            return null
        }
        return try {
            cryptoManager.decrypt(ciphertext, iv)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears the encrypted API key.
     */
    suspend fun clearApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_CIPHERTEXT)
            preferences.remove(KEY_IV)
        }
    }
}
