package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "appearance")

enum class ThemeMode { SYSTEM, DARK, LIGHT }

interface ThemePreferences {
    val mode: Flow<ThemeMode>
    suspend fun setMode(mode: ThemeMode)
}

class DataStoreThemePreferences(private val context: Context) : ThemePreferences {
    private val key = stringPreferencesKey("theme_mode")
    override val mode: Flow<ThemeMode> = context.themeDataStore.data.map { preferences ->
        runCatching { ThemeMode.valueOf(preferences[key] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    override suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[key] = mode.name }
    }
}
