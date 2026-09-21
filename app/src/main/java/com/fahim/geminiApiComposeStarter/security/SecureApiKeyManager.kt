package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

private val Context.secureApiKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class SecureApiKeyManager(
    private val context: Context
) {

    companion object {

        private const val KEYSTORE_PROVIDER =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "GeminiApiKeyEncryptionKey"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128

        private val ENCRYPTED_KEY =
            stringPreferencesKey(
                "encrypted_api_key"
            )

        private val IV_KEY =
            stringPreferencesKey(
                "api_key_iv"
            )
    }

    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                KEYSTORE_PROVIDER
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            )

        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

        val keySpec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private fun encrypt(
        plainText: String
    ): Pair<String, String> {

        val secretKey =
            getOrCreateSecretKey()

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey
        )

        val encryptedBytes =
            cipher.doFinal(
                plainText.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val encryptedText =
            Base64.encodeToString(
                encryptedBytes,
                Base64.NO_WRAP
            )

        val iv =
            Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP
            )

        return Pair(
            encryptedText,
            iv
        )
    }

    private fun decrypt(
        encryptedText: String,
        iv: String
    ): String {

        val secretKey =
            getOrCreateSecretKey()

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        val ivBytes =
            Base64.decode(
                iv,
                Base64.NO_WRAP
            )

        val encryptedBytes =
            Base64.decode(
                encryptedText,
                Base64.NO_WRAP
            )

        val parameterSpec =
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                ivBytes
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            parameterSpec
        )

        val decryptedBytes =
            cipher.doFinal(
                encryptedBytes
            )

        return String(
            decryptedBytes,
            StandardCharsets.UTF_8
        )
    }

    suspend fun saveApiKey(
        apiKey: String
    ) {

        if (apiKey.isBlank()) {
            return
        }

        val (encryptedText, iv) =
            encrypt(apiKey)

        context.secureApiKeyDataStore.edit {
                preferences ->

            preferences[ENCRYPTED_KEY] =
                encryptedText

            preferences[IV_KEY] =
                iv
        }
    }

    suspend fun getApiKey(): String? {

        val preferences =
            context.secureApiKeyDataStore
                .data
                .first()

        val encryptedText =
            preferences[ENCRYPTED_KEY]

        val iv =
            preferences[IV_KEY]

        if (
            encryptedText.isNullOrBlank() ||
            iv.isNullOrBlank()
        ) {
            return null
        }

        return try {

            decrypt(
                encryptedText,
                iv
            )

        } catch (e: Exception) {

            null
        }
    }
}