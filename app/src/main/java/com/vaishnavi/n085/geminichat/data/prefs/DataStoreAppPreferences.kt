package com.vaishnavi.n085.geminichat.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Preferences DataStore implementation: user name + encrypted API key (ciphertext only). */
class DataStoreAppPreferences(private val context: Context) : AppPreferences {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val API_KEY_IV = stringPreferencesKey("api_key_iv")
        val API_KEY_CIPHERTEXT = stringPreferencesKey("api_key_ciphertext")
    }

    override val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: DEFAULT_USER_NAME
    }

    override val apiKeyCipher: Flow<ApiKeyCipher?> = context.dataStore.data.map { prefs ->
        val iv = prefs[Keys.API_KEY_IV]
        val ciphertext = prefs[Keys.API_KEY_CIPHERTEXT]
        if (iv.isNullOrBlank() || ciphertext.isNullOrBlank()) null else ApiKeyCipher(iv, ciphertext)
    }

    override suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    override suspend fun saveApiKeyCipher(iv: String, ciphertext: String) {
        context.dataStore.edit {
            it[Keys.API_KEY_IV] = iv
            it[Keys.API_KEY_CIPHERTEXT] = ciphertext
        }
    }

    companion object {
        const val DEFAULT_USER_NAME = "Vaishnavi"
    }
}
