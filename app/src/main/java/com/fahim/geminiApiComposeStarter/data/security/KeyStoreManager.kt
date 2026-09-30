package com.fahim.geminiApiComposeStarter.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeyStoreManager {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply {
        load(null)
    }

    fun getOrCreateSecretKey(): SecretKey {
        if (keyStore.containsAlias(ALIAS_NAME)) {
            val entry = keyStore.getEntry(ALIAS_NAME, null) as? KeyStore.SecretKeyEntry
            if (entry?.secretKey != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_TYPE,
        )

        val spec = KeyGenParameterSpec.Builder(
            ALIAS_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptData(plainText: String): Pair<ByteArray, ByteArray> {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(encryptedBytes, iv)
    }

    fun decryptData(cipherText: ByteArray, iv: ByteArray): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val KEYSTORE_TYPE = "AndroidKeyStore"
        private const val ALIAS_NAME = "TanayGeminiKeyAlias"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
