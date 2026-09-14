package com.fahim.geminiApiComposeStarter.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

/** Small, low-stakes preferences that personalise the experience across app restarts. */
class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    override val useDynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC_COLOR] ?: true
    }

    override suspend fun setUseDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    companion object {
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    }
}
