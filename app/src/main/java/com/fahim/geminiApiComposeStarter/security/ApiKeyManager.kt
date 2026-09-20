package com.fahim.geminiApiComposeStarter.security
import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyManager(context: Context) {

    companion object {
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiApiKeyEncryptionKey"

        private const val PREF_NAME = "secure_api_storage"
        private const val ENCRYPTED_API_KEY = "encrypted_api_key"
        private const val IV = "api_key_iv"
    }

    private val preferences =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Gets the AES-256 key from Android Keystore.
     * If it doesn't exist, creates it.
     */
    private fun getOrCreateSecretKey(): SecretKey {

        val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(
                KEY_ALIAS,
                null
            ) as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_NAME
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    /**
     * Encrypts and stores the API key.
     */
    fun saveApiKey(apiKey: String) {

        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey
        )

        val encryptedBytes = cipher.doFinal(
            apiKey.toByteArray(StandardCharsets.UTF_8)
        )

        val iv = cipher.iv

        preferences.edit()
            .putString(
                ENCRYPTED_API_KEY,
                Base64.encodeToString(
                    encryptedBytes,
                    Base64.DEFAULT
                )
            )
            .putString(
                IV,
                Base64.encodeToString(
                    iv,
                    Base64.DEFAULT
                )
            )
            .apply()
    }

    /**
     * Decrypts and returns the API key.
     */
    fun getApiKey(): String? {

        val encryptedApiKey =
            preferences.getString(
                ENCRYPTED_API_KEY,
                null
            )

        val encodedIv =
            preferences.getString(
                IV,
                null
            )

        if (
            encryptedApiKey == null ||
            encodedIv == null
        ) {
            return null
        }

        val secretKey = getOrCreateSecretKey()

        val encryptedBytes = Base64.decode(
            encryptedApiKey,
            Base64.DEFAULT
        )

        val iv = Base64.decode(
            encodedIv,
            Base64.DEFAULT
        )

        val cipher = Cipher.getInstance(
            "AES/GCM/NoPadding"
        )

        val gcmSpec = GCMParameterSpec(
            128,
            iv
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            gcmSpec
        )

        val decryptedBytes = cipher.doFinal(
            encryptedBytes
        )

        return String(
            decryptedBytes,
            StandardCharsets.UTF_8
        )
    }
}