package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferences(
    private val context: Context
) {

    companion object {
        private val DYNAMIC_COLOR =
            booleanPreferencesKey("dynamic_color")
    }

    val dynamicColor: Flow<Boolean> =
        context.userPreferencesDataStore.data.map { preferences ->
            preferences[DYNAMIC_COLOR] ?: true
        }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }
}