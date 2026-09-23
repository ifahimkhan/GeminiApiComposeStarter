package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureApiKeyStorage(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "SecureApiKeyStorage"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_alias"

        private const val PREFS_NAME = "secure_api_key_prefs"
        private const val ENCRYPTED_KEY = "encrypted_api_key"
        private const val IV_KEY = "api_key_iv"

        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey is SecretKey) return existingKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    fun saveApiKey(apiKey: String) {
        require(apiKey.isNotBlank()) {
            "API key cannot be blank"
        }

        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val encryptedBytes = cipher.doFinal(
            apiKey.toByteArray(Charsets.UTF_8)
        )

        val success = preferences.edit()
            .putString(
                ENCRYPTED_KEY,
                Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            )
            .putString(
                IV_KEY,
                Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            )
            .commit()

        check(success) {
            "Failed to save encrypted API key"
        }
    }

    fun getApiKey(): String? {
        val encryptedKey = preferences.getString(ENCRYPTED_KEY, null)
        val encodedIv = preferences.getString(IV_KEY, null)

        if (encryptedKey.isNullOrBlank() || encodedIv.isNullOrBlank()) {
            return null
        }

        return try {
            val encryptedBytes = Base64.decode(encryptedKey, Base64.NO_WRAP)
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)

            require(iv.size == 12) {
                "Invalid AES-GCM IV"
            }

            val cipher = Cipher.getInstance(AES_MODE)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH, iv)
            )

            val decryptedKey = cipher.doFinal(encryptedBytes)
                .toString(Charsets.UTF_8)

            decryptedKey.takeIf { it.isNotBlank() }

        } catch (e: Exception) {
            Log.w(TAG, "Unable to decrypt stored API key; clearing invalid data")
            clearStoredData()
            null
        }
    }

    fun clearApiKey() {
        clearStoredData()

        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "Could not remove Keystore key", e)
        }
    }

    private fun clearStoredData() {
        preferences.edit()
            .remove(ENCRYPTED_KEY)
            .remove(IV_KEY)
            .apply()
    }
}