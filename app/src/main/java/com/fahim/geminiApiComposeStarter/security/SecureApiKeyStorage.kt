package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Handles secure storage of the Gemini API key.
 * On first launch, the key from BuildConfig is encrypted with Android Keystore (AES-256-GCM)
 * and persisted in private local storage. It is only decrypted in-memory when required.
 */
class SecureApiKeyStorage(
    context: Context,
    private val keyStoreManager: KeyStoreManager = KeyStoreManager(),
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    /**
     * Initializes secure storage on first launch or when the key is updated.
     * Encrypts the provided raw API key and stores only the ciphertext and IV.
     */
    fun initializeKeyIfNeeded(rawApiKey: String) {
        val trimmedKey = rawApiKey.trim()
        if (trimmedKey.isNotBlank()) {
            val currentDecrypted = getDecryptedApiKey()
            if (currentDecrypted != trimmedKey) {
                storeEncryptedKey(trimmedKey)
            }
        }
    }

    /**
     * Checks if an encrypted API key is already stored.
     */
    fun hasStoredKey(): Boolean {
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    /**
     * Encrypts and persists a new API key.
     */
    fun storeEncryptedKey(apiKey: String) {
        val payload = keyStoreManager.encrypt(apiKey)
        val (ivStr, cipherStr) = payload.toBase64Strings()
        prefs.edit()
            .putString(KEY_IV, ivStr)
            .putString(KEY_CIPHERTEXT, cipherStr)
            .apply()
    }

    /**
     * Decrypts the stored key strictly in-memory when needed to invoke Gemini.
     * Never logs, displays, or exposes the decrypted key.
     */
    fun getDecryptedApiKey(): String? {
        val ivStr = prefs.getString(KEY_IV, null) ?: return null
        val cipherStr = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        return try {
            val payload = EncryptedPayload.fromBase64Strings(ivStr, cipherStr)
            keyStoreManager.decrypt(payload).trim().takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears the stored encrypted key.
     */
    fun clearKey() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "gemini_secure_storage"
        private const val KEY_IV = "gemini_api_key_iv"
        private const val KEY_CIPHERTEXT = "gemini_api_key_ciphertext"
    }
}
