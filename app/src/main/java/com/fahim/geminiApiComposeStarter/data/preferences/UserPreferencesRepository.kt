package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.data.security.EncryptedPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gemini_user_preferences")

data class UserPreferences(
    val themeMode: String = "SYSTEM",
    val autoScroll: Boolean = true,
    val userName: String = "Tanmay Sharma",
    val hasEncryptedKey: Boolean = false,
)

/**
 * Preferences DataStore repository for managing user settings and securely storing ciphertext.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {

    private object PreferencesKeys {
        val KEY_CIPHERTEXT = stringPreferencesKey("encrypted_api_key_ciphertext")
        val KEY_IV = stringPreferencesKey("encrypted_api_key_iv")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val KEY_USER_NAME = stringPreferencesKey("user_display_name")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val theme = preferences[PreferencesKeys.KEY_THEME] ?: "SYSTEM"
            val autoScroll = preferences[PreferencesKeys.KEY_AUTO_SCROLL] ?: true
            val userName = preferences[PreferencesKeys.KEY_USER_NAME] ?: "Tanmay Sharma"
            val ciphertext = preferences[PreferencesKeys.KEY_CIPHERTEXT]
            UserPreferences(
                themeMode = theme,
                autoScroll = autoScroll,
                userName = userName,
                hasEncryptedKey = !ciphertext.isNullOrBlank(),
            )
        }

    val encryptedApiKeyFlow: Flow<EncryptedPayload?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val ciphertext = preferences[PreferencesKeys.KEY_CIPHERTEXT]
            val iv = preferences[PreferencesKeys.KEY_IV]
            if (!ciphertext.isNullOrBlank() && !iv.isNullOrBlank()) {
                EncryptedPayload(ciphertextBase64 = ciphertext, ivBase64 = iv)
            } else {
                null
            }
        }

    suspend fun saveEncryptedApiKey(payload: EncryptedPayload) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_CIPHERTEXT] = payload.ciphertextBase64
            preferences[PreferencesKeys.KEY_IV] = payload.ivBase64
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_THEME] = mode
        }
    }

    suspend fun setAutoScroll(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_AUTO_SCROLL] = enabled
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_USER_NAME] = name
        }
    }
}
