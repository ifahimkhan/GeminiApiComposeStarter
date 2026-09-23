package com.example.c008_nishit_assignment1.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.c008_nishit_assignment1.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ApiKeyManager manages the secure storage of the Gemini API Key using AES-256-GCM
 * with keys stored securely in the AndroidKeyStore.
 */
class ApiKeyManager(private val context: Context) {

    private val alias = "GeminiApiKeyAlias"
    private val prefsName = "secure_api_key_prefs"
    private val encryptedKeyProp = "encrypted_api_key"
    private val ivProp = "api_key_iv"

    init {
        try {
            ensureKeyStored()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existingKey = keyStore.getKey(alias, null) as? SecretKey
            if (existingKey != null) return existingKey

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ensureKeyStored() {
        val initialKey = BuildConfig.GEMINI_API_KEY
        if (initialKey.isNotEmpty() && initialKey != "your_api_key_here") {
            val savedKey = getDecryptedApiKey()
            if (savedKey != initialKey) {
                encryptAndSaveKey(initialKey)
            }
        }
    }

    fun encryptAndSaveKey(apiKey: String) {
        try {
            val secretKey = getOrCreateSecretKey() ?: return
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
            val ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT)

            context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                .edit()
                .putString(encryptedKeyProp, encryptedBase64)
                .putString(ivProp, ivBase64)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDecryptedApiKey(): String {
        try {
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val encryptedBase64 = prefs.getString(encryptedKeyProp, null)
            val ivBase64 = prefs.getString(ivProp, null)

            if (encryptedBase64 != null && ivBase64 != null) {
                val secretKey = getOrCreateSecretKey()
                if (secretKey != null) {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val iv = Base64.decode(ivBase64, Base64.DEFAULT)
                    val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    val decryptedBytes = cipher.doFinal(encryptedBytes)
                    return String(decryptedBytes, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return BuildConfig.GEMINI_API_KEY
    }
}
