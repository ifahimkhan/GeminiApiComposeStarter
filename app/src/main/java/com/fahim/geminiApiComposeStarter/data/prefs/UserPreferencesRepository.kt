package com.fahim.geminiApiComposeStarter.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserPreferences(
    val autoSendVoice: Boolean = false,
    val dynamicColor: Boolean = true,
)

private val Context.userPreferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

class UserPreferencesRepository(
    context: Context,
) {
    private val store = context.applicationContext.userPreferencesStore

    val preferences: Flow<UserPreferences> = store.data.map { prefs ->
        UserPreferences(
            autoSendVoice = prefs[Keys.AUTO_SEND_VOICE] ?: false,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
        )
    }

    suspend fun setAutoSendVoice(enabled: Boolean) {
        store.edit { it[Keys.AUTO_SEND_VOICE] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    private object Keys {
        val AUTO_SEND_VOICE = booleanPreferencesKey("auto_send_voice")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
