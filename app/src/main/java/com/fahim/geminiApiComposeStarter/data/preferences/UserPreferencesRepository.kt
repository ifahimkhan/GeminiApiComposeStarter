package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val selectedModel: String = DEFAULT_MODEL,
    val userDisplayName: String = "Student",
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }
}

class UserPreferencesRepository(private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val model = preferences[KEY_SELECTED_MODEL] ?: UserPreferences.DEFAULT_MODEL
        val name = preferences[KEY_USER_NAME] ?: "Student"
        UserPreferences(selectedModel = model, userDisplayName = name)
    }

    suspend fun updateSelectedModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    companion object {
        private val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val KEY_USER_NAME = stringPreferencesKey("user_display_name")
    }
}
