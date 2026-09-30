package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    val encryptedApiKeyData: Flow<Pair<String, String>>
    suspend fun saveEncryptedApiKey(encryptedKey: String, iv: String)
    suspend fun getEncryptedApiKeyData(): Pair<String, String>
    val lastConversationId: Flow<String?>
    suspend fun setLastConversationId(conversationId: String)
}

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val API_KEY_IV = stringPreferencesKey("api_key_iv")
        val LAST_CONVERSATION_ID = stringPreferencesKey("last_conversation_id")
    }

    override val themeMode: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeString = preferences[PreferencesKeys.THEME_MODE]
            ThemeMode.fromString(themeString)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override val encryptedApiKeyData: Flow<Pair<String, String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val encryptedKey = preferences[PreferencesKeys.ENCRYPTED_API_KEY].orEmpty()
            val iv = preferences[PreferencesKeys.API_KEY_IV].orEmpty()
            encryptedKey to iv
        }

    override suspend fun saveEncryptedApiKey(encryptedKey: String, iv: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ENCRYPTED_API_KEY] = encryptedKey
            preferences[PreferencesKeys.API_KEY_IV] = iv
        }
    }

    override suspend fun getEncryptedApiKeyData(): Pair<String, String> {
        return encryptedApiKeyData.first()
    }

    override val lastConversationId: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_CONVERSATION_ID]
        }

    override suspend fun setLastConversationId(conversationId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_CONVERSATION_ID] = conversationId
        }
    }
}
