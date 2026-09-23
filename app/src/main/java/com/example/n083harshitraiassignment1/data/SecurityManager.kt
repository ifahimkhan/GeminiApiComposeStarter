package com.example.n083harshitraiassignment1.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityManager {

    companion object {

        private const val KEYSTORE_NAME =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "GeminiApiKeyEncryptionKey"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"
    }

    private fun getOrCreateKey(): SecretKey {

        val keyStore = KeyStore.getInstance(
            KEYSTORE_NAME
        )

        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {

            val entry = keyStore.getEntry(
                KEY_ALIAS,
                null
            ) as KeyStore.SecretKeyEntry

            return entry.secretKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_NAME
            )

        val keySpec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    fun encrypt(
        plainText: String
    ): String {

        val cipher =
            Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                plainText.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val iv = cipher.iv

        val combined =
            iv + encryptedBytes

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    fun decrypt(
        encryptedText: String
    ): String {

        val combined =
            Base64.decode(
                encryptedText,
                Base64.NO_WRAP
            )

        val iv =
            combined.copyOfRange(
                0,
                12
            )

        val encryptedBytes =
            combined.copyOfRange(
                12,
                combined.size
            )

        val cipher =
            Cipher.getInstance(TRANSFORMATION)

        val parameterSpec =
            GCMParameterSpec(
                128,
                iv
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            parameterSpec
        )

        val decrypted =
            cipher.doFinal(
                encryptedBytes
            )

        return String(
            decrypted,
            StandardCharsets.UTF_8
        )
    }
}