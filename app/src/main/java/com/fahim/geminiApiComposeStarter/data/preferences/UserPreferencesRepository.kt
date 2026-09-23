package com.fahim.geminiApiComposeStarter.data.preferences

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val selectedModel: String = DEFAULT_MODEL,
    val autoScroll: Boolean = true,
    val encryptedApiKey: String = "",
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }
}

class UserPreferencesRepository(
    private val context: Context,
) {
    private object PreferencesKeys {
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                selectedModel = preferences[PreferencesKeys.SELECTED_MODEL]
                    ?: UserPreferences.DEFAULT_MODEL,
                autoScroll = preferences[PreferencesKeys.AUTO_SCROLL] ?: true,
                encryptedApiKey = preferences[PreferencesKeys.ENCRYPTED_API_KEY].orEmpty(),
            )
        }

    suspend fun updateSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_MODEL] = model
        }
    }

    suspend fun updateAutoScroll(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCROLL] = enabled
        }
    }

    suspend fun saveEncryptedApiKey(encryptedKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_API_KEY] = encryptedKey
        }
    }
}
