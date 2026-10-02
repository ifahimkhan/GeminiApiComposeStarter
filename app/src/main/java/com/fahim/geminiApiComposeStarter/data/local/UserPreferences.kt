package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val userName: String = "User",
    val systemPrompt: String = "You are a helpful, witty, and concise AI assistant powered by Gemini."
)

/**
 * Abstraction for reading and writing user preferences so ViewModel can be tested without Android Context.
 */
interface PreferencesRepository {
    val preferencesFlow: Flow<UserPreferences>
    suspend fun updateUserName(name: String)
    suspend fun updateSystemPrompt(prompt: String)
}

class UserPreferencesRepository(private val context: Context) : PreferencesRepository {

    companion object {
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private const val DEFAULT_USER_NAME = "User"
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a helpful, witty, and concise AI assistant powered by Gemini."
    }

    override val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            userName = prefs[KEY_USER_NAME] ?: DEFAULT_USER_NAME,
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
        )
    }

    override suspend fun updateUserName(name: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    override suspend fun updateSystemPrompt(prompt: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_SYSTEM_PROMPT] = prompt
        }
    }
}
