package com.fahim.geminiApiComposeStarter.data.preferences

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

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferencesRepository(
    private val context: Context
) {

    companion object {
        private val THEME_KEY: Preferences.Key<String> =
            stringPreferencesKey("theme_preference")
    }

    val themePreference: Flow<ThemePreference> =
        context.userPreferencesDataStore.data.map { preferences ->

            when (
                preferences[THEME_KEY]
            ) {
                ThemePreference.LIGHT.name ->
                    ThemePreference.LIGHT

                ThemePreference.DARK.name ->
                    ThemePreference.DARK

                else ->
                    ThemePreference.SYSTEM
            }
        }

    suspend fun setThemePreference(
        preference: ThemePreference
    ) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[THEME_KEY] = preference.name
        }
    }
}