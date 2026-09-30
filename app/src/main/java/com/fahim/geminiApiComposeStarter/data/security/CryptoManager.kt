package com.fahim.geminiApiComposeStarter.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "CryptoManager"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "gemini_api_key_alias"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_SIZE = 256
private const val GCM_TAG_LENGTH = 128

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
)

/**
 * Manages AES-256-GCM encryption and decryption backed by AndroidKeyStore.
 */
open class CryptoManager {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateSecretKey()
    }

    open fun generateSecretKey(): SecretKey {
        keyStore.deleteEntry(KEY_ALIAS)
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    open fun removeKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing KeyStore alias", e)
        }
    }

    /**
     * Encrypts plaintext bytes using a fresh IV with AES-256-GCM.
     */
    open fun encrypt(plaintext: String): EncryptedPayload {
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
     * Decrypts ciphertext with the provided IV.
     * Throws security exceptions on authentication failure, invalid key, or corrupted payload.
     */
    @Throws(
        AEADBadTagException::class,
        java.security.InvalidKeyException::class,
        java.security.GeneralSecurityException::class,
        IllegalArgumentException::class,
    )
    open fun decrypt(ciphertextBase64: String, ivBase64: String): String {
        val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: throw java.security.InvalidKeyException("Keystore key not found for alias $KEY_ALIAS")

        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
