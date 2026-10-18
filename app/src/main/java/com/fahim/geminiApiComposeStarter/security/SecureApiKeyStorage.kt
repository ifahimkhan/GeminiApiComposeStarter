package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_security_prefs")

/**
 * Manages secure persistence of the Gemini API key ciphertext using Preferences DataStore.
 * Plaintext is never stored in persistent storage; only encrypted ciphertext and IV are saved.
 * Decryption occurs strictly in-memory when needed by the generative model factory.
 */
class SecureApiKeyStorage(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
) {
    private val ciphertextKey = stringPreferencesKey("encrypted_gemini_api_key_cipher")
    private val ivKey = stringPreferencesKey("encrypted_gemini_api_key_iv")

    /**
     * Encrypts and securely persists the given API key.
     */
    suspend fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        val encrypted = cryptoManager.encrypt(apiKey)
        val cipherBase64 = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP)

        context.securityDataStore.edit { prefs ->
            prefs[ciphertextKey] = cipherBase64
            prefs[ivKey] = ivBase64
        }
    }

    /**
     * Decrypts the persisted ciphertext into memory.
     * Returns null if no key is stored or decryption fails.
     */
    suspend fun getDecryptedApiKey(): String? {
        val prefs = context.securityDataStore.data.firstOrNull() ?: return null
        val cipherBase64 = prefs[ciphertextKey] ?: return null
        val ivBase64 = prefs[ivKey] ?: return null

        return try {
            val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            cryptoManager.decrypt(cipherBytes, ivBytes)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks whether an encrypted API key exists in storage.
     */
    suspend fun hasEncryptedKey(): Boolean {
        val prefs = context.securityDataStore.data.firstOrNull() ?: return false
        return !prefs[ciphertextKey].isNullOrBlank() && !prefs[ivKey].isNullOrBlank()
    }
}
