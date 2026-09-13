package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "user_preferences"
)

/*
 * Interface makes preferences easy to fake
 * during ViewModel unit tests.
 */
interface UserPreferences {

    val conciseReplies: Flow<Boolean>

    suspend fun setConciseReplies(
        enabled: Boolean
    )
}

class UserPreferencesRepository(
    private val context: Context
) : UserPreferences {

    companion object {

        private val CONCISE_REPLIES =
            booleanPreferencesKey(
                "concise_replies"
            )
    }

    override val conciseReplies: Flow<Boolean> =
        context.userPreferencesDataStore
            .data
            .map { preferences ->

                preferences[
                    CONCISE_REPLIES
                ] ?: false
            }

    override suspend fun setConciseReplies(
        enabled: Boolean
    ) {

        context.userPreferencesDataStore
            .edit { preferences ->

                preferences[
                    CONCISE_REPLIES
                ] = enabled
            }
    }
}