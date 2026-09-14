package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the Gemini API key at rest using an AES-256-GCM key generated inside the
 * Android Keystore (KeyGenParameterSpec). Only the ciphertext + IV are ever persisted
 * (in a private SharedPreferences file); the plaintext key exists in memory only for the
 * instant it takes to create the [com.google.ai.client.generativeai.GenerativeModel] and
 * is never logged, toasted or displayed.
 *
 * This raises the bar against casual inspection of app storage/backups but, like any
 * client-side scheme, cannot fully hide a secret from a determined attacker with root or
 * debugger access to the device. See README.md for the production-grade alternative
 * (server-side proxy / Firebase App Check + restricted API keys).
 */
class ApiKeyStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /**
     * Ensures [plainKey] is encrypted-and-persisted, then returns it unchanged so callers can
     * use it immediately. Subsequent calls to [decryptedKey] read back the encrypted copy
     * instead of relying on [plainKey] again. No-op when [plainKey] is blank.
     */
    fun ensureEncrypted(plainKey: String): String {
        if (plainKey.isBlank()) return plainKey
        if (prefs.contains(PREF_CIPHERTEXT)) {
            // Already sealed from a previous launch; nothing further to persist.
            return plainKey
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val ciphertext = cipher.doFinal(plainKey.toByteArray(Charsets.UTF_8))
        prefs.edit {
            putString(PREF_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
        return plainKey
    }

    /** Decrypts the persisted key, or null if nothing has been sealed yet. */
    fun decryptedKey(): String? {
        val ciphertext = prefs.getString(PREF_CIPHERTEXT, null) ?: return null
        val iv = prefs.getString(PREF_IV, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
        }
        val plaintext = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
        return String(plaintext, Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PREFS_NAME = "gemini_secure_prefs"
        private const val PREF_CIPHERTEXT = "api_key_ciphertext"
        private const val PREF_IV = "api_key_iv"
    }
}
