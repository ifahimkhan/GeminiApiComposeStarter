package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Log

/**
 * Manages the security lifecycle of the Gemini API key.
 *
 * Requirements satisfied:
 * - The AES key is generated and maintained in Android Keystore.
 * - The Gemini API key is encrypted at rest using AES-256-GCM.
 * - Only the ciphertext and IV are persisted. Plaintext is never stored.
 * - Decryption occurs strictly in-memory when needed by the Gemini client.
 * - Plaintext is never logged, toasted, displayed, or embedded in exceptions.
 */
class ApiKeyManager(
    private val cryptoEngine: CryptoEngine,
    private val storage: ApiKeyStorage,
) {

    constructor(context: Context) : this(
        cryptoEngine = KeyStoreCryptoEngine(),
        storage = EncryptedSharedPrefsStorage(context),
    )

    companion object {
        private const val TAG = "ApiKeyManager"
    }

    /**
     * Initializes secure storage on first launch or when a new valid BuildConfig key is provided.
     * Encrypts the provided key using AES-256-GCM and persists the ciphertext + IV.
     * The plaintext key is discarded from this scope immediately after encryption.
     */
    @Synchronized
    fun initialize(buildConfigKey: String) {
        val trimmedKey = buildConfigKey.trim()
        if (trimmedKey.isNotBlank()) {
            if (!storage.hasEncryptedKey()) {
                encryptAndStore(trimmedKey)
            }
        }
    }

    /**
     * Encrypts the given plaintext key and saves only the ciphertext and IV to persistent storage.
     */
    @Synchronized
    fun encryptAndStore(plainTextKey: String) {
        try {
            val encryptedData = cryptoEngine.encrypt(plainTextKey)
            storage.saveEncryptedKey(encryptedData)
        } catch (e: Exception) {
            // Note: Never log the key or plaintext in logs or exceptions
            Log.e(TAG, "Failed to encrypt and store API key: ${e.javaClass.simpleName}")
        }
    }

    /**
     * Checks if a valid encrypted API key is present in persistent storage.
     */
    fun hasApiKey(): Boolean {
        return storage.hasEncryptedKey()
    }

    /**
     * Decrypts the stored API key in memory.
     * This is invoked only at the moment GenerativeModel is instantiated.
     * The decrypted value is never stored in persistent variables or logs.
     */
    @Synchronized
    fun getDecryptedApiKey(): String? {
        val encryptedData = storage.getEncryptedKey() ?: return null
        return try {
            cryptoEngine.decrypt(encryptedData)
        } catch (e: Exception) {
            // Note: Never log the key or plaintext in logs or exceptions
            Log.e(TAG, "Failed to decrypt API key from secure storage: ${e.javaClass.simpleName}")
            null
        }
    }

    /**
     * Clears the encrypted API key from storage if needed.
     */
    @Synchronized
    fun clear() {
        storage.clear()
    }
}
