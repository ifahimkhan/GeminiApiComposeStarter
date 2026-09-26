package com.fahim.geminiApiComposeStarter.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

class SecureApiKeyManager(
    private val userPreferences: UserPreferences
) {

    companion object {

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "GeminiApiComposeStarterApiKey"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128
    }

    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
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
                    KeyProperties
                        .ENCRYPTION_PADDING_NONE
                )
                .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private fun encrypt(
        plainText: String
    ): String {

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                plainText.toByteArray(
                    Charsets.UTF_8
                )
            )

        val iv = cipher.iv

        val combined =
            ByteBuffer
                .allocate(
                    4 +
                            iv.size +
                            encryptedBytes.size
                )
                .putInt(iv.size)
                .put(iv)
                .put(encryptedBytes)
                .array()

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(
        encryptedText: String
    ): String {

        val combined =
            Base64.decode(
                encryptedText,
                Base64.NO_WRAP
            )

        val buffer =
            ByteBuffer.wrap(combined)

        val ivLength =
            buffer.int

        require(ivLength > 0) {
            "Invalid encrypted API key."
        }

        val iv =
            ByteArray(ivLength)

        buffer.get(iv)

        val encryptedBytes =
            ByteArray(buffer.remaining())

        buffer.get(encryptedBytes)

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        val parameterSpec =
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            parameterSpec
        )

        return String(
            cipher.doFinal(
                encryptedBytes
            ),
            Charsets.UTF_8
        )
    }

    suspend fun getOrCreateApiKey(
        bootstrapApiKey: String
    ): String {

        val encryptedKey =
            userPreferences
                .encryptedGeminiApiKey
                .first()

        if (!encryptedKey.isNullOrBlank()) {
            return decrypt(encryptedKey)
        }

        if (bootstrapApiKey.isBlank()) {
            throw IllegalStateException(
                "Gemini API key is not configured."
            )
        }

        val encryptedBootstrapKey =
            encrypt(bootstrapApiKey)

        userPreferences
            .saveEncryptedGeminiApiKey(
                encryptedBootstrapKey
            )

        return bootstrapApiKey
    }
}
