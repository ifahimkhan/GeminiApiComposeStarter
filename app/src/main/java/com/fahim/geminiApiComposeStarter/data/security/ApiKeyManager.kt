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

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_PREF_FILE, Context.MODE_PRIVATE)
        }
    }

    fun setupEncryptedKey(rawApiKey: String = BuildConfig.GEMINI_API_KEY) {
        if (hasStoredKey() || rawApiKey.isBlank()) return

        try {
            val (encrypted, iv) = keyStoreManager.encryptData(rawApiKey)
            val cipherBase64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            securePrefs.edit {
                putString(KEY_CIPHERTEXT, cipherBase64)
                putString(KEY_IV, ivBase64)
            }
        } catch (_: Exception) {
            // Ignore setup failures
        }
    }

    fun getApiKey(): String {
        val cipherBase64 = securePrefs.getString(KEY_CIPHERTEXT, null)
        val ivBase64 = securePrefs.getString(KEY_IV, null)

        if (!cipherBase64.isNullOrEmpty() && !ivBase64.isNullOrEmpty()) {
            try {
                val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
                val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
                val decrypted = keyStoreManager.decryptData(cipherBytes, ivBytes)
                if (decrypted.isNotBlank()) {
                    return decrypted
                }
            } catch (_: Exception) {
                // Return fallback
            }
        }

        val configKey = BuildConfig.GEMINI_API_KEY
        if (configKey.isNotBlank()) {
            setupEncryptedKey(configKey)
        }
        return configKey
    }

    private fun hasStoredKey(): Boolean {
        return securePrefs.contains(KEY_CIPHERTEXT) && securePrefs.contains(KEY_IV)
    }

    companion object {
        private const val PREF_FILE_NAME = "tanay_secure_prefs"
        private const val FALLBACK_PREF_FILE = "tanay_fallback_prefs"
        private const val KEY_CIPHERTEXT = "enc_key"
        private const val KEY_IV = "enc_iv"
    }
}
