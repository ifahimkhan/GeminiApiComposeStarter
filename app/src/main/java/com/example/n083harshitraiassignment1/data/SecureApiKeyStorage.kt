package com.example.n083harshitraiassignment1.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Secure storage manager for API credentials.
 *
 * Implements encryption at rest using the Android KeyStore with AES-256-GCM.
 * Plaintext keys are encrypted on first initialization and only the ciphertext
 * is persisted. Decryption occurs strictly in-memory when needed by network layers.
 */
open class SecureApiKeyStorage(
    private val context: Context? = null,
    private val securityManager: SecurityManager = SecurityManager()
) {

    companion object {
        private const val PREFS_NAME = "secure_vault_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_api_key"
    }

    private val sharedPreferences: SharedPreferences? by lazy {
        context?.applicationContext?.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    /**
     * Retrieves the decrypted API key from encrypted storage.
     * On first launch, encrypts and stores [fallbackKey] if not yet persisted.
     */
    open fun getDecryptedApiKey(fallbackKey: String): String {
        val prefs = sharedPreferences ?: return fallbackKey
        val encryptedKey = prefs.getString(KEY_ENCRYPTED_API_KEY, null)

        if (encryptedKey.isNullOrBlank()) {
            if (fallbackKey.isNotBlank()) {
                try {
                    val cipherText = securityManager.encrypt(fallbackKey)
                    prefs.edit()
                        .putString(KEY_ENCRYPTED_API_KEY, cipherText)
                        .apply()
                } catch (_: Exception) {
                    // If KeyStore initialization fails on legacy device, return fallback
                }
            }
            return fallbackKey
        }

        return try {
            securityManager.decrypt(encryptedKey)
        } catch (_: Exception) {
            // If Keystore was reset/invalidated, refresh with fallback
            if (fallbackKey.isNotBlank()) {
                try {
                    val cipherText = securityManager.encrypt(fallbackKey)
                    prefs.edit()
                        .putString(KEY_ENCRYPTED_API_KEY, cipherText)
                        .apply()
                } catch (_: Exception) { }
            }
            fallbackKey
        }
    }
}
