package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_gemini_prefs")

/**
 * Manages encrypted storage of the Gemini API key.
 * On first launch, the key is encrypted using Android Keystore AES-256-GCM.
 * Only the ciphertext and IV are persisted on disk.
 * Decryption occurs only in memory when initializing the AI model.
 */
class SecureKeyStorage(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
) {

    private object PreferencesKeys {
        val ENCRYPTED_KEY = stringPreferencesKey("encrypted_api_key")
        val IV = stringPreferencesKey("encryption_iv")
    }

    /**
     * Initializes encryption at rest for the API key if not already stored.
     * Only ciphertext and IV are saved in DataStore.
     */
    suspend fun secureKeyAtRest(rawApiKey: String) {
        if (rawApiKey.isBlank()) return

        val alreadySaved = context.secureDataStore.data.map { prefs ->
            prefs[PreferencesKeys.ENCRYPTED_KEY] != null
        }.first()

        if (!alreadySaved) {
            val payload = cryptoManager.encrypt(rawApiKey)
            val base64Cipher = Base64.encodeToString(payload.cipherText, Base64.NO_WRAP)
            val base64Iv = Base64.encodeToString(payload.iv, Base64.NO_WRAP)

            context.secureDataStore.edit { prefs ->
                prefs[PreferencesKeys.ENCRYPTED_KEY] = base64Cipher
                prefs[PreferencesKeys.IV] = base64Iv
            }
        }
    }

    /**
     * Retrieves and decrypts the API key strictly in-memory.
     * Never logs, toasts, or persists the plaintext string.
     */
    suspend fun getDecryptedApiKey(): String? {
        val prefs = context.secureDataStore.data.first()
        val cipherBase64 = prefs[PreferencesKeys.ENCRYPTED_KEY]
        val ivBase64 = prefs[PreferencesKeys.IV]

        if (cipherBase64.isNullOrBlank() || ivBase64.isNullOrBlank()) {
            return null
        }

        return try {
            val cipherBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            cryptoManager.decrypt(iv = ivBytes, cipherText = cipherBytes)
        } catch (e: Exception) {
            null
        }
    }
}
