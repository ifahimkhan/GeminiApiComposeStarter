package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class SecureApiKeyManager(
    private val context: Context,
) {

    companion object {
        private const val KEY_ALIAS = "gemini_api_key_aes_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private val ENCRYPTED_API_KEY =
            stringPreferencesKey("encrypted_api_key")

        private val IV =
            stringPreferencesKey("api_key_iv")
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (keyStore.getKey(KEY_ALIAS, null) as SecretKey)
        }

        val keyGenerator = KeyGenerator.getInstance(
            "AES",
            ANDROID_KEYSTORE
        )

        val keySpec = android.security.keystore.KeyGenParameterSpec.Builder(
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

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    suspend fun storeApiKey(apiKey: String) {
        if (apiKey.isBlank()) return

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes = cipher.doFinal(apiKey.toByteArray())
        val ivBytes = cipher.iv

        context.apiKeyDataStore.edit { preferences ->
            preferences[ENCRYPTED_API_KEY] =
                Base64.encodeToString(
                    encryptedBytes,
                    Base64.NO_WRAP
                )

            preferences[IV] =
                Base64.encodeToString(
                    ivBytes,
                    Base64.NO_WRAP
                )
        }
    }

    suspend fun getApiKey(): String? {
        val preferences = context.apiKeyDataStore.data.first()

        val encryptedValue =
            preferences[ENCRYPTED_API_KEY] ?: return null

        val ivValue =
            preferences[IV] ?: return null

        val encryptedBytes =
            Base64.decode(encryptedValue, Base64.NO_WRAP)

        val ivBytes =
            Base64.decode(ivValue, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, ivBytes)
        )

        return cipher
            .doFinal(encryptedBytes)
            .toString(Charsets.UTF_8)
    }
}