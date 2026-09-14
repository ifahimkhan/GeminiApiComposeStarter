package com.example.myapplication.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val autoScrollEnabled: Flow<Boolean>
    val darkModeOverride: Flow<Boolean?>
    suspend fun setAutoScroll(enabled: Boolean)
    suspend fun setDarkMode(enabled: Boolean?)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private val autoScrollKey = booleanPreferencesKey("auto_scroll_enabled")
    private val darkModeKey = booleanPreferencesKey("dark_mode_override")

    override val autoScrollEnabled: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[autoScrollKey] ?: true
    }

    override val darkModeOverride: Flow<Boolean?> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[darkModeKey]
    }

    override suspend fun setAutoScroll(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[autoScrollKey] = enabled
        }
    }

    override suspend fun setDarkMode(enabled: Boolean?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (enabled == null) {
                prefs.remove(darkModeKey)
            } else {
                prefs[darkModeKey] = enabled
            }
        }
    }
}
