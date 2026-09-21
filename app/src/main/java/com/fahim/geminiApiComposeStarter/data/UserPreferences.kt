package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import androidx.datastore.preferences.core.emptyPreferences

private val Context.settings by preferencesDataStore("user_preferences")
interface UserPreferences {
    val compactMessages: Flow<Boolean>
    val darkMode: Flow<Boolean?>
    suspend fun setCompactMessages(value: Boolean)
    suspend fun setDarkMode(value: Boolean?)
}
class DataStoreUserPreferences(private val context: Context) : UserPreferences {
    private val compact = booleanPreferencesKey("compact_messages")
    private val darkModeKey = booleanPreferencesKey("dark_mode_override")
    private val darkModeSetKey = booleanPreferencesKey("dark_mode_set")
    override val compactMessages = context.settings.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { it[compact] ?: false }
    override val darkMode = context.settings.data.catch {
        if (it is IOException) emit(emptyPreferences()) else throw it
    }.map { prefs -> if (prefs[darkModeSetKey] == true) prefs[darkModeKey] else null }
    override suspend fun setCompactMessages(value: Boolean) {
        context.settings.edit { it[compact] = value }
    }
    override suspend fun setDarkMode(value: Boolean?) {
        context.settings.edit {
            if (value == null) {
                it.remove(darkModeKey); it.remove(darkModeSetKey)
            } else {
                it[darkModeKey] = value; it[darkModeSetKey] = true
            }
        }
    }
}
