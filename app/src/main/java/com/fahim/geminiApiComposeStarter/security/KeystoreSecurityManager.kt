package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages encryption at rest for sensitive values such as the Gemini API Key.
 * Generates an AES-256-GCM key inside the Android Keystore system.
 * The decrypted key is never logged, toasted, or exposed in UI.
 */
class KeystoreSecurityManager(private val context: Context) {

    companion object {
        private const val KEY_ALIAS = "GeminiApiKeySecretKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "secure_gemini_prefs"
        private const val ENCRYPTED_KEY_PARAM = "encrypted_gemini_api_key"
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        ensureKeystoreKeyExists()
    }

    private fun ensureKeystoreKeyExists() {
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
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts and persists the API key at rest securely.
     */
    fun storeApiKeyEncrypted(apiKey: String) {
        if (apiKey.isBlank()) return
        encryptedPrefs.edit().putString(ENCRYPTED_KEY_PARAM, apiKey).apply()
    }

    /**
     * Decrypts the API key in memory only when GenerativeModel is initialized.
     */
    fun getDecryptedApiKey(): String {
        return encryptedPrefs.getString(ENCRYPTED_KEY_PARAM, null) ?: ""
    }

    /**
     * Direct AES-256-GCM cipher encryption helper (used for custom DataStore byte payloads).
     */
    fun encryptBytes(plainText: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(plainText)
        return Pair(iv, encryptedData)
    }

    /**
     * Direct AES-256-GCM cipher decryption helper.
     */
    fun decryptBytes(iv: ByteArray, encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
