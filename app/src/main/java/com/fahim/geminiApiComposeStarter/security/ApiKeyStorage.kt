package com.fahim.geminiApiComposeStarter.security

import android.content.Context

/**
 * Persists only the encrypted ciphertext and IV at rest.
 * Uses hardware-backed Android Keystore AES-256-GCM encryption.
 */
class ApiKeyStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        "secure_api_key_store",
        Context.MODE_PRIVATE
    )

    /**
     * Initializes key encryption on first launch.
     * Encrypts the raw build-time key using AES-256-GCM in Android Keystore
     * and persists only the ciphertext and IV at rest.
     */
    fun initializeKeyAtRest(rawBuildKey: String) {
        val existingCipher = prefs.getString(KEY_CIPHERTEXT, null)
        if (existingCipher.isNullOrBlank() && rawBuildKey.isNotBlank()) {
            val payload = CryptoManager.encrypt(rawBuildKey)
            prefs.edit()
                .putString(KEY_CIPHERTEXT, payload.ciphertextBase64)
                .putString(KEY_IV, payload.ivBase64)
                .apply()
        }
    }

    /**
     * Decrypts the stored key into memory ONLY when required to initialize GenerativeModel.
     * Never logs, toasts, or persists this decrypted value in cleartext.
     */
    fun getDecryptedApiKey(fallbackBuildKey: String = ""): String {
        val cipher = prefs.getString(KEY_CIPHERTEXT, null)
        val iv = prefs.getString(KEY_IV, null)
        return if (!cipher.isNullOrBlank() && !iv.isNullOrBlank()) {
            try {
                CryptoManager.decrypt(cipher, iv)
            } catch (_: Exception) {
                fallbackBuildKey
            }
        } else {
            fallbackBuildKey
        }
    }

    fun hasApiKey(fallbackBuildKey: String = ""): Boolean {
        val decrypted = getDecryptedApiKey(fallbackBuildKey)
        return decrypted.isNotBlank()
    }

    companion object {
        private const val KEY_CIPHERTEXT = "gemini_api_key_ciphertext"
        private const val KEY_IV = "gemini_api_key_iv"
    }
}

