package com.example.assignment1_c061.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    val lastQuery: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_QUERY] ?: ""
    }

    suspend fun saveLastQuery(query: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_QUERY] = query
        }
    }

    companion object {
        private val KEY_LAST_QUERY = stringPreferencesKey("last_user_query")
    }
}
