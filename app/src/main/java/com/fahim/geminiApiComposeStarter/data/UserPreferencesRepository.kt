package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** User preferences that persist across launches. */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")
private val THEME_MODE = stringPreferencesKey("theme_mode")

class DataStoreUserPreferencesRepository(context: Context) : UserPreferencesRepository {

    private val dataStore = context.applicationContext.userPrefsDataStore

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }
}
