package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences
import java.util.Base64

/**
 * Handles encrypted persistence and in-memory decryption of the Gemini API key.
 *
 * Requirements satisfied:
 * - Persists only ciphertext and IV; plaintext is NEVER written to storage.
 * - Hardware-backed Android Keystore provides the AES-256 key.
 * - AES-256-GCM encryption with fresh randomized IV on every store operation.
 * - Decryption occurs strictly in-memory when accessed by the repository.
 * - Never logs or exposes decrypted secrets.
 */
class SecureApiKeyStorage(
    context: Context,
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    ),
) {

    /**
     * Encrypts the provided raw API key using Keystore AES-256-GCM and persists
     * ONLY the ciphertext and the IV. Plaintext is discarded immediately.
     */
    fun storeApiKey(rawKey: String) {
        val trimmedKey = rawKey.trim()
        if (trimmedKey.isEmpty()) return

        val secretKey = KeystoreManager.getOrCreateSecretKey()
        val payload = AesGcmCipher.encrypt(plaintext = trimmedKey, secretKey = secretKey)

        val ciphertextBase64 = Base64.getEncoder().encodeToString(payload.ciphertext)
        val ivBase64 = Base64.getEncoder().encodeToString(payload.iv)

        preferences.edit()
            .putString(KEY_CIPHERTEXT, ciphertextBase64)
            .putString(KEY_IV, ivBase64)
            .apply()
    }

    /**
     * Decrypts the stored ciphertext in-memory using the Keystore AES-256 key and IV.
     * Returns null if no encrypted key is stored or decryption fails.
     * The returned string must only be used in-memory and never logged or toasted.
     */
    fun getDecryptedApiKey(): String? {
        val ciphertextBase64 = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivBase64 = preferences.getString(KEY_IV, null) ?: return null

        return try {
            val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
            val iv = Base64.getDecoder().decode(ivBase64)
            val payload = AesGcmCipher.EncryptedPayload(ciphertext = ciphertext, iv = iv)
            val secretKey = KeystoreManager.getOrCreateSecretKey()
            AesGcmCipher.decrypt(payload = payload, secretKey = secretKey)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if ciphertext and IV exist in storage without performing in-memory decryption.
     */
    fun hasEncryptedApiKey(): Boolean {
        return preferences.contains(KEY_CIPHERTEXT) && preferences.contains(KEY_IV)
    }

    companion object {
        private const val PREFS_NAME = "gemini_secure_storage"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_ciphertext"
        private const val KEY_IV = "encrypted_api_key_iv"
    }
}
