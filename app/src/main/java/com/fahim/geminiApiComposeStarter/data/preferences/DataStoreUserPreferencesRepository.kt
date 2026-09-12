package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class DataStoreUserPreferencesRepository(
    context: Context,
) : UserPreferencesRepository {

    private val dataStore = context.applicationContext.userPreferencesDataStore

    override val displayName: Flow<String> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[DISPLAY_NAME].orEmpty() }

    override suspend fun setDisplayName(name: String) {
        dataStore.edit { preferences ->
            preferences[DISPLAY_NAME] = name.trim().take(MAX_NAME_LENGTH)
        }
    }

    private companion object {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        const val MAX_NAME_LENGTH = 30
    }
}
