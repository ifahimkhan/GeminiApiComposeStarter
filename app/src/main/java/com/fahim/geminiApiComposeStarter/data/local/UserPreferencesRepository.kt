package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val useDynamicColor: Boolean = true,
    val isDarkMode: Boolean? = null,
)

class UserPreferencesRepository(private val context: Context) {

    private val dynamicColorKey = booleanPreferencesKey("dynamic_color_enabled")
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")
    private val hasCustomDarkModeKey = booleanPreferencesKey("has_custom_dark_mode")

    val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        val dynamicColor = prefs[dynamicColorKey] ?: true
        val hasCustom = prefs[hasCustomDarkModeKey] ?: false
        val darkMode = if (hasCustom) prefs[darkModeKey] else null
        UserPreferences(
            useDynamicColor = dynamicColor,
            isDarkMode = darkMode,
        )
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[dynamicColorKey] = enabled
        }
    }

    suspend fun setDarkMode(isDark: Boolean?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (isDark == null) {
                prefs[hasCustomDarkModeKey] = false
                prefs.remove(darkModeKey)
            } else {
                prefs[hasCustomDarkModeKey] = true
                prefs[darkModeKey] = isDark
            }
        }
    }
}
