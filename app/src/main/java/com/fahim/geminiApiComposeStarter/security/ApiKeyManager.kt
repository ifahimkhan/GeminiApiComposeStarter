package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.securityDataStore by preferencesDataStore(name = "gemini_secure_storage")

/**
 * Manages secure persistence and on-demand in-memory decryption of the Gemini API key.
 * The plaintext API key is NEVER persisted to disk and NEVER logged.
 */
class ApiKeyManager(
    private val context: Context,
    private val keystoreManager: KeystoreManager = KeystoreManager(),
) {

    private val ivKey = stringPreferencesKey("encrypted_api_key_iv")
    private val cipherKey = stringPreferencesKey("encrypted_api_key_cipher")

    /**
     * Encrypts the build-time API key using Android Keystore AES-256-GCM
     * and persists only the ciphertext & IV into DataStore.
     */
    suspend fun initializeKey(rawKey: String) {
        if (rawKey.isBlank()) return

        val hasExistingCipher = context.securityDataStore.data.map { preferences ->
            preferences[cipherKey] != null && preferences[ivKey] != null
        }.first()

        if (!hasExistingCipher) {
            val (iv, ciphertext) = keystoreManager.encrypt(rawKey.trim())
            val encodedIv = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encodedCipher = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

            context.securityDataStore.edit { prefs ->
                prefs[ivKey] = encodedIv
                prefs[cipherKey] = encodedCipher
            }
        }
    }

    /**
     * Decrypts the API key in memory only when required to initialize GenerativeModel.
     * Never store the returned value in fields that outlive the repository.
     */
    suspend fun getDecryptedApiKey(): String {
        return try {
            val prefs = context.securityDataStore.data.first()
            val encodedIv = prefs[ivKey]
            val encodedCipher = prefs[cipherKey]

            if (!encodedIv.isNullOrBlank() && !encodedCipher.isNullOrBlank()) {
                val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
                val ciphertext = Base64.decode(encodedCipher, Base64.NO_WRAP)
                keystoreManager.decrypt(iv, ciphertext)
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Checks whether an encrypted API key exists in persistent storage.
     */
    suspend fun hasApiKey(): Boolean {
        val prefs = context.securityDataStore.data.first()
        return !prefs[cipherKey].isNullOrBlank() && !prefs[ivKey].isNullOrBlank()
    }
}
