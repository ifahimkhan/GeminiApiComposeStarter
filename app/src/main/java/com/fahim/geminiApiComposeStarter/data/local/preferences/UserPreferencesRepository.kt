package com.fahim.geminiApiComposeStarter.data.local.preferences

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

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "vivaan_user_preferences")

data class UserPreferences(
    val studentName: String = "Vivaan Shrivastava",
    val rollNumber: String = "N157",
    val isDynamicColorEnabled: Boolean = true,
    val selectedModel: String = "gemini-1.5-flash",
    val isHapticFeedbackEnabled: Boolean = true
)

/**
 * Repository for lightweight user settings and UI preferences using Preferences DataStore.
 * Designed according to Android Jetpack DataStore best practices.
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_STUDENT_NAME = stringPreferencesKey("student_name")
        val KEY_ROLL_NUMBER = stringPreferencesKey("roll_number")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                studentName = preferences[KEY_STUDENT_NAME] ?: "Vivaan Shrivastava",
                rollNumber = preferences[KEY_ROLL_NUMBER] ?: "N157",
                isDynamicColorEnabled = preferences[KEY_DYNAMIC_COLOR] ?: true,
                selectedModel = preferences[KEY_SELECTED_MODEL] ?: "gemini-1.5-flash",
                isHapticFeedbackEnabled = preferences[KEY_HAPTIC_FEEDBACK] ?: true
            )
        }

    suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun updateSelectedModel(model: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun updateHapticFeedback(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_HAPTIC_FEEDBACK] = enabled
        }
    }
}
