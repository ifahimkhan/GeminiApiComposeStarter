package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

/**
 * Repository interface managing persistence of the user's theme selection.
 */
interface ThemePreferencesRepository {
    /**
     * Flow emitting the user's saved dark mode choice.
     * Emits null when no explicit preference has been stored yet (sensible default).
     */
    val isDarkMode: Flow<Boolean?>

    /**
     * Persists the user's dark mode preference to DataStore.
     */
    suspend fun setDarkMode(isDark: Boolean)
}

/**
 * Default implementation backed by Jetpack Preferences DataStore.
 */
class ThemePreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository {

    constructor(context: Context) : this(context.themeDataStore)

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    }

    override val isDarkMode: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_DARK_MODE]
    }

    override suspend fun setDarkMode(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDark
        }
    }
}
