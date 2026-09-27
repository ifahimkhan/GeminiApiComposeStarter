package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the Gemini API key with a 256-bit AES-GCM key that is generated and kept entirely
 * inside the Android Keystore (the raw key material never leaves hardware-backed storage).
 *
 * Only the ciphertext and the IV needed to decrypt it are written to disk (SharedPreferences).
 * The plaintext API key is only ever held in memory, and only for as long as it takes to build
 * the Gemini [com.google.ai.client.generativeai.GenerativeModel] — it is never logged, displayed,
 * or included in any user-visible message.
 */
class ApiKeyManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val secretKey: SecretKey
        get() = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: generateKey()

    private fun generateKey(): SecretKey {
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

    private fun persistEncrypted(plainKey: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val cipherText = cipher.doFinal(plainKey.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(PREF_CIPHERTEXT, Base64.encodeToString(cipherText, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Returns the usable API key. On first run (or if [freshPlainKey] from local.properties has
     * changed since the last run) it encrypts and persists [freshPlainKey]. On subsequent runs it
     * decrypts the value that was already stored, demonstrating that only encrypted data is kept
     * at rest.
     */
    fun getDecryptedKey(freshPlainKey: String): String {
        if (freshPlainKey.isBlank()) return ""

        val storedCipherText = prefs.getString(PREF_CIPHERTEXT, null)
        val storedIv = prefs.getString(PREF_IV, null)

        if (storedCipherText == null || storedIv == null) {
            persistEncrypted(freshPlainKey)
            return freshPlainKey
        }

        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(storedIv, Base64.NO_WRAP)),
                )
            }
            val decrypted = cipher.doFinal(Base64.decode(storedCipherText, Base64.NO_WRAP))
            val decryptedText = String(decrypted, Charsets.UTF_8)
            if (decryptedText == freshPlainKey) {
                decryptedText
            } else {
                // The key in local.properties changed since the last run — re-encrypt the new one.
                persistEncrypted(freshPlainKey)
                freshPlainKey
            }
        } catch (e: Exception) {
            // Stored ciphertext is unreadable (e.g. the Keystore entry was cleared). Re-encrypt
            // from the source of truth rather than crashing the app.
            persistEncrypted(freshPlainKey)
            freshPlainKey
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PREFS_NAME = "secure_api_key_prefs"
        private const val PREF_CIPHERTEXT = "encrypted_api_key"
        private const val PREF_IV = "encrypted_api_key_iv"
    }
}
