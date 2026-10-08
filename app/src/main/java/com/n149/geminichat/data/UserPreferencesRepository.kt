package com.n149.geminichat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Preferences DataStore for user-configurable settings.
 * Persists across restarts without blocking the main thread.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_DARK_MODE] ?: false }

    val systemPrompt: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_SYSTEM_PROMPT] ?: "You are a helpful assistant." }

    val modelName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_MODEL_NAME] ?: "gemini-1.5-flash" }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_DARK_MODE] = enabled }
    }

    suspend fun setSystemPrompt(prompt: String) {
        context.dataStore.edit { prefs -> prefs[KEY_SYSTEM_PROMPT] = prompt }
    }

    suspend fun setModelName(name: String) {
        context.dataStore.edit { prefs -> prefs[KEY_MODEL_NAME] = name }
    }
}
