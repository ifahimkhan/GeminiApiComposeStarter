package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userDataStore by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface UserPreferences {
    val theme: Flow<ThemeMode>
    suspend fun setTheme(mode: ThemeMode)
}

class DataStoreUserPreferences(context: Context) : UserPreferences {
    private val store = context.applicationContext.userDataStore
    private val themeKey = stringPreferencesKey("theme")

    override val theme = store.data.map { preferences ->
        ThemeMode.entries.firstOrNull { it.name == preferences[themeKey] } ?: ThemeMode.SYSTEM
    }

    override suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[themeKey] = mode.name }
    }
}
