package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages encryption and decryption of sensitive secrets (such as the Gemini API key)
 * at rest using AES-256-GCM backed by the hardware-backed Android KeyStore provider.
 */
class KeystoreCryptoManager(
    private val keyAlias: String = KEY_ALIAS,
) {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    init {
        getOrCreateSecretKey()
    }

    /**
     * Retrieves the existing AES key from Android KeyStore or generates a new one.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the given plaintext using AES-256-GCM.
     * Returns a pair of Base64-encoded (ciphertext, initializationVector).
     */
    fun encrypt(plaintext: String): EncryptedPayload {
        if (plaintext.isEmpty()) return EncryptedPayload("", "")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts the given ciphertext using the stored AES-256-GCM key and IV.
     * Returns the plaintext string in memory only.
     */
    fun decrypt(encryptedPayload: EncryptedPayload): String {
        if (encryptedPayload.ciphertextBase64.isEmpty() || encryptedPayload.ivBase64.isEmpty()) {
            return ""
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey()
        val iv = Base64.decode(encryptedPayload.ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(encryptedPayload.ciphertextBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_keystore_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}

/**
 * Container holding encrypted ciphertext and its initialization vector (IV).
 */
data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
)
