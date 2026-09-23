package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages secure storage of the Gemini API key.
 *
 * Security workflow:
 * 1. On first launch, encrypts the provided API key using AES-256-GCM via Android KeyStore.
 * 2. Persists only ciphertext and IV at rest.
 * 3. Decrypts the key strictly in-memory at the moment GenerativeModel is instantiated.
 * 4. Never exposes or logs decrypted credentials.
 */
class SecureApiKeyStorage(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        // Fallback to standard SharedPreferences (since ciphertext is already Keystore AES-256-GCM encrypted)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Stores the API key after encrypting it with Android KeyStore AES-256-GCM.
     */
    fun saveApiKey(rawKey: String) {
        if (rawKey.isBlank()) return
        val (cipherText, iv) = KeyStoreEncryptionManager.encrypt(rawKey)
        val encodedCipherText = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_CIPHERTEXT, encodedCipherText)
            .putString(KEY_IV, encodedIv)
            .apply()
    }

    /**
     * Decrypts and returns the API key in memory.
     * Returns an empty string if no encrypted key is stored.
     */
    fun getDecryptedApiKey(): String {
        val encodedCipherText = prefs.getString(KEY_CIPHERTEXT, null) ?: return ""
        val encodedIv = prefs.getString(KEY_IV, null) ?: return ""

        return try {
            val cipherText = Base64.decode(encodedCipherText, Base64.NO_WRAP)
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            KeyStoreEncryptionManager.decrypt(cipherText, iv)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Checks if a valid encrypted API key is already stored.
     */
    fun hasStoredApiKey(): Boolean {
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    companion object {
        private const val PREFS_NAME = "secure_gemini_credentials"
        private const val KEY_CIPHERTEXT = "api_key_ciphertext"
        private const val KEY_IV = "api_key_iv"
    }
}
