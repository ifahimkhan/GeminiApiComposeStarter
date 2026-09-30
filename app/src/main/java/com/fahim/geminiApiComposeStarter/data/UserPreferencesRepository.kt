package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val userName: Flow<String>
    suspend fun setUserName(name: String)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private val USER_NAME_KEY = stringPreferencesKey("user_name")

    override val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY] ?: "User"
    }

    override suspend fun setUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
        }
    }
}