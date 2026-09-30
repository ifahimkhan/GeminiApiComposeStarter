package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyCryptoManager {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): EncryptedData {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes = cipher.doFinal(
            plainText.toByteArray(Charsets.UTF_8)
        )

        return EncryptedData(
            cipherText = Base64.encodeToString(
                encryptedBytes,
                Base64.NO_WRAP
            ),
            iv = Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP
            )
        )
    }

    fun decrypt(encryptedData: EncryptedData): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        val ivBytes = Base64.decode(
            encryptedData.iv,
            Base64.NO_WRAP
        )

        val encryptedBytes = Base64.decode(
            encryptedData.cipherText,
            Base64.NO_WRAP
        )

        val gcmSpec = GCMParameterSpec(
            128,
            ivBytes
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            gcmSpec
        )

        val decryptedBytes = cipher.doFinal(
            encryptedBytes
        )

        return decryptedBytes.toString(Charsets.UTF_8)
    }
}

data class EncryptedData(
    val cipherText: String,
    val iv: String
)
