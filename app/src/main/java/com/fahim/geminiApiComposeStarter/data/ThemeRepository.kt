package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

open class ThemeRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")

    open val selectedThemeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeString = preferences[themeKey] ?: AppTheme.SYSTEM.name
            try {
                AppTheme.valueOf(themeString)
            } catch (e: Exception) {
                AppTheme.SYSTEM
            }
        }

    open suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = theme.name
        }
    }
}
