package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.secureDataStore by preferencesDataStore(name = "secure_prefs")
private val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_gemini_api_key")

/**
 * Persists only the AES-256-GCM ciphertext of the API key. The plaintext exists in memory
 * just long enough to build the GenerativeModel and is never logged or shown.
 */
class ApiKeyStore(
    context: Context,
    private val buildConfigKey: String,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) {
    private val dataStore = context.applicationContext.secureDataStore

    /** Returns the decrypted key, encrypting and storing it on first launch. Null if no key is configured. */
    suspend fun getApiKey(): String? = withContext(Dispatchers.IO) {
        val stored = dataStore.data.first()[ENCRYPTED_API_KEY]
        val decrypted = stored?.let { runCatching { cipher.decrypt(it) }.getOrNull() }

        when {
            // Nothing usable stored (first launch, or Keystore key lost e.g. after restore) -> re-encrypt.
            buildConfigKey.isNotBlank() && decrypted != buildConfigKey -> {
                dataStore.edit { it[ENCRYPTED_API_KEY] = cipher.encrypt(buildConfigKey) }
                buildConfigKey
            }
            else -> decrypted?.takeIf { it.isNotBlank() }
        }
    }
}
