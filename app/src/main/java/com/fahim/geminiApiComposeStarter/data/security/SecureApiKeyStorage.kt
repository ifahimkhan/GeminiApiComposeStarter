package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles persistent storage of the encrypted API key.
 * Only the ciphertext and IV are persisted at rest.
 * Decryption occurs exclusively in memory when creating the GenerativeModel instance.
 */
class SecureApiKeyStorage(
    context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
) {
    companion object {
        private const val PREFS_NAME = "secure_gemini_storage"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_payload"
        private const val KEY_IV = "encrypted_api_key_iv"
    }

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Encrypts and persists the API key if not already saved or if a new valid key is provided.
     */
    fun saveApiKey(rawKey: String) {
        if (rawKey.isBlank()) return
        try {
            val payload = cryptoManager.encrypt(rawKey)
            sharedPreferences.edit()
                .putString(KEY_CIPHERTEXT, payload.ciphertext)
                .putString(KEY_IV, payload.iv)
                .apply()
        } catch (_: Exception) {
            // Silently fail without exposing key content
        }
    }

    /**
     * Decrypts and returns the API key in memory.
     * Never logs, prints, or exposes the returned string.
     */
    fun getDecryptedApiKey(): String? {
        val ciphertext = sharedPreferences.getString(KEY_CIPHERTEXT, null)
        val iv = sharedPreferences.getString(KEY_IV, null)
        if (ciphertext.isNullOrBlank() || iv.isNullOrBlank()) {
            return null
        }
        return try {
            cryptoManager.decrypt(ivBase64 = iv, ciphertextBase64 = ciphertext)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if a valid encrypted API key is persisted.
     */
    fun hasKey(): Boolean {
        return sharedPreferences.contains(KEY_CIPHERTEXT) && sharedPreferences.contains(KEY_IV)
    }

    /**
     * Clears any stored encrypted credentials.
     */
    fun clearKey() {
        sharedPreferences.edit().clear().apply()
    }
}
