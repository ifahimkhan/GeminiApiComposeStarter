package com.fahim.geminiApiComposeStarter.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pitwall_prefs")

class ApiKeyDataStore(private val context: Context) {

    companion object {
        private val CIPHERTEXT_KEY = stringPreferencesKey("encrypted_api_key_ciphertext")
        private val IV_KEY = stringPreferencesKey("encrypted_api_key_iv")
    }

    val encryptedApiKeyFlow: Flow<Pair<String, String>> = context.dataStore.data
        .map { preferences ->
            val ciphertext = preferences[CIPHERTEXT_KEY].orEmpty()
            val iv = preferences[IV_KEY].orEmpty()
            Pair(ciphertext, iv)
        }

    suspend fun saveEncryptedApiKey(ciphertext: String, iv: String) {
        context.dataStore.edit { preferences ->
            preferences[CIPHERTEXT_KEY] = ciphertext
            preferences[IV_KEY] = iv
        }
    }
}
