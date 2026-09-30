package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists only encrypted ciphertext and IV at rest.
 * On first launch, initializes encrypted storage from BuildConfig.
 * Raw API key is never written to disk unencrypted, logged, or exposed in toasts.
 */
class SecureKeyStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gemini_secure_storage"
        private const val KEY_CIPHERTEXT = "encrypted_api_key"
        private const val KEY_IV = "encryption_iv"
        private const val KEY_IS_INITIALIZED = "is_secure_key_initialized"
    }

    /**
     * Initializes encrypted storage with the provided API key if not already initialized.
     * Uses AES-256-GCM via Android KeyStore.
     */
    fun initializeIfNeeded(rawApiKey: String) {
        if (!prefs.getBoolean(KEY_IS_INITIALIZED, false) && rawApiKey.isNotBlank()) {
            val payload = KeyStoreManager.encrypt(rawApiKey)
            prefs.edit()
                .putString(KEY_CIPHERTEXT, payload.cipherTextBase64)
                .putString(KEY_IV, payload.ivBase64)
                .putBoolean(KEY_IS_INITIALIZED, true)
                .apply()
        }
    }

    /**
     * Retrieves and decrypts the API key in memory only.
     * Never store the returned value in persistent properties or logs.
     */
    fun getDecryptedApiKey(): String {
        val cipherText = prefs.getString(KEY_CIPHERTEXT, null) ?: return ""
        val iv = prefs.getString(KEY_IV, null) ?: return ""
        return try {
            KeyStoreManager.decrypt(cipherText, iv)
        } catch (e: Exception) {
            ""
        }
    }

    fun hasKey(): Boolean {
        return prefs.getBoolean(KEY_IS_INITIALIZED, false) &&
                !prefs.getString(KEY_CIPHERTEXT, null).isNullOrBlank()
    }
}
