package com.rannbir.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserProfile(
    val userName: String = "Rannbir Sachdeva",
    val rollNumber: String = "N087",
    val preferredPersona: String = "Helpful AI Assistant",
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val ROLL_NUMBER = stringPreferencesKey("roll_number")
        val PREFERRED_PERSONA = stringPreferencesKey("preferred_persona")
    }

    val userProfileFlow: Flow<UserProfile> = context.dataStore.data.map { preferences ->
        UserProfile(
            userName = preferences[PreferencesKeys.USER_NAME] ?: "Rannbir Sachdeva",
            rollNumber = preferences[PreferencesKeys.ROLL_NUMBER] ?: "N087",
            preferredPersona = preferences[PreferencesKeys.PREFERRED_PERSONA] ?: "Helpful AI Assistant"
        )
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun updateRollNumber(rollNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ROLL_NUMBER] = rollNumber
        }
    }
}
