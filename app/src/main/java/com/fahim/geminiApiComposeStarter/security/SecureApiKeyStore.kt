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

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "gemini_api_key_aes"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128
private const val DATASTORE_NAME = "secure_api_key"

private val Context.secureApiKeyDataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

class SecureApiKeyStore(
    private val context: Context,
) {

    private val encryptedApiKeyKey =
        stringPreferencesKey("encrypted_api_key")

    suspend fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return

        val encryptedValue = encrypt(apiKey)

        context.secureApiKeyDataStore.edit { preferences ->
            preferences[encryptedApiKeyKey] = encryptedValue
        }
    }

    suspend fun getApiKey(): String? {
        val encryptedValue =
            context.secureApiKeyDataStore.data.first()[encryptedApiKeyKey]
                ?: return null

        return decrypt(encryptedValue)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            "AES",
            KEYSTORE_PROVIDER
        )

        val keyGenParameterSpec =
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()

        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val iv = cipher.iv
        val encryptedBytes =
            cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

        // Store IV + ciphertext together.
        val combined = ByteArray(
            iv.size + encryptedBytes.size
        )

        System.arraycopy(
            iv,
            0,
            combined,
            0,
            iv.size
        )

        System.arraycopy(
            encryptedBytes,
            0,
            combined,
            iv.size,
            encryptedBytes.size
        )

        return Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(
            value,
            Base64.NO_WRAP
        )

        val ivSize = 12

        require(combined.size > ivSize) {
            "Invalid encrypted API key"
        }

        val iv = combined.copyOfRange(
            0,
            ivSize
        )

        val encryptedBytes = combined.copyOfRange(
            ivSize,
            combined.size
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )
        )

        return String(
            cipher.doFinal(encryptedBytes),
            StandardCharsets.UTF_8
        )
    }
}