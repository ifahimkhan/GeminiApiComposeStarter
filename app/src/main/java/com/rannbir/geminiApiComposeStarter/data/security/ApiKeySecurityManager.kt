package com.rannbir.geminiApiComposeStarter.data.security

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
 * Manages encryption at rest for the Gemini API Key using hardware-backed
 * Android Keystore with AES-256-GCM.
 *
 * The raw API key is encrypted on first launch, stored only as ciphertext and IV,
 * and decrypted in-memory strictly when GenerativeModel is instantiated.
 */
class ApiKeySecurityManager(private val context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyGenerated()
    }

    private fun ensureKeyGenerated() {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts the provided raw API key and persists only the ciphertext and IV.
     */
    @Synchronized
    fun encryptAndStoreApiKey(rawKey: String) {
        if (rawKey.isBlank()) return

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(rawKey.toByteArray(Charsets.UTF_8))

        val encodedCiphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)

        sharedPreferences.edit()
            .putString(KEY_CIPHERTEXT, encodedCiphertext)
            .putString(KEY_IV, encodedIv)
            .apply()
    }

    /**
     * Decrypts the API key in memory. Never logs or exposes the returned value.
     */
    @Synchronized
    fun getDecryptedApiKey(): String? {
        val encodedCiphertext = sharedPreferences.getString(KEY_CIPHERTEXT, null)
        val encodedIv = sharedPreferences.getString(KEY_IV, null)

        if (encodedCiphertext == null || encodedIv == null) {
            return null
        }

        return try {
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasStoredKey(): Boolean {
        return sharedPreferences.contains(KEY_CIPHERTEXT) && sharedPreferences.contains(KEY_IV)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiKeystoreKey_N087"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "gemini_secure_vault_prefs"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_ciphertext"
        private const val KEY_IV = "encrypted_api_key_iv"
    }
}
