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
 * Manages secure AES-256-GCM encryption and decryption of the Gemini API key
 * using the Android Keystore system.
 *
 * - Plaintext API keys are never persisted to disk.
 * - The 256-bit AES master key is generated and stored inside the Android Keystore.
 * - Ciphertext and random IVs are stored in private SharedPreferences.
 * - Decryption occurs strictly in memory when required by the client.
 */
class ApiKeySecurityManager(
    private val context: Context,
) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves the existing 256-bit AES master key from Android Keystore,
     * or generates a new one if it does not already exist.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )

        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the provided plaintext API key using AES-256-GCM with a fresh 12-byte IV.
     * Persists ONLY the Base64-encoded IV and ciphertext into private SharedPreferences.
     */
    fun saveEncryptedApiKey(plaintextKey: String): Boolean {
        if (plaintextKey.isBlank()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv ?: return false
            val ciphertext = cipher.doFinal(plaintextKey.toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            sharedPreferences.edit()
                .putString(KEY_IV, ivBase64)
                .putString(KEY_CIPHERTEXT, ciphertextBase64)
                .commit()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Decrypts the stored ciphertext using the Android Keystore AES-256-GCM key.
     * Returns the plaintext key solely in memory, or null if decryption fails or data is missing.
     */
    fun getDecryptedApiKey(): String? {
        val ivBase64 = sharedPreferences.getString(KEY_IV, null) ?: return null
        val ciphertextBase64 = sharedPreferences.getString(KEY_CIPHERTEXT, null) ?: return null

        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

            if (iv.size != GCM_IV_LENGTH_BYTES) {
                return null
            }

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if an encrypted API key is already present in persistent storage.
     */
    fun hasStoredApiKey(): Boolean {
        return sharedPreferences.contains(KEY_IV) && sharedPreferences.contains(KEY_CIPHERTEXT)
    }

    /**
     * Resolves the API key:
     * 1. If an encrypted API key already exists in storage, decrypts and returns it.
     * 2. If none exists and [apiKeyFromBuildConfig] is non-empty, encrypts and persists
     *    the encrypted representation, then returns the plaintext in memory.
     * 3. If neither exists, returns null without writing invalid data.
     */
    fun getOrSaveApiKey(apiKeyFromBuildConfig: String?): String? {
        val existing = getDecryptedApiKey()
        if (existing != null) {
            return existing
        }

        if (!apiKeyFromBuildConfig.isNullOrBlank()) {
            val saved = saveEncryptedApiKey(apiKeyFromBuildConfig)
            if (saved) {
                return apiKeyFromBuildConfig
            }
        }

        return null
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12

        private const val PREFS_NAME = "gemini_secure_key_prefs"
        private const val KEY_IV = "encrypted_api_key_iv"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_ciphertext"
    }
}
