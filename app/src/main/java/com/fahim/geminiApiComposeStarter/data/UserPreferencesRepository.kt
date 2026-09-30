package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One DataStore file ("settings") for the whole app. */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Remembers user preferences. An interface so tests can use a fake. */
interface UserPreferencesRepository {
    /** true = dark, false = light, null = follow the phone's setting. */
    val darkMode: Flow<Boolean?>
    suspend fun setDarkMode(enabled: Boolean)
}

class DataStoreUserPreferencesRepository(private val context: Context) : UserPreferencesRepository {

    private val darkModeKey = booleanPreferencesKey("dark_mode")

    override val darkMode: Flow<Boolean?> =
        context.settingsDataStore.data.map { prefs -> prefs[darkModeKey] }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[darkModeKey] = enabled }
    }
}