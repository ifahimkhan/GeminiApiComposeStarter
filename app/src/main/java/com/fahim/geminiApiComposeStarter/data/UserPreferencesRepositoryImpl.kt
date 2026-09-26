package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    override val darkModeOverride: Flow<Boolean?> =
        context.dataStore.data.map { it[DARK_MODE_KEY] }

    override suspend fun setDarkModeOverride(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }
}
