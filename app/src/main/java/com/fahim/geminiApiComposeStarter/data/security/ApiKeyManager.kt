package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyManager(context: Context) {

    companion object {
        private const val KEY_ALIAS = "DocuRAG_ApiKey_Key"
        private const val PREFS_NAME = "secure_api_key"
        private const val ENCRYPTED_KEY = "encrypted_api_key"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val preferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getKey(KEY_ALIAS, null) as SecretKey
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

    fun storeApiKey(apiKey: String) {
        if (apiKey.isBlank()) return

        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        val iv = cipher.iv

        val combined = iv + encryptedBytes

        val encoded = Base64.encodeToString(
            combined,
            Base64.NO_WRAP
        )

        preferences.edit()
            .putString(ENCRYPTED_KEY, encoded)
            .apply()
    }

    fun getApiKey(): String {
        val storedValue = preferences.getString(ENCRYPTED_KEY, null)
            ?: return ""

        return try {
            val combined = Base64.decode(
                storedValue,
                Base64.NO_WRAP
            )

            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val secretKey = getOrCreateSecretKey()

            val cipher = Cipher.getInstance(AES_MODE)

            val gcmSpec = GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                gcmSpec
            )

            String(
                cipher.doFinal(encryptedBytes),
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            ""
        }
    }
}