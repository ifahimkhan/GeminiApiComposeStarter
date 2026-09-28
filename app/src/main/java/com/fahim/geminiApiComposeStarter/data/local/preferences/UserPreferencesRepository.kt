package com.fahim.geminiApiComposeStarter.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val selectedModel: String = DEFAULT_MODEL,
    val isDarkMode: Boolean? = null, // null = follow system
    val dynamicColorEnabled: Boolean = true,
    val encryptedApiKey: String? = null,
    val encryptedApiKeyIv: String? = null,
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val ENCRYPTED_API_KEY_IV = stringPreferencesKey("encrypted_api_key_iv")
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
            val model = preferences[PreferenceKeys.SELECTED_MODEL] ?: UserPreferences.DEFAULT_MODEL
            val themeMode = preferences[PreferenceKeys.THEME_MODE] ?: "SYSTEM"
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> null
            }
            val dynamicColor = preferences[PreferenceKeys.DYNAMIC_COLOR] ?: true
            val encKey = preferences[PreferenceKeys.ENCRYPTED_API_KEY]
            val encIv = preferences[PreferenceKeys.ENCRYPTED_API_KEY_IV]

            UserPreferences(
                selectedModel = model,
                isDarkMode = isDark,
                dynamicColorEnabled = dynamicColor,
                encryptedApiKey = encKey,
                encryptedApiKeyIv = encIv,
            )
        }

    suspend fun updateSelectedModel(model: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SELECTED_MODEL] = model
        }
    }

    suspend fun updateThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun saveEncryptedApiKey(ciphertext: String, iv: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.ENCRYPTED_API_KEY] = ciphertext
            preferences[PreferenceKeys.ENCRYPTED_API_KEY_IV] = iv
        }
    }

    suspend fun clearEncryptedApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.ENCRYPTED_API_KEY)
            preferences.remove(PreferenceKeys.ENCRYPTED_API_KEY_IV)
        }
    }
}
