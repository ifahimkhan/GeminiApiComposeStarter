package com.fahim.geminiApiComposeStarter.data

import android.content.Context

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)


interface UserPreferences {

    val userName: Flow<String>

    val darkMode: Flow<Boolean>

    suspend fun saveUserName(
        name: String
    )

    suspend fun saveDarkMode(
        enabled: Boolean
    )
}


class UserPreferencesImpl(
    private val context: Context
) : UserPreferences {

    companion object {

        private val USER_NAME =
            stringPreferencesKey("user_name")

        private val DARK_MODE =
            booleanPreferencesKey("dark_mode")
    }


    override val userName: Flow<String> =
        context.userPreferencesDataStore.data
            .map { preferences ->
                preferences[USER_NAME] ?: ""
            }


    override val darkMode: Flow<Boolean> =
        context.userPreferencesDataStore.data
            .map { preferences ->
                preferences[DARK_MODE] ?: false
            }


    override suspend fun saveUserName(
        name: String
    ) {
        context.userPreferencesDataStore.edit { preferences ->

            preferences[USER_NAME] =
                name.trim()
        }
    }


    override suspend fun saveDarkMode(
        enabled: Boolean
    ) {
        context.userPreferencesDataStore.edit { preferences ->

            preferences[DARK_MODE] =
                enabled
        }
    }
}