package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.ui.chat.DEFAULT_DISPLAY_NAME
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.chatPreferences by preferencesDataStore(
    name = "chat_preferences",
)

interface DisplayNameStore {
    val displayName: Flow<String>
    suspend fun setDisplayName(name: String)
}

class UserPreferences(
    private val context: Context,
) : DisplayNameStore {

    override val displayName: Flow<String> =
        context.chatPreferences.data.map { preferences ->
            preferences[DISPLAY_NAME_KEY]
                ?.trim()
                ?.ifBlank { DEFAULT_DISPLAY_NAME }
                ?: DEFAULT_DISPLAY_NAME
        }

    override suspend fun setDisplayName(name: String) {
        val safeName = name
            .trim()
            .ifBlank { DEFAULT_DISPLAY_NAME }

        context.chatPreferences.edit { preferences ->
            preferences[DISPLAY_NAME_KEY] = safeName
        }
    }

    private companion object {
        val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
    }
}
