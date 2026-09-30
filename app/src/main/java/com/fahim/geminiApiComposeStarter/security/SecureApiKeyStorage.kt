package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SecureApiKeyStorage(
    private val context: Context,
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiApiKeyEncryptionKey"

        private const val PREFS_NAME = "secure_api_key"
        private const val ENCRYPTED_KEY = "encrypted_key"
        private const val IV_KEY = "encryption_iv"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH = 128
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }

        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                "AES",
                KEYSTORE_PROVIDER,
            )

        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM,
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE,
                )
                .build(),
        )

        return keyGenerator.generateKey()
    }

    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return

        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
        )

        val encrypted =
            cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        preferences.edit()
            .putString(
                ENCRYPTED_KEY,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP,
                ),
            )
            .putString(
                IV_KEY,
                Base64.encodeToString(
                    cipher.iv,
                    Base64.NO_WRAP,
                ),
            )
            .apply()
    }

    fun getApiKey(): String? {
        val encryptedString =
            preferences.getString(
                ENCRYPTED_KEY,
                null,
            ) ?: return null

        val ivString =
            preferences.getString(
                IV_KEY,
                null,
            ) ?: return null

        val encrypted =
            Base64.decode(
                encryptedString,
                Base64.NO_WRAP,
            )

        val iv =
            Base64.decode(
                ivString,
                Base64.NO_WRAP,
            )

        val cipher =
            Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv,
            ),
        )

        return String(
            cipher.doFinal(encrypted),
            Charsets.UTF_8,
        )
    }
}