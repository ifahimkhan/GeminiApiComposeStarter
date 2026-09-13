package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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

private val Context.secureDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class SecureApiKeyStorage(
    private val context: Context
) {

    private companion object {
        const val KEY_ALIAS = "gemini_api_key_key"

        val ENCRYPTED_KEY = stringPreferencesKey("encrypted_api_key")
        val IV_KEY = stringPreferencesKey("api_key_iv")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    suspend fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            return
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val encryptedBytes = cipher.doFinal(
            apiKey.toByteArray(Charsets.UTF_8)
        )

        val encryptedApiKey = Base64.encodeToString(
            encryptedBytes,
            Base64.NO_WRAP
        )

        val iv = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        context.secureDataStore.edit { preferences ->
            preferences[ENCRYPTED_KEY] = encryptedApiKey
            preferences[IV_KEY] = iv
        }
    }

    suspend fun getApiKey(): String? {
        val preferences = context.secureDataStore.data.first()

        val encryptedApiKey =
            preferences[ENCRYPTED_KEY] ?: return null

        val iv =
            preferences[IV_KEY] ?: return null

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(
                    128,
                    Base64.decode(iv, Base64.NO_WRAP)
                )
            )

            val decryptedBytes = cipher.doFinal(
                Base64.decode(
                    encryptedApiKey,
                    Base64.NO_WRAP
                )
            )

            String(
                decryptedBytes,
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            null
        }
    }
}