package com.example.c002_dhruvil_assignment1.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object ApiKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "GeminiApiKeyEncryptionKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREF_NAME = "secure_api_prefs"
    private const val PREF_CIPHERTEXT = "api_key_ciphertext"
    private const val PREF_IV = "api_key_iv"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun getOrStoreApiKey(context: Context, rawApiKey: String): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedCiphertext = prefs.getString(PREF_CIPHERTEXT, null)
        val storedIv = prefs.getString(PREF_IV, null)

        if (!storedCiphertext.isNullOrEmpty() && !storedIv.isNullOrEmpty()) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(128, Base64.decode(storedIv, Base64.DEFAULT))
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
                val decryptedBytes = cipher.doFinal(Base64.decode(storedCiphertext, Base64.DEFAULT))
                val decryptedKey = String(decryptedBytes, Charsets.UTF_8)

                if (rawApiKey.isEmpty() || rawApiKey == decryptedKey) {
                    return decryptedKey
                }
            } catch (_: Exception) {
            }
        }

        // Encrypt and store rawApiKey
        if (rawApiKey.isNotEmpty()) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(rawApiKey.toByteArray(Charsets.UTF_8))

                prefs.edit()
                    .putString(PREF_CIPHERTEXT, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
                    .putString(PREF_IV, Base64.encodeToString(iv, Base64.DEFAULT))
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return rawApiKey
    }
}
