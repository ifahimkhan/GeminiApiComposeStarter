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
 * Manages encryption and decryption of the Gemini API key at rest using Android Keystore
 * and AES-256-GCM encryption.
 *
 * Security Architecture:
 * - A cryptographic master key is generated and stored securely inside hardware-backed Android Keystore.
 * - The API key provided at build time (or user configuration) is encrypted using AES/GCM/NoPadding.
 * - Only the resulting ciphertext and initialization vector (IV) are persisted in private SharedPreferences.
 * - Decryption occurs strictly in-memory at the instant the GenerativeModel client is instantiated.
 * - Plaintext API keys are NEVER logged, toasted, displayed, or written to Room/disk.
 */
class ApiKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiMasterKey_N157"
        private const val PREFS_NAME = "secure_gemini_prefs"
        private const val KEY_CIPHERTEXT = "encrypted_api_key"
        private const val KEY_IV = "encryption_iv"
        private const val GCM_TAG_LENGTH = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Retrieves or generates an AES-256 SecretKey in Android Keystore.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (keyEntry != null) {
                return keyEntry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the plaintext API key using AES-256-GCM and stores ciphertext + IV in private storage.
     */
    @Synchronized
    fun storeEncryptedApiKey(plainKey: String): Boolean {
        if (plainKey.isBlank()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainKey.toByteArray(Charsets.UTF_8))

            val encodedCipherText = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)

            prefs.edit()
                .putString(KEY_CIPHERTEXT, encodedCipherText)
                .putString(KEY_IV, encodedIv)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Decrypts the stored API key in memory using the Keystore key.
     * Returns null if no encrypted key is stored or decryption fails.
     */
    @Synchronized
    fun getDecryptedApiKey(): String? {
        val encodedCipherText = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val encodedIv = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val cipherText = Base64.decode(encodedCipherText, Base64.NO_WRAP)
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val secretKey = getOrCreateSecretKey()

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if an encrypted API key is currently stored.
     */
    fun hasStoredKey(): Boolean {
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    /**
     * Initializes key storage on application startup.
     * Encrypts the build-time key if present and not already persisted.
     */
    fun initializeKey(buildTimeKey: String) {
        if (buildTimeKey.isNotBlank() && !hasStoredKey()) {
            storeEncryptedApiKey(buildTimeKey)
        }
    }
}
