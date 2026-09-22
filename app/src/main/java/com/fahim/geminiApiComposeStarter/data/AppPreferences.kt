package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferences by preferencesDataStore(name = "user_preferences")

/** A small user-visible preference; null means follow the system theme. */
class AppPreferences(private val context: Context) {
    val darkMode: Flow<Boolean?> = context.userPreferences.data.map { it[DARK_MODE] }

    suspend fun setDarkMode(enabled: Boolean) {
        context.userPreferences.edit { it[DARK_MODE] = enabled }
    }

    private companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }
}
