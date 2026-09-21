package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.security.ApiKeyVault
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persists lightweight user preferences (Preferences DataStore) and the *encrypted* Gemini
 * API key material. Only ciphertext + IV are ever written here — see [ApiKeyVault].
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val API_KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        val API_KEY_IV = stringPreferencesKey("gemini_api_key_iv")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /**
     * Ensures the plaintext [rawApiKey] read from BuildConfig is encrypted-at-rest exactly
     * once, then returns the decrypted key for immediate, in-memory use when constructing the
     * GenerativeModel. Nothing plaintext is written to disk at any point.
     */
    suspend fun resolveApiKey(rawApiKey: String): String {
        if (rawApiKey.isBlank()) return ""

        val prefs = context.dataStore.data.first()
        val existingCiphertext = prefs[Keys.API_KEY_CIPHERTEXT]
        val existingIv = prefs[Keys.API_KEY_IV]

        if (existingCiphertext != null && existingIv != null) {
            return runCatching {
                ApiKeyVault.decrypt(ApiKeyVault.EncryptedPayload(existingCiphertext, existingIv))
            }.getOrElse { encryptAndStore(rawApiKey) }
        }
        return encryptAndStore(rawApiKey)
    }

    private suspend fun encryptAndStore(rawApiKey: String): String {
        val payload = ApiKeyVault.encrypt(rawApiKey)
        context.dataStore.edit {
            it[Keys.API_KEY_CIPHERTEXT] = payload.ciphertextBase64
            it[Keys.API_KEY_IV] = payload.ivBase64
        }
        return rawApiKey
    }
}
