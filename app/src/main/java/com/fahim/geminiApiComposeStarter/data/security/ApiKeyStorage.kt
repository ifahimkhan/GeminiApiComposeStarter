package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// Declare this once, outside the class.
private val Context.apiKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)

class ApiKeyStorage(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private const val KEY_ALIAS = "gemini_api_encryption_key"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private val CIPHERTEXT = stringPreferencesKey("ciphertext")
        private val IV = stringPreferencesKey("iv")

        // Shared across instances to avoid simultaneous key creation.
        private val mutex = Mutex()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )

        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(specification)
        return generator.generateKey()
    }

    /**
     * Call during app initialisation.
     * Encrypts the configured key with a fresh random IV.
     * Also replaces a previously stored API key after key rotation.
     */
    suspend fun saveApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) {
            "GEMINI_API_KEY is missing. Configure it and rebuild."
        }

        withContext(Dispatchers.IO) {
            mutex.withLock {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    getOrCreateSecretKey()
                )

                val plaintext = apiKey.toByteArray(Charsets.UTF_8)

                val encrypted = try {
                    cipher.doFinal(plaintext)
                } finally {
                    plaintext.fill(0)
                }

                appContext.apiKeyDataStore.edit { preferences ->
                    preferences[CIPHERTEXT] = Base64.encodeToString(
                        encrypted,
                        Base64.NO_WRAP
                    )
                    preferences[IV] = Base64.encodeToString(
                        cipher.iv,
                        Base64.NO_WRAP
                    )
                }
            }
        }
    }

    /**
     * Call only when constructing the Gemini model.
     * Never log or display the returned value.
     */
    suspend fun decryptApiKey(): String =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val preferences = appContext.apiKeyDataStore.data.first()

                val encryptedText = preferences[CIPHERTEXT]
                    ?: error("Encrypted API key is not available.")

                val encodedIv = preferences[IV]
                    ?: error("API key encryption IV is missing.")

                val keyStore = KeyStore.getInstance(KEYSTORE).apply {
                    load(null)
                }

                val secretKey =
                    keyStore.getKey(KEY_ALIAS, null) as? SecretKey
                        ?: error("API key encryption key is unavailable.")

                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    GCMParameterSpec(
                        128,
                        Base64.decode(encodedIv, Base64.NO_WRAP)
                    )
                )

                val plaintext = cipher.doFinal(
                    Base64.decode(encryptedText, Base64.NO_WRAP)
                )

                try {
                    String(plaintext, Charsets.UTF_8)
                } finally {
                    plaintext.fill(0)
                }
            }
        }
}