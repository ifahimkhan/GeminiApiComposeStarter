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
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val userName: String = "Student",
    val systemPromptTone: String = "Helpful AI Assistant"
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val SYSTEM_PROMPT_TONE = stringPreferencesKey("system_prompt_tone")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val userName = preferences[PreferencesKeys.USER_NAME] ?: "Student"
            val tone = preferences[PreferencesKeys.SYSTEM_PROMPT_TONE] ?: "Helpful AI Assistant"
            UserPreferences(userName = userName, systemPromptTone = tone)
        }

    suspend fun updateUserName(name: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name.trim()
        }
    }

    suspend fun updateSystemPromptTone(tone: String) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT_TONE] = tone.trim()
        }
    }
}
