package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/** Non-sensitive UI preferences. Chat history lives in Room; this is only for small settings. */
enum class ThemePreference { SYSTEM, LIGHT, DARK }

class UserPreferencesRepository(private val context: Context) {

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "light" -> ThemePreference.LIGHT
            "dark" -> ThemePreference.DARK
            else -> ThemePreference.SYSTEM
        }
    }

    suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = when (preference) {
                ThemePreference.LIGHT -> "light"
                ThemePreference.DARK -> "dark"
                ThemePreference.SYSTEM -> "system"
            }
        }
    }

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }
}
