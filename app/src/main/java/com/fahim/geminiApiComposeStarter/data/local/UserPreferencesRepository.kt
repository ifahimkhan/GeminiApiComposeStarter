package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private val encryptedApiKeyKey = stringPreferencesKey("encrypted_api_key")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val encryptedApiKey: Flow<String> = applicationContext.dataStore.data.map { preferences ->
        preferences[encryptedApiKeyKey] ?: ""
    }

    val themeMode: Flow<ThemeMode> = applicationContext.dataStore.data.map { preferences ->
        val savedModeName = preferences[themeModeKey] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(savedModeName)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun saveEncryptedApiKey(encryptedKey: String) {
        applicationContext.dataStore.edit { preferences ->
            preferences[encryptedApiKeyKey] = encryptedKey
        }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        applicationContext.dataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.name
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesRepository? = null

        fun getInstance(context: Context): UserPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
