package com.fahim.geminiApiComposeStarter.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Extension property to get the DataStore instance from a Context. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** Snapshot of user preferences exposed to the UI. */
data class UserPrefs(
    val userName: String = "",
    val lastSessionTimestamp: Long = 0L,
)

/**
 * Persists lightweight user preferences (name, last session time) using Preferences DataStore.
 * These survive app restarts and are used to personalise the welcome message.
 */
class UserPreferences(private val context: Context) {

    private object Keys {
        val USER_NAME              = stringPreferencesKey("user_name")
        val LAST_SESSION_TIMESTAMP = longPreferencesKey("last_session_timestamp")
    }

    val userPrefsFlow: Flow<UserPrefs> = context.dataStore.data.map { prefs ->
        UserPrefs(
            userName              = prefs[Keys.USER_NAME] ?: "",
            lastSessionTimestamp  = prefs[Keys.LAST_SESSION_TIMESTAMP] ?: 0L,
        )
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun updateLastSession() {
        context.dataStore.edit { it[Keys.LAST_SESSION_TIMESTAMP] = System.currentTimeMillis() }
    }
}
