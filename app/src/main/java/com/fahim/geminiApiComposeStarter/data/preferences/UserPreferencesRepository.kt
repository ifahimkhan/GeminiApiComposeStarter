package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val AUTO_SCROLL_KEY = booleanPreferencesKey("chat_auto_scroll")
    }

    val themeFlow: Flow<AppTheme> = dataStore.data.map { preferences ->
        when (preferences[THEME_KEY]) {
            AppTheme.LIGHT.name -> AppTheme.LIGHT
            AppTheme.DARK.name -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    val autoScrollFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_SCROLL_KEY] ?: true
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun setAutoScroll(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SCROLL_KEY] = enabled
        }
    }
}
