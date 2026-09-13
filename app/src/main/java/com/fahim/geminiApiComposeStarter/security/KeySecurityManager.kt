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
 * Encrypted payload containing base64 encoded ciphertext and initialization vector (IV).
 */
data class EncryptedData(
    val ciphertextBase64: String,
    val ivBase64: String,
)

/**
 * Security Manager providing AES-256-GCM encryption and decryption backed by Android KeyStore.
 * Ensures the Gemini API key is encrypted before persisting and decrypted only in memory when needed.
 */
class KeySecurityManager {

    private val provider = "AndroidKeyStore"
    private val keyAlias = "GeminiApiKeyStoreAlias"
    private val transformation = "AES/GCM/NoPadding"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(provider).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (keyStore.containsAlias(keyAlias)) {
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            provider
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts the provided plaintext using AES-256-GCM.
     * Returns an [EncryptedData] holding Base64-encoded ciphertext and IV.
     */
    fun encrypt(plainText: String): EncryptedData? {
        if (plainText.isBlank()) return null
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            EncryptedData(
                ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decrypts AES-256-GCM encrypted data using the Android KeyStore key.
     * Returns the plaintext string only in memory, or null on failure.
     */
    fun decrypt(encryptedData: EncryptedData): String? {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            val cipherBytes = Base64.decode(encryptedData.ciphertextBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(encryptedData.ivBase64, Base64.NO_WRAP)
            val gcmSpec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
