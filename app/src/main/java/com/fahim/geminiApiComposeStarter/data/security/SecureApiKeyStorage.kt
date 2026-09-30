package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.data.preferences.appPreferencesDataStore
import com.fahim.geminiApiComposeStarter.security.CryptoManager
import kotlinx.coroutines.flow.firstOrNull
import java.util.Base64

class SecureApiKeyStorage(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager = CryptoManager(),
) {
    constructor(context: Context) : this(
        dataStore = context.appPreferencesDataStore,
        cryptoManager = CryptoManager(),
    )

    companion object {
        private const val TAG = "SecureApiKeyStorage"
        private val KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        private val KEY_IV = stringPreferencesKey("gemini_api_key_iv")
    }

    /**
     * Retrieves the decrypted API key in memory only.
     * 1. If encrypted ciphertext + IV exist in DataStore, decrypts using Keystore AES key.
     * 2. If no ciphertext exists (first launch) and [buildConfigApiKey] is provided:
     *    encrypts with AES-256-GCM, persists ciphertext + IV, and returns the key.
     * 3. Returns empty string if neither is available or if decryption fails.
     */
    suspend fun getOrInitializeApiKey(buildConfigApiKey: String): String {
        try {
            val preferences = dataStore.data.firstOrNull()
            val ciphertextB64 = preferences?.get(KEY_CIPHERTEXT)
            val ivB64 = preferences?.get(KEY_IV)

            if (!ciphertextB64.isNullOrBlank() && !ivB64.isNullOrBlank()) {
                val ciphertext = Base64.getDecoder().decode(ciphertextB64)
                val iv = Base64.getDecoder().decode(ivB64)
                val decryptedBytes = cryptoManager.decrypt(ciphertext, iv)
                val decryptedKey = String(decryptedBytes, Charsets.UTF_8).trim()
                if (decryptedKey.isNotEmpty()) {
                    return decryptedKey
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption of persisted API key failed, falling back to build config")
        }

        // First launch or fallback: encrypt and persist if buildConfigApiKey is present
        val cleanBuildKey = buildConfigApiKey.trim()
        if (cleanBuildKey.isNotEmpty()) {
            try {
                val (ciphertext, iv) = cryptoManager.encrypt(cleanBuildKey.toByteArray(Charsets.UTF_8))
                val ciphertextB64 = Base64.getEncoder().encodeToString(ciphertext)
                val ivB64 = Base64.getEncoder().encodeToString(iv)

                dataStore.edit { prefs ->
                    prefs[KEY_CIPHERTEXT] = ciphertextB64
                    prefs[KEY_IV] = ivB64
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to encrypt and persist API key to DataStore")
            }
            return cleanBuildKey
        }

        return ""
    }
}
