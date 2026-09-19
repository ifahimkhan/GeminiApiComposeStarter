package com.nmims.c010.gemini.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_prefs")

data class UserPrefs(
    val themeMode: String = "system",   // system | light | dark
    val modelName: String = "gemini-1.5-flash",
    val speakReplies: Boolean = false
)

class UserPrefsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val MODEL = stringPreferencesKey("model_name")
        val SPEAK = booleanPreferencesKey("speak_replies")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            themeMode = p[Keys.THEME] ?: "system",
            modelName = p[Keys.MODEL] ?: "gemini-1.5-flash",
            speakReplies = p[Keys.SPEAK] ?: false
        )
    }

    suspend fun setTheme(mode: String) =
        context.dataStore.edit { it[Keys.THEME] = mode }

    suspend fun setModel(name: String) =
        context.dataStore.edit { it[Keys.MODEL] = name }
}
