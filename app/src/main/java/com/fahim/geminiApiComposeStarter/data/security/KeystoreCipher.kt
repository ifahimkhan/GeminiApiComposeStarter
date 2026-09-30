package com.fahim.geminiApiComposeStarter.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_SIZE_BITS = 256
private const val IV_SIZE_BYTES = 12
private const val TAG_SIZE_BITS = 128

/**
 * AES-256-GCM backed by a key that lives inside the Android Keystore. The raw key material
 * never enters app memory, so the ciphertext is useless if copied off the device.
 *
 * Blob layout: `IV (12 bytes) || ciphertext+tag`.
 */
class KeystoreCipher(private val alias: String = "gemini_api_key_aes") : SecretCipher {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        // The Keystore generates a fresh random IV for every encryption.
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return cipher.iv + cipher.doFinal(plaintext)
    }

    override fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > IV_SIZE_BYTES) { "Ciphertext too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = blob.copyOfRange(0, IV_SIZE_BYTES)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        return cipher.doFinal(blob, IV_SIZE_BYTES, blob.size - IV_SIZE_BYTES)
    }

    private fun secretKey(): SecretKey =
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: generateKey()

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
