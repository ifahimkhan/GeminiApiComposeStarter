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

    private val preferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun initializeApiKeyIfNeeded(rawKey: String = BuildConfig.GEMINI_API_KEY) {
        if (isKeyStored() || rawKey.isBlank()) return

        try {
            val (encryptedBytes, iv) = keyStoreManager.encrypt(rawKey)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            preferences.edit {
                putString(KEY_CIPHER, cipherBase64)
                putString(KEY_IV, ivBase64)
            }
        } catch (_: Exception) {
            // Silently fallback without exposing sensitive details
        }
    }

    fun getApiKey(): String {
        val cipherBase64 = preferences.getString(KEY_CIPHER, null)
        val ivBase64 = preferences.getString(KEY_IV, null)

        if (!cipherBase64.isNullOrEmpty() && !ivBase64.isNullOrEmpty()) {
            try {
                val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
                val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
                val decrypted = keyStoreManager.decrypt(cipherBytes, ivBytes)
                if (decrypted.isNotBlank()) {
                    return decrypted
                }
            } catch (_: Exception) {
                // Ignore and return fallback below
            }
        }

        val configKey = BuildConfig.GEMINI_API_KEY
        if (configKey.isNotBlank()) {
            initializeApiKeyIfNeeded(configKey)
        }
        return configKey
    }

    private fun isKeyStored(): Boolean {
        return preferences.contains(KEY_CIPHER) && preferences.contains(KEY_IV)
    }

    companion object {
        private const val PREFERENCES_NAME = "encrypted_api_prefs"
        private const val FALLBACK_PREFS_NAME = "api_key_fallback_prefs"
        private const val KEY_CIPHER = "cipher_key"
        private const val KEY_IV = "iv_key"
    }
}
