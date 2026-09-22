package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.security.SecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface PreferencesRepository {
    val themePreference: Flow<String>
    val isVoiceEnabled: Flow<Boolean>
    val encryptedApiKey: Flow<SecurityManager.EncryptedResult?>
    suspend fun setThemePreference(theme: String)
    suspend fun setVoiceEnabled(enabled: Boolean)
    suspend fun saveEncryptedApiKey(ciphertext: String, iv: String)
}

class PreferencesRepositoryImpl(private val context: Context) : PreferencesRepository {

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val VOICE_ENABLED_KEY = booleanPreferencesKey("voice_enabled_preference")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val ENCRYPTED_API_KEY_IV = stringPreferencesKey("encrypted_api_key_iv")
    }

    override val themePreference: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "SYSTEM"
    }

    override val isVoiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_ENABLED_KEY] ?: true
    }

    override val encryptedApiKey: Flow<SecurityManager.EncryptedResult?> = context.dataStore.data.map { preferences ->
        val ciphertext = preferences[ENCRYPTED_API_KEY] ?: ""
        val iv = preferences[ENCRYPTED_API_KEY_IV] ?: ""
        if (ciphertext.isNotEmpty() && iv.isNotEmpty()) {
            SecurityManager.EncryptedResult(ciphertext, iv)
        } else {
            null
        }
    }

    override suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    override suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_ENABLED_KEY] = enabled
        }
    }

    override suspend fun saveEncryptedApiKey(ciphertext: String, iv: String) {
        context.dataStore.edit { preferences ->
            preferences[ENCRYPTED_API_KEY] = ciphertext
            preferences[ENCRYPTED_API_KEY_IV] = iv
        }
    }
}
