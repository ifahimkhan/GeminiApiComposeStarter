package com.fahim.geminiApiComposeStarter.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Separate DataStore file so it can be excluded from backups (see backup_rules.xml). */
val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

private val KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")

/**
 * Keeps the Gemini API key encrypted at rest.
 *
 * Flow: on first launch the build-time key (from BuildConfig, which came from local.properties or
 * the CI environment) is encrypted with the Keystore AES-256-GCM key and only the ciphertext is
 * written to DataStore. From then on the plain key exists only in memory, briefly, when
 * [getApiKey] is called to build the GenerativeModel. It is never logged, toasted or displayed.
 */
class ApiKeyStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SecretCipher,
    private val buildTimeKey: String,
) {
    suspend fun getApiKey(): String = withContext(Dispatchers.Default) {
        val stored = dataStore.data.first()[KEY_CIPHERTEXT]
        // A failure here means the ciphertext is corrupt or the Keystore key was wiped
        // (e.g. restored backup, screen-lock reset). Treat it as "nothing stored".
        val existing = stored?.let { runCatching { cipher.decrypt(it) }.getOrNull() }

        when {
            buildTimeKey.isBlank() -> existing.orEmpty()
            existing == buildTimeKey -> existing
            else -> {
                // First launch, or the developer changed the key in local.properties.
                dataStore.edit { it[KEY_CIPHERTEXT] = cipher.encrypt(buildTimeKey) }
                buildTimeKey
            }
        }
    }
}
