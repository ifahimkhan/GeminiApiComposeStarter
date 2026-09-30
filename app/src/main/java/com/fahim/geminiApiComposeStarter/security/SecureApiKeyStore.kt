package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.data.settingsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Keeps the Gemini API key encrypted at rest.
 * Only the ciphertext is saved in DataStore; the plain key is never written to disk.
 */
class SecureApiKeyStore(private val context: Context) {

    private val encryptedKeyPref = stringPreferencesKey("encrypted_gemini_api_key")

    /** First launch: encrypt the key and save only the ciphertext. Later launches do nothing. */
    suspend fun saveIfNeeded(plainKey: String) = withContext(Dispatchers.IO) {
        if (plainKey.isBlank()) return@withContext
        val alreadySaved = context.settingsDataStore.data.first()[encryptedKeyPref] != null
        if (!alreadySaved) {
            val cipherText = KeystoreCipher.encrypt(plainKey)
            context.settingsDataStore.edit { prefs -> prefs[encryptedKeyPref] = cipherText }
        }
    }

    /**
     * Decrypts the key in memory. Called only when the GenerativeModel is created.
     * Never log, toast or display the value returned here.
     */
    suspend fun getApiKey(): String = withContext(Dispatchers.IO) {
        val cipherText = context.settingsDataStore.data
            .map { prefs -> prefs[encryptedKeyPref] }
            .filterNotNull()
            .first()
        KeystoreCipher.decrypt(cipherText)
    }
}