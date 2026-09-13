package com.example.c031_geminiapicompose.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

open class UserPreferencesRepository(
    private val context: Context? = null,
    private val customDataStore: DataStore<Preferences>? = null
) {
    private object PreferencesKeys {
        val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    }

    private val actualDataStore: DataStore<Preferences>?
        get() = customDataStore ?: context?.dataStore

    open val isDarkModeFlow: Flow<Boolean?>
        get() = actualDataStore?.data?.map { preferences ->
            preferences[PreferencesKeys.DARK_MODE_KEY]
        } ?: kotlinx.coroutines.flow.flowOf(null)

    open suspend fun setDarkMode(enabled: Boolean) {
        actualDataStore?.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_KEY] = enabled
        }
    }
}
