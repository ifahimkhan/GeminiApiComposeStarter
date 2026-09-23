package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.secureStore by preferencesDataStore(name = "secure_prefs")

/** Keeps the API key in DataStore as Keystore-encrypted ciphertext only. */
class ApiKeyStorage(
    private val context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) {
    private val keyName = stringPreferencesKey("encrypted_gemini_api_key")

    /**
     * Encrypts and stores the build-time key (if there is one), then returns the
     * decrypted key read back from storage. Returns "" if no key is available.
     */
    suspend fun syncAndLoad(buildKey: String): String {
        if (buildKey.isNotBlank()) {
            context.secureStore.edit { it[keyName] = cipher.encrypt(buildKey) }
        }
        val stored = context.secureStore.data.first()[keyName] ?: return ""
        return runCatching { cipher.decrypt(stored) }.getOrDefault(buildKey)
    }
}