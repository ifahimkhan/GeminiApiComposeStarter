package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoKeyManager {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private fun obtainSecretKey(): SecretKey {
        val existingEntry = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry?.secretKey != null) {
            return existingEntry.secretKey
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    fun encryptSecret(rawText: String): EncryptedPayload {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, obtainSecretKey())
        val encryptedBytes = cipher.doFinal(rawText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(cipherText = encryptedBytes, iv = cipher.iv)
    }

    fun decryptSecret(payload: EncryptedPayload): String {
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        val gcmSpec = GCMParameterSpec(GCM_TAG_BITS, payload.iv)
        cipher.init(Cipher.DECRYPT_MODE, obtainSecretKey(), gcmSpec)
        val plainBytes = cipher.doFinal(payload.cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    data class EncryptedPayload(
        val cipherText: ByteArray,
        val iv: ByteArray,
    )

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val ALIAS = "GeminiSecretKeyAlias"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
    }
}
