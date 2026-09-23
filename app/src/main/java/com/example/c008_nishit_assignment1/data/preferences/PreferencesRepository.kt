package com.example.c008_nishit_assignment1.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * PreferencesRepository persists user preferences like theme settings using DataStore.
 */
open class PreferencesRepository(private val context: Context? = null) {

    private val isDarkModeKey = booleanPreferencesKey("is_dark_mode")

    open val isDarkMode: Flow<Boolean?>
        get() {
            val ctx = context ?: return flowOf(null)
            return try {
                ctx.dataStore.data
                    .map { preferences -> preferences[isDarkModeKey] }
                    .catch { emit(null) }
            } catch (e: Exception) {
                flowOf(null)
            }
        }

    open suspend fun setDarkMode(enabled: Boolean) {
        context?.let { ctx ->
            try {
                ctx.dataStore.edit { preferences ->
                    preferences[isDarkModeKey] = enabled
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
