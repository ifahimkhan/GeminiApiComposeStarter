package com.fahim.geminiApiComposeStarter.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPrefsStore by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserPreferences(
    val preferredName: String = "",
    val useDynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedModel: String = "gemini-3.6-flash"
)

/**
 * Persists user settings including theme mode, preferred name, and model choice.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val PREFERRED_NAME = stringPreferencesKey("preferred_name")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_MODEL = stringPreferencesKey("selected_model")
    }

    val preferences: Flow<UserPreferences> = context.userPrefsStore.data.map { prefs ->
        val themeModeString = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeString) }.getOrDefault(ThemeMode.SYSTEM)

        UserPreferences(
            preferredName = prefs[Keys.PREFERRED_NAME] ?: "",
            useDynamicColor = prefs[Keys.USE_DYNAMIC_COLOR] ?: true,
            themeMode = themeMode,
            selectedModel = prefs[Keys.SELECTED_MODEL] ?: "gemini-3.6-flash"
        )
    }

    suspend fun setPreferredName(name: String) {
        context.userPrefsStore.edit { it[Keys.PREFERRED_NAME] = name }
    }

    suspend fun setUseDynamicColor(enabled: Boolean) {
        context.userPrefsStore.edit { it[Keys.USE_DYNAMIC_COLOR] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userPrefsStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSelectedModel(modelName: String) {
        context.userPrefsStore.edit { it[Keys.SELECTED_MODEL] = modelName }
    }
}
