package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import com.fahim.geminiApiComposeStarter.BuildConfig

import kotlinx.coroutines.flow.first

import java.nio.charset.StandardCharsets
import java.security.KeyStore

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


private val Context.secureKeyDataStore by preferencesDataStore(
    name = "secure_api_key"
)


class SecureApiKeyStore(
    private val context: Context
) {

    companion object {

        private const val KEYSTORE_NAME =
            "AndroidKeyStore"

        private const val KEY_ALIAS =
            "gemini_api_key_aes_key"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128

        private val ENCRYPTED_KEY =
            stringPreferencesKey(
                "encrypted_gemini_api_key"
            )
    }


    /*
     * Creates the AES-256 key in Android Keystore
     * if it does not already exist.
     *
     * The Gemini API key is read from BuildConfig,
     * encrypted using AES-GCM, and the resulting
     * ciphertext together with the IV is stored
     * in Preferences DataStore.
     */
    suspend fun initializeEncryptedKey() {

        val currentValue =
            context.secureKeyDataStore.data.first()[
                ENCRYPTED_KEY
            ]

        /*
         * Do not encrypt the API key again if an
         * encrypted value already exists.
         */
        if (!currentValue.isNullOrBlank()) {
            return
        }

        val apiKey =
            BuildConfig.GEMINI_API_KEY

        require(apiKey.isNotBlank()) {
            "GEMINI_API_KEY is empty."
        }

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

        val ciphertext =
            cipher.doFinal(
                apiKey.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val iv =
            cipher.iv

        /*
         * Store the IV and ciphertext as:
         *
         * Base64(IV):Base64(ciphertext)
         */
        val storedValue =
            Base64.encodeToString(
                iv,
                Base64.NO_WRAP
            ) +
                    ":" +
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP
                    )

        context.secureKeyDataStore.edit { preferences ->

            preferences[
                ENCRYPTED_KEY
            ] = storedValue
        }
    }


    /*
     * Reads the encrypted API key from DataStore,
     * decrypts it using the AES key stored in
     * Android Keystore, and returns it in memory.
     */
    suspend fun getDecryptedApiKey(): String {

        val storedValue =
            context.secureKeyDataStore.data.first()[
                ENCRYPTED_KEY
            ]
                ?: throw IllegalStateException(
                    "Encrypted Gemini API key not found."
                )

        val parts =
            storedValue.split(":")

        require(parts.size == 2) {
            "Stored encrypted API key is invalid."
        }

        val iv =
            Base64.decode(
                parts[0],
                Base64.NO_WRAP
            )

        val ciphertext =
            Base64.decode(
                parts[1],
                Base64.NO_WRAP
            )

        val secretKey =
            getOrCreateSecretKey()

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )
        )

        val plaintext =
            cipher.doFinal(
                ciphertext
            )

        return String(
            plaintext,
            StandardCharsets.UTF_8
        )
    }


    /*
     * Retrieves the existing AES key from Android
     * Keystore, or generates it if it does not exist.
     */
    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                KEYSTORE_NAME
            )

        keyStore.load(null)

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
                .setRandomizedEncryptionRequired(
                    true
                )
                .build()

        keyGenerator.init(
            keySpec
        )

        return keyGenerator.generateKey()
    }
}