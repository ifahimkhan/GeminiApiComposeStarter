package com.fahim.geminiApiComposeStarter.security

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

private val Context.secureApiKeyDataStore by preferencesDataStore(
    name = "secure_api_key_store"
)

class SecureApiKeyManager(
    private val context: Context
) {

    companion object {
        private const val KEY_ALIAS = "gemini_api_key_aes"

        private val ENCRYPTED_API_KEY =
            stringPreferencesKey("encrypted_gemini_api_key")

        private val API_KEY_IV =
            stringPreferencesKey("gemini_api_key_iv")
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey =
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    suspend fun storeApiKeyIfNeeded(apiKey: String) {
        if (apiKey.isBlank()) return

        val preferences =
            context.secureApiKeyDataStore.data.first()

        val existingCiphertext =
            preferences[ENCRYPTED_API_KEY]

        val existingIv =
            preferences[API_KEY_IV]

        if (
            !existingCiphertext.isNullOrBlank() &&
            !existingIv.isNullOrBlank()
        ) {
            return
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes = cipher.doFinal(
            apiKey.toByteArray(Charsets.UTF_8)
        )

        val encryptedBase64 = Base64.encodeToString(
            encryptedBytes,
            Base64.NO_WRAP
        )

        val ivBase64 = Base64.encodeToString(
            cipher.iv,
            Base64.NO_WRAP
        )

        context.secureApiKeyDataStore.edit { prefs ->
            prefs[ENCRYPTED_API_KEY] = encryptedBase64
            prefs[API_KEY_IV] = ivBase64
        }
    }

    suspend fun getDecryptedApiKey(): String? {
        val preferences =
            context.secureApiKeyDataStore.data.first()

        val encryptedBase64 =
            preferences[ENCRYPTED_API_KEY]
                ?: return null

        val ivBase64 =
            preferences[API_KEY_IV]
                ?: return null

        val encryptedBytes = Base64.decode(
            encryptedBase64,
            Base64.NO_WRAP
        )

        val iv = Base64.decode(
            ivBase64,
            Base64.NO_WRAP
        )

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, iv)
        )

        val decryptedBytes =
            cipher.doFinal(encryptedBytes)

        return String(
            decryptedBytes,
            Charsets.UTF_8
        )
    }
}