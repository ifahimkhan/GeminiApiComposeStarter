package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val SELECTED_MODEL_KEY = stringPreferencesKey("selected_model_name")
        val DARK_MODE_OVERRIDE_KEY = booleanPreferencesKey("dark_mode_override")
        val USER_NAME_KEY = stringPreferencesKey("user_display_name")
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_MODEL_KEY] ?: "gemini-1.5-flash"
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY] ?: "Sudhanshu (N139)"
    }

    suspend fun saveSelectedModel(modelName: String) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL_KEY] = modelName
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }
}
