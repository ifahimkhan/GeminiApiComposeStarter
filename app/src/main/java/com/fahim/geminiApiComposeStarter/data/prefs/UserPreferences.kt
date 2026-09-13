package com.fahim.geminiApiComposeStarter.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.text.get

private val Context.userDataStore by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface PreferencesApi {
    val themeMode: Flow<ThemeMode>
    val dynamicColorEnabled: Flow<Boolean>
    val username: Flow<String?>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setUsername(name: String)
}

class UserPreferences(private val context: Context) : PreferencesApi {

    private val themeModeKey = intPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val usernameKey = stringPreferencesKey("username")

    override val themeMode: Flow<ThemeMode> = context.userDataStore.data.map { prefs ->
        when (prefs[themeModeKey] ?: 0) {
            1 -> ThemeMode.LIGHT
            2 -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    override val dynamicColorEnabled: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[dynamicColorKey] ?: true
    }

    override val username: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[usernameKey]
    }

    override suspend fun setUsername(name: String) {
        context.userDataStore.edit { prefs -> prefs[usernameKey] = name.trim() }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.userDataStore.edit { prefs ->
            prefs[themeModeKey] = when (mode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            }
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.userDataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
    }
}
