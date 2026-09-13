package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

class PreferencesSettingsRepository(
    context: Context,
) : SettingsRepository {

    private val dataStore = context.applicationContext.userPreferences

    override val compactBubbles: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_COMPACT_BUBBLES] ?: false
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]
            ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    override suspend fun setCompactBubbles(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_COMPACT_BUBBLES] = enabled }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_COMPACT_BUBBLES = booleanPreferencesKey("compact_bubbles")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}