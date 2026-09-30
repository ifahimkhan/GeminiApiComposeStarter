package com.fahim.geminiApiComposeStarter.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Preference key reserved exclusively for user theme preference.
 * Security keys (e.g., encrypted_api_key_ciphertext) are maintained separately.
 */
val KEY_THEME_MODE = stringPreferencesKey("user_theme_mode")

/**
 * Abstraction for observing and saving application user preferences.
 */
interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(themeMode: ThemeMode)
}

/**
 * Preferences DataStore backed implementation of [UserPreferencesRepository].
 * Operations are safely dispatched on [ioDispatcher].
 */
class UserPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserPreferencesRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            ThemeMode.fromStorage(preferences[KEY_THEME_MODE])
        }
        .flowOn(ioDispatcher)

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                preferences[KEY_THEME_MODE] = themeMode.name
            }
        }
    }
}
