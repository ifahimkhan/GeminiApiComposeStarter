package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles AES-256-GCM cryptographic operations backed by Android Keystore.
 * Plaintext keys are never logged or stored.
 */
class KeystoreCryptoManager(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val keyStoreProvider: String = ANDROID_KEYSTORE,
) {
    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "GeminiApiKeyKeystoreAlias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(keyStoreProvider).apply { load(null) }
    }

    /**
     * Retrieves an existing AES-256 key from Android Keystore or generates a new one.
     */
    fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            keyStoreProvider,
        )
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plaintext using AES-256-GCM with the Keystore key.
     * Generates a unique 12-byte IV per encryption operation.
     */
    fun encrypt(plaintext: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(iv = iv, ciphertext = ciphertext)
    }

    /**
     * Decrypts ciphertext using AES-256-GCM strictly in-memory.
     */
    fun decrypt(payload: EncryptedPayload): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val decryptedBytes = cipher.doFinal(payload.ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
