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
 * Data structure holding AES-256-GCM ciphertext and the associated initialization vector (IV).
 */
data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
)

/**
 * Android Keystore manager providing hardware-backed AES-256-GCM encryption at rest.
 *
 * Requirements satisfied:
 * - Generates key inside Android Keystore using [KeyGenParameterSpec]
 * - AES-256 key size
 * - GCM block mode with NoPadding (AEAD)
 * - Decrypts strictly in-memory
 * - Never logs or exposes plaintext keys
 */
class CryptoManager(
    private val keyAlias: String = KEY_ALIAS,
) {

    companion object {
        const val KEY_ALIAS = "gemini_api_keystore_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_SIZE = 256
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    /**
     * Retrieves the existing SecretKey from Android Keystore or generates a new AES-256 key.
     */
    fun getOrCreateSecretKey(): SecretKey {
        return try {
            val existingKey = keyStore.getKey(keyAlias, null) as? SecretKey
            if (existingKey != null) {
                existingKey
            } else {
                generateSecretKey()
            }
        } catch (_: Exception) {
            generateSecretKey()
        }
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the provided plaintext using AES-256-GCM.
     *
     * @param plaintext Sensitive string to encrypt.
     * @return [EncryptedPayload] containing Base64 encoded ciphertext and IV.
     */
    fun encrypt(plaintext: String): EncryptedPayload {
        if (plaintext.isEmpty()) {
            return EncryptedPayload(ciphertextBase64 = "", ivBase64 = "")
        }
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
        )
    }

    /**
     * Decrypts the [EncryptedPayload] back to plaintext in memory.
     *
     * @param payload Base64 encoded ciphertext and IV.
     * @return Decrypted plaintext string.
     */
    fun decrypt(payload: EncryptedPayload): String {
        if (payload.ciphertextBase64.isBlank() || payload.ivBase64.isBlank()) {
            return ""
        }
        val ciphertext = Base64.decode(payload.ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
