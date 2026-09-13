package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private companion object {
        val KEY_EXPANDED_INPUT = booleanPreferencesKey("expanded_input_mode")
    }

    val isExpandedInput: Flow<Boolean> = context.userPreferencesDataStore.data.map { preferences ->
        preferences[KEY_EXPANDED_INPUT] ?: true
    }

    suspend fun setExpandedInput(expanded: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[KEY_EXPANDED_INPUT] = expanded
        }
    }
}
