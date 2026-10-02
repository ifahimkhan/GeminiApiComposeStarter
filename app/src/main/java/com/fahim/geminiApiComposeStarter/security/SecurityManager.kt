package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.securityDataStore by preferencesDataStore(name = "secure_credentials")

/**
 * Handles AES-256-GCM cryptographic operations backed by Android KeyStore.
 * The encryption key is securely generated and kept inside hardware-backed KeyStore.
 * Only the initialization vector (IV) and encrypted ciphertext are persisted at rest.
 */
class SecurityManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_keystore_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128

        private val KEY_ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_gemini_api_key")
        private val KEY_API_KEY_IV = stringPreferencesKey("gemini_api_key_iv")
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Retrieves the existing AES SecretKey from Android KeyStore or creates a new 256-bit AES key.
     */
    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(parameterSpec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Checks if encrypted API key credentials already exist in DataStore.
     */
    suspend fun hasStoredEncryptedKey(): Boolean {
        val prefs = context.securityDataStore.data.first()
        return !prefs[KEY_ENCRYPTED_API_KEY].isNullOrBlank() && !prefs[KEY_API_KEY_IV].isNullOrBlank()
    }

    /**
     * Encrypts the provided API key using AES-256-GCM and persists only the IV and ciphertext.
     * The plaintext key is discarded immediately after encryption.
     */
    suspend fun storeEncryptedApiKey(plainApiKey: String) {
        if (plainApiKey.isBlank()) return

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainApiKey.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        context.securityDataStore.edit { preferences ->
            preferences[KEY_API_KEY_IV] = ivBase64
            preferences[KEY_ENCRYPTED_API_KEY] = ciphertextBase64
        }
    }

    /**
     * Initializes key at rest on initial launch if not already stored.
     */
    suspend fun ensureApiKeyStored(plainApiKey: String) {
        if (plainApiKey.isNotBlank() && !hasStoredEncryptedKey()) {
            storeEncryptedApiKey(plainApiKey)
        }
    }

    /**
     * Decrypts the stored API key in memory at the exact moment it is needed.
     * Plaintext key is returned only in-memory and never logged or exposed.
     */
    suspend fun getDecryptedApiKey(): String? {
        val prefs = context.securityDataStore.data.first()
        val ivBase64 = prefs[KEY_API_KEY_IV] ?: return null
        val ciphertextBase64 = prefs[KEY_ENCRYPTED_API_KEY] ?: return null

        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
