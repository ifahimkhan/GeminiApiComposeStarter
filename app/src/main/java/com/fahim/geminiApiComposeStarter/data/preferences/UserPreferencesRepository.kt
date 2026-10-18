package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val isDarkMode: Flow<Boolean?>
    suspend fun setDarkMode(isDark: Boolean?)
    suspend fun toggleDarkMode()
}

class UserPreferencesRepositoryImpl(
    private val context: Context,
) : UserPreferencesRepository {

    private val isDarkModeKey = booleanPreferencesKey("pref_dark_mode")

    override val isDarkMode: Flow<Boolean?> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[isDarkModeKey]
    }

    override suspend fun setDarkMode(isDark: Boolean?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (isDark == null) {
                prefs.remove(isDarkModeKey)
            } else {
                prefs[isDarkModeKey] = isDark
            }
        }
    }

    override suspend fun toggleDarkMode() {
        val current = context.userPreferencesDataStore.data.map { it[isDarkModeKey] }.first()
        // If null or false, switch to true; if true, switch to false
        val next = when (current) {
            true -> false
            false -> true
            null -> true
        }
        setDarkMode(next)
    }
}
