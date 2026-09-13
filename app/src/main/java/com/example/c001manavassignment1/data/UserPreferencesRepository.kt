package com.example.c001manavassignment1.data

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface IUserPreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>
    suspend fun updateDarkMode(isDarkMode: Boolean)
}

class UserPreferencesRepository(private val context: Context) : IUserPreferencesRepository {

    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    override val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isDarkMode = preferences[isDarkModeKey] ?: false
            UserPreferences(isDarkMode)
        }

    override suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isDarkModeKey] = isDarkMode
        }
    }
}

data class UserPreferences(val isDarkMode: Boolean)
