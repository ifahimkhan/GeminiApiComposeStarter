package com.silas.geminichat.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "user_settings")

/** Remembers user preferences across app restarts. */
interface UserPreferencesRepository {
    val userName: Flow<String>
    suspend fun setUserName(name: String)
}

class DataStoreUserPreferencesRepository(private val context: Context) :
    UserPreferencesRepository {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
    }

    override val userName: Flow<String> =
        context.settingsDataStore.data.map { it[Keys.USER_NAME] ?: "" }

    override suspend fun setUserName(name: String) {
        context.settingsDataStore.edit { it[Keys.USER_NAME] = name }
    }
}
