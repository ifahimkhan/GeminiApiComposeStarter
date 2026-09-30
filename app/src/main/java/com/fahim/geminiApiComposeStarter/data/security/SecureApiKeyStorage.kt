package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages encrypted storage of sensitive API credentials at rest.
 *
 * Combines Android Keystore (to protect the AES-256 key) with AES-GCM authenticated
 * encryption to protect the actual API key string in private SharedPreferences.
 */
class SecureApiKeyStorage(
    context: Context,
    private val keyStoreManager: KeyStoreManager = KeyStoreManager(),
    prefsName: String = PREFS_NAME,
) {

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /**
     * Encrypts and securely stores the API key.
     */
    fun storeEncryptedApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        try {
            val encryptedPayload = keyStoreManager.encrypt(apiKey)
            sharedPreferences.edit()
                .putString(KEY_ENCRYPTED_API_KEY, encryptedPayload)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt API key for secure storage", e)
        }
    }

    /**
     * Decrypts and retrieves the stored API key, or returns null if not stored or corrupted.
     */
    fun getDecryptedApiKey(): String? {
        val encryptedPayload = sharedPreferences.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
        return try {
            keyStoreManager.decrypt(encryptedPayload)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key from secure storage, clearing invalid data", e)
            clearStoredKey()
            null
        }
    }

    /**
     * Checks whether an encrypted API key is currently stored.
     */
    fun hasStoredKey(): Boolean {
        return sharedPreferences.contains(KEY_ENCRYPTED_API_KEY)
    }

    /**
     * Clears the encrypted API key from storage.
     */
    fun clearStoredKey() {
        sharedPreferences.edit().remove(KEY_ENCRYPTED_API_KEY).apply()
    }

    companion object {
        private const val TAG = "SecureApiKeyStorage"
        private const val PREFS_NAME = "gemini_secure_credentials"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_api_key"
    }
}
