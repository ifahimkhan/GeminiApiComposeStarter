package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.Base64

/**
 * Manages secure persistence of the encrypted Gemini API key.
 *
 * Plaintext API key is NEVER persisted:
 * - On first launch (or when a new key is provided via BuildConfig),
 *   it is encrypted with AES-256-GCM in Android Keystore.
 * - Only the ciphertext, initialization vector (IV), and a SHA-256 hash are persisted.
 * - Decryption occurs strictly on demand in-memory.
 */
class SecureApiKeyManager(
    context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(),
) : ApiKeyProvider {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    companion object {
        private const val PREFS_NAME = "gemini_secure_prefs"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_ciphertext"
        private const val KEY_IV = "encrypted_api_key_iv"
        private const val KEY_SEED_HASH = "seed_key_sha256"
    }

    /**
     * Initializes and stores encrypted key if provided through BuildConfig.
     * Checks SHA-256 hash to re-encrypt only when the configured key changes.
     */
    @Synchronized
    fun initializeFromBuildConfig(buildConfigApiKey: String) {
        val trimmed = buildConfigApiKey.trim()
        if (trimmed.isEmpty()) return

        val currentHash = sha256(trimmed)
        val storedHash = prefs.getString(KEY_SEED_HASH, null)

        if (storedHash != currentHash || !hasApiKey()) {
            storeApiKeyInternal(trimmed, currentHash)
        }
    }

    /**
     * Encrypts the plaintext key using AES-256-GCM and stores ciphertext and IV.
     * Plaintext is never written to disk or logged.
     */
    @Synchronized
    private fun storeApiKeyInternal(plainKey: String, hash: String) {
        try {
            val encryptedPayload = cryptoManager.encrypt(plainKey)
            val ivBase64 = Base64.getEncoder().encodeToString(encryptedPayload.iv)
            val cipherBase64 = Base64.getEncoder().encodeToString(encryptedPayload.ciphertext)

            prefs.edit()
                .putString(KEY_IV, ivBase64)
                .putString(KEY_CIPHERTEXT, cipherBase64)
                .putString(KEY_SEED_HASH, hash)
                .apply()
        } catch (_: Exception) {
            // Suppress details to avoid leaking context in logs
        }
    }

    /**
     * Decrypts the API key strictly in-memory when required.
     * Returns null if missing or decryption fails.
     */
    override fun getApiKey(): String? {
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null
        val cipherBase64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null

        return try {
            val iv = Base64.getDecoder().decode(ivBase64)
            val ciphertext = Base64.getDecoder().decode(cipherBase64)
            cryptoManager.decrypt(EncryptedPayload(iv = iv, ciphertext = ciphertext))
        } catch (_: Exception) {
            null
        }
    }

    override fun hasApiKey(): Boolean {
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
