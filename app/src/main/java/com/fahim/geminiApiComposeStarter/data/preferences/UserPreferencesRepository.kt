package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val LAST_PROMPT_KEY = stringPreferencesKey("last_prompt")
    }

    val lastPrompt: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_PROMPT_KEY] ?: ""
        }

    suspend fun saveLastPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_PROMPT_KEY] = prompt
        }
    }
}
