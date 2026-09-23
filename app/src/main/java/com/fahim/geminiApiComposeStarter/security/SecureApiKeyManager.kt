package com.fahim.geminiApiComposeStarter.security

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

class SecureApiKeyManager {

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            ?: (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plaintext: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv ?: throw IllegalStateException("Cipher IV was not generated")
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return iv + ciphertext
    }

    fun decrypt(data: ByteArray): String {
        require(data.size >= IV_LENGTH) { "Data is too short to contain a valid IV" }
        val iv = data.copyOfRange(0, IV_LENGTH)
        val ciphertext = data.copyOfRange(IV_LENGTH, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val plaintextBytes = cipher.doFinal(ciphertext)
        return String(plaintextBytes, Charsets.UTF_8)
    }

    fun storeApiKey(context: Context, apiKey: String) {
        try {
            val encryptedBytes = encrypt(apiKey)
            val base64Encoded = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ENCRYPTED_API_KEY, base64Encoded).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store API key", e)
        }
    }

    fun retrieveApiKey(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val base64Encoded = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return null
            val encryptedBytes = Base64.decode(base64Encoded, Base64.DEFAULT)
            decrypt(encryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve API key", e)
            null
        }
    }

    fun hasStoredKey(context: Context): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.contains(KEY_ENCRYPTED_API_KEY) && !prefs.getString(KEY_ENCRYPTED_API_KEY, null).isNullOrBlank()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check stored key status", e)
            false
        }
    }

    fun clearStoredKey(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_ENCRYPTED_API_KEY).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear stored API key", e)
        }
    }

    companion object {
        private const val TAG = "SecureApiKeyManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_key_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val PREFS_NAME = "secure_api_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_api_key"
    }
}
