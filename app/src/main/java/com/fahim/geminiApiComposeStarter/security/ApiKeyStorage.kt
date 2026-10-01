package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

/**
 * Manages encryption and decryption of the Gemini API key at rest using AES-256-GCM
 * with keys stored securely in the Android Keystore.
 *
 * Persists only the encrypted ciphertext and initialization vector (IV) in Preferences DataStore.
 * Plaintext decryption is performed in memory strictly when creating the GenerativeModel,
 * and is never logged, toasted, or written to persistent storage.
 */
class ApiKeyStorage(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "gemini_api_keystore_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private val PREF_ENCRYPTED_KEY = stringPreferencesKey("encrypted_gemini_api_key")
        private val PREF_IV = stringPreferencesKey("gemini_api_key_iv")
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Retrieves or generates an AES-256 key in the Android Keystore.
     */
    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts the provided API key with AES-256-GCM and persists the ciphertext and IV.
     */
    suspend fun saveEncryptedApiKey(plainApiKey: String) {
        if (plainApiKey.isBlank()) return

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plainApiKey.toByteArray(Charsets.UTF_8))

        val encryptedBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        context.securityDataStore.edit { prefs ->
            prefs[PREF_ENCRYPTED_KEY] = encryptedBase64
            prefs[PREF_IV] = ivBase64
        }
    }

    /**
     * Decrypts the API key in memory. Never logs or displays the key.
     * Returns null if no encrypted key is stored or decryption fails.
     */
    suspend fun getDecryptedApiKey(): String? {
        val (encryptedBase64, ivBase64) = context.securityDataStore.data.map { prefs ->
            Pair(prefs[PREF_ENCRYPTED_KEY], prefs[PREF_IV])
        }.first()

        if (encryptedBase64.isNullOrBlank() || ivBase64.isNullOrBlank()) {
            return null
        }

        return try {
            val secretKey = getOrCreateSecretKey()
            val ciphertext = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if an encrypted API key is already stored.
     */
    suspend fun hasStoredApiKey(): Boolean {
        return context.securityDataStore.data.map { prefs ->
            !prefs[PREF_ENCRYPTED_KEY].isNullOrBlank() && !prefs[PREF_IV].isNullOrBlank()
        }.first()
    }
}
