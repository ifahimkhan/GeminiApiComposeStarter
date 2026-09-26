package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferences(
    private val context: Context
) : PreferencesStore {

    private object PreferencesKeys {

        val SHOW_USER_MESSAGES =
            booleanPreferencesKey(
                "show_user_messages"
            )

        val ENCRYPTED_GEMINI_API_KEY =
            stringPreferencesKey(
                "encrypted_gemini_api_key"
            )
    }

    override val showUserMessages: Flow<Boolean> =
        context.dataStore.data.map { preferences ->

            preferences[
                PreferencesKeys.SHOW_USER_MESSAGES
            ] ?: true
        }

    override suspend fun setShowUserMessages(
        enabled: Boolean
    ) {

        context.dataStore.edit { preferences ->

            preferences[
                PreferencesKeys.SHOW_USER_MESSAGES
            ] = enabled
        }
    }

    override val encryptedGeminiApiKey:
            Flow<String?> =
        context.dataStore.data.map { preferences ->

            preferences[
                PreferencesKeys.ENCRYPTED_GEMINI_API_KEY
            ]
        }

    override suspend fun saveEncryptedGeminiApiKey(
        encryptedApiKey: String
    ) {

        context.dataStore.edit { preferences ->

            preferences[
                PreferencesKeys.ENCRYPTED_GEMINI_API_KEY
            ] = encryptedApiKey
        }
    }
}