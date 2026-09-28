package com.fahim.geminiApiComposeStarter.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles AES-256-GCM encryption and decryption of sensitive API keys using the hardware-backed
 * Android KeyStore system.
 *
 * Requirements satisfied:
 * - Master key generated and securely isolated within Android KeyStore
 * - AES-256-GCM authenticated encryption with KeyGenParameterSpec
 * - Only ciphertext and IV are persisted at rest
 * - Plaintext API key is decrypted strictly in memory at the exact moment of GenerativeModel initialization
 * - Zero logging, displaying, or persistence of the plaintext key
 */
class ApiKeyManager(
    private val keyAlias: String = KEY_ALIAS,
) {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        ensureMasterKeyExists()
    }

    /**
     * Ensures an AES-256-GCM master key exists in the Android KeyStore.
     * Generates a new key pair spec if not already created.
     */
    private fun ensureMasterKeyExists() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER,
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("KeyStore entry for alias $keyAlias not found")
        return entry.secretKey
    }

    /**
     * Encrypts the plaintext API key using AES-256-GCM.
     * Returns a pair of Base64-encoded strings: (ciphertextBase64, ivBase64).
     */
    fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        return Pair(cipherTextBase64, ivBase64)
    }

    /**
     * Decrypts the ciphertext using the stored IV and KeyStore master key.
     * The result is held strictly in memory and never persisted or logged.
     */
    fun decrypt(cipherTextBase64: String, ivBase64: String): String {
        val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiApiKeyAlias_N141"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
