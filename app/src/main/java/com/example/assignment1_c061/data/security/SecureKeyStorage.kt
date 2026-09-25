package com.example.assignment1_c061.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureKeyStorage(context: Context) {

    private val sharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyGenerated()
    }

    private fun ensureKeyGenerated() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypts the raw API key and stores the ciphertext and IV securely in SharedPreferences.
     */
    fun saveEncryptedApiKey(rawApiKey: String) {
        if (rawApiKey.isBlank()) return
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(rawApiKey.toByteArray(Charsets.UTF_8))

        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        sharedPreferences.edit()
            .putString(KEY_IV, ivString)
            .putString(KEY_CIPHERTEXT, ciphertextString)
            .apply()
    }

    /**
     * Decrypts and returns the API key in memory at the exact moment needed.
     */
    fun getDecryptedApiKey(): String {
        val ivString = sharedPreferences.getString(KEY_IV, null)
        val ciphertextString = sharedPreferences.getString(KEY_CIPHERTEXT, null)

        if (ivString.isNullOrEmpty() || ciphertextString.isNullOrEmpty()) {
            return ""
        }

        return try {
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextString, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private const val PREFS_NAME = "secure_api_key_prefs"
        private const val KEY_ALIAS = "GeminiApiKeyAlias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_IV = "encrypted_key_iv"
        private const val KEY_CIPHERTEXT = "encrypted_key_ciphertext"
    }
}
