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

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")

    override val userPreferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
        val themeModeName = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
        val dynamicColor = prefs[dynamicColorKey] ?: true
        UserPreferences(themeMode = themeMode, dynamicColor = dynamicColor)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[themeModeKey] = themeMode.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[dynamicColorKey] = enabled
        }
    }
}
