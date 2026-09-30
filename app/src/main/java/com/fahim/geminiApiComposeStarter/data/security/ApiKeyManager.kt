package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.BuildConfig

class ApiKeyManager(
    context: Context,
    private val keyStoreManager: KeyStoreManager = KeyStoreManager(),
) {

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Initializes API key encryption on first launch.
     * Encrypts BuildConfig.GEMINI_API_KEY using Android Keystore AES-256-GCM.
     */
    fun initializeApiKeyIfNeeded(apiKeyToEncrypt: String = BuildConfig.GEMINI_API_KEY) {
        if (hasEncryptedKey() || apiKeyToEncrypt.isBlank()) return

        try {
            val (cipherText, iv) = keyStoreManager.encrypt(apiKeyToEncrypt)
            val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            sharedPreferences.edit {
                putString(KEY_CIPHERTEXT, cipherTextBase64)
                putString(KEY_IV, ivBase64)
            }
        } catch (_: Exception) {
            // Do not log key or exceptions that might leak plaintext
        }
    }

    /**
     * Decrypts the stored API key in-memory.
     * Never exposes or logs the decrypted string.
     */
    fun getApiKey(): String {
        val cipherTextBase64 = sharedPreferences.getString(KEY_CIPHERTEXT, null)
        val ivBase64 = sharedPreferences.getString(KEY_IV, null)

        if (!cipherTextBase64.isNullOrEmpty() && !ivBase64.isNullOrEmpty()) {
            try {
                val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val decryptedKey = keyStoreManager.decrypt(cipherText, iv)
                if (decryptedKey.isNotBlank()) {
                    return decryptedKey
                }
            } catch (_: Exception) {
                // If decryption fails, fallback
            }
        }

        // Fallback or initial encryption trigger
        val rawConfigKey = BuildConfig.GEMINI_API_KEY
        if (rawConfigKey.isNotBlank()) {
            initializeApiKeyIfNeeded(rawConfigKey)
        }
        return rawConfigKey
    }

    private fun hasEncryptedKey(): Boolean {
        return sharedPreferences.contains(KEY_CIPHERTEXT) && sharedPreferences.contains(KEY_IV)
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "secure_api_key_prefs"
        private const val FALLBACK_PREFS_NAME = "fallback_api_key_prefs"
        private const val KEY_CIPHERTEXT = "encrypted_gemini_api_key"
        private const val KEY_IV = "gemini_api_key_iv"
    }
}
