package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val isDarkTheme: Flow<Boolean>
    suspend fun setDarkTheme(isDark: Boolean)
}

class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    override val isDarkTheme: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_DARK_THEME] ?: false
        }

    override suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = isDark
        }
    }
}
