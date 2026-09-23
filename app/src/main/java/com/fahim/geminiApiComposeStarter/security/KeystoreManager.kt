package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages master key generation and retrieval inside the hardware-backed Android Keystore.
 *
 * Requirements satisfied:
 * - Hardware/TEE backed Android Keystore provider ("AndroidKeyStore").
 * - AES-256 with GCM block mode and NoPadding.
 * - Key material never leaves the secure keystore subsystem.
 */
object KeystoreManager {

    private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "GeminiApiKeyStoreAlias"
    private const val KEY_SIZE_BITS = 256

    /**
     * Retrieves existing AES-256 secret key from Android Keystore, or creates a new one.
     */
    fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey?.let { return it }
        }

        return generateSecretKey()
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER,
        )

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(false) // We supply our own cryptographically secure IV in AesGcmCipher
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
}
