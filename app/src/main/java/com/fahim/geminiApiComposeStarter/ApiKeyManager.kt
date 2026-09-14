package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "GeminiApiKeyEncryptionKey"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

private const val PREFS_NAME = "secure_api_key"
private const val ENCRYPTED_KEY = "encrypted_key"
private const val IV_KEY = "iv"

class ApiKeyManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(KEY_ALIAS, null)

        if (existingKey is SecretKey) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance("AES", KEYSTORE_PROVIDER)

        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )

        return keyGenerator.generateKey()
    }

    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encrypted = cipher.doFinal(
            apiKey.toByteArray(StandardCharsets.UTF_8)
        )

        val iv = cipher.iv

        preferences.edit()
            .putString(
                ENCRYPTED_KEY,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            )
            .putString(
                IV_KEY,
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    fun getApiKey(): String {
        val encryptedString =
            preferences.getString(ENCRYPTED_KEY, null)

        val ivString =
            preferences.getString(IV_KEY, null)

        if (
            encryptedString.isNullOrBlank() ||
            ivString.isNullOrBlank()
        ) {
            return ""
        }

        val encrypted =
            Base64.decode(encryptedString, Base64.NO_WRAP)

        val iv =
            Base64.decode(ivString, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, iv)
        )

        return String(
            cipher.doFinal(encrypted),
            StandardCharsets.UTF_8
        )
    }
}