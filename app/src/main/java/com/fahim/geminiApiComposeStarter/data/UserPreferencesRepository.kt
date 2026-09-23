package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferences by preferencesDataStore(
    name = "user_preferences"
)

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferencesRepository(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        private val THEME = stringPreferencesKey("theme")
    }

    val themePreference: Flow<ThemePreference> =
        appContext.userPreferences.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences ->
                val savedValue = preferences[THEME]

                ThemePreference.entries.firstOrNull {
                    it.name == savedValue
                } ?: ThemePreference.SYSTEM
            }

    suspend fun saveTheme(theme: ThemePreference) {
        appContext.userPreferences.edit { preferences ->
            preferences[THEME] = theme.name
        }
    }
}
