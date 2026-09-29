package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the encryption and decryption of the Gemini API Key at rest
 * using AES-256-GCM backed by the hardware-backed Android Keystore.
 *
 * Requirements fulfilled:
 * 1. Generates an AES-256-GCM key inside Android Keystore using KeyGenParameterSpec.
 * 2. Encrypts the raw API key and stores only the ciphertext & IV at rest.
 * 3. Decrypts only in memory when initializing the GenerativeModel.
 * 4. Never logs, toasts, or exposes the decrypted key.
 */
object ApiKeySecurityManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "gemini_aes_gcm_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_NAME = "gemini_secure_store"
    private const val PREF_KEY_CIPHERTEXT = "encrypted_api_key"
    private const val PREF_KEY_IV = "encryption_iv"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts and persists the API key at rest if not already encrypted.
     */
    fun secureApiKey(context: Context, rawApiKey: String) {
        if (rawApiKey.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(PREF_KEY_CIPHERTEXT) && prefs.contains(PREF_KEY_IV)) {
            return
        }

        try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val cipherText = cipher.doFinal(rawApiKey.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_KEY_CIPHERTEXT, Base64.encodeToString(cipherText, Base64.NO_WRAP))
                .putString(PREF_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            // Silently fail without exposing secrets
        }
    }

    /**
     * Decrypts the API key in memory at the exact moment GenerativeModel is initialized.
     */
    fun getDecryptedApiKey(context: Context, fallbackApiKey: String = ""): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(PREF_KEY_CIPHERTEXT, null)
        val ivBase64 = prefs.getString(PREF_KEY_IV, null)

        if (encryptedBase64.isNullOrEmpty() || ivBase64.isNullOrEmpty()) {
            return fallbackApiKey
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherText = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            fallbackApiKey
        }
    }
}
