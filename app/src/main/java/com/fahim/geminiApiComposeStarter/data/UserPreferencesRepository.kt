package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesRepository(
    private val context: Context
) {

    companion object {
        private val RESPONSE_STYLE_KEY =
            stringPreferencesKey("response_style")

        const val CONCISE = "Concise"
        const val NORMAL = "Normal"
        const val DETAILED = "Detailed"
    }

    val responseStyle: Flow<String> =
        context.userPreferencesDataStore.data.map { preferences ->

            preferences[RESPONSE_STYLE_KEY] ?: NORMAL
        }

    suspend fun setResponseStyle(style: String) {

        context.userPreferencesDataStore.edit { preferences ->

            preferences[RESPONSE_STYLE_KEY] = style
        }
    }
}