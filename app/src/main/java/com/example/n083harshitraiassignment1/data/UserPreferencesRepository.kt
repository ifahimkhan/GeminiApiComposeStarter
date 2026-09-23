package com.example.n083harshitraiassignment1.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val userName: String = "User",
    val preferredTone: String = "Balanced",
    val autoScrollEnabled: Boolean = true
)

open class UserPreferencesRepository(
    private val context: Context? = null
) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val PREFERRED_TONE = stringPreferencesKey("preferred_tone")
        val AUTO_SCROLL = booleanPreferencesKey("auto_scroll_enabled")
    }

    open val userPreferencesFlow: Flow<UserPreferences> by lazy {
        context?.dataStore?.data
            ?.catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            ?.map { preferences ->
                val userName = preferences[PreferencesKeys.USER_NAME] ?: "User"
                val preferredTone = preferences[PreferencesKeys.PREFERRED_TONE] ?: "Balanced"
                val autoScroll = preferences[PreferencesKeys.AUTO_SCROLL] ?: true
                UserPreferences(
                    userName = userName,
                    preferredTone = preferredTone,
                    autoScrollEnabled = autoScroll
                )
            } ?: kotlinx.coroutines.flow.emptyFlow()
    }

    suspend fun updateUserName(name: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun updatePreferredTone(tone: String) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_TONE] = tone
        }
    }

    suspend fun updateAutoScroll(enabled: Boolean) {
        context?.dataStore?.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SCROLL] = enabled
        }
    }
}
