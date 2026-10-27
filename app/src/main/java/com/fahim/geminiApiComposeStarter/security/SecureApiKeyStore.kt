package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.BuildConfig

class SecureApiKeyStore(
    context: Context,
    private val cryptoKeyManager: CryptoKeyManager = CryptoKeyManager(),
) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            context.getSharedPreferences(FALLBACK_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    fun ensureKeyEncrypted(initialKey: String = BuildConfig.GEMINI_API_KEY) {
        if (isKeyStored() || initialKey.isBlank()) return

        try {
            val payload = cryptoKeyManager.encryptSecret(initialKey)
            prefs.edit {
                putString(PREF_CIPHER, Base64.encodeToString(payload.cipherText, Base64.NO_WRAP))
                putString(PREF_IV, Base64.encodeToString(payload.iv, Base64.NO_WRAP))
            }
        } catch (_: Exception) {
            // Never log key material or exception details
        }
    }

    fun resolveApiKeyInMemory(): String {
        val encodedCipher = prefs.getString(PREF_CIPHER, null)
        val encodedIv = prefs.getString(PREF_IV, null)

        if (!encodedCipher.isNullOrEmpty() && !encodedIv.isNullOrEmpty()) {
            try {
                val payload = CryptoKeyManager.EncryptedPayload(
                    cipherText = Base64.decode(encodedCipher, Base64.NO_WRAP),
                    iv = Base64.decode(encodedIv, Base64.NO_WRAP),
                )
                val decrypted = cryptoKeyManager.decryptSecret(payload)
                if (decrypted.isNotBlank()) {
                    return decrypted
                }
            } catch (_: Exception) {
                // Fall back to BuildConfig if keystore state was reset
            }
        }

        val buildConfigKey = BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank()) {
            ensureKeyEncrypted(buildConfigKey)
        }
        return buildConfigKey
    }

    private fun isKeyStored(): Boolean {
        return prefs.contains(PREF_CIPHER) && prefs.contains(PREF_IV)
    }

    companion object {
        private const val PREFS_FILE_NAME = "gemini_encrypted_key_store"
        private const val FALLBACK_FILE_NAME = "gemini_key_store_fallback"
        private const val PREF_CIPHER = "key_ciphertext"
        private const val PREF_IV = "key_iv"
    }
}
