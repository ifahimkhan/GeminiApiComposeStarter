package com.fahim.geminiApiComposeStarter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.security.EncryptedPayload
import com.fahim.geminiApiComposeStarter.security.KeystoreCryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

data class UserPreferences(
    val userName: String = "User",
    val selectedModel: String = "gemini-1.5-flash",
    val tone: String = "Balanced",
    val isKeyConfigured: Boolean = false,
)

class UserPreferencesRepository(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(),
) {
    private object PreferencesKeys {
        val KEY_ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_api_key")
        val KEY_API_KEY_IV = stringPreferencesKey("api_key_iv")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_TONE = stringPreferencesKey("assistant_tone")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val hasKey = !preferences[PreferencesKeys.KEY_ENCRYPTED_API_KEY].isNullOrEmpty() ||
                    BuildConfig.GEMINI_API_KEY.isNotBlank()
            UserPreferences(
                userName = preferences[PreferencesKeys.KEY_USER_NAME] ?: "User",
                selectedModel = preferences[PreferencesKeys.KEY_SELECTED_MODEL] ?: "gemini-1.5-flash",
                tone = preferences[PreferencesKeys.KEY_TONE] ?: "Balanced",
                isKeyConfigured = hasKey,
            )
        }

    /**
     * Initializes the encrypted API key on first launch using BuildConfig fallback
     * if no key is yet saved in DataStore.
     */
    suspend fun initializeKeyAtRest() {
        val preferences = context.dataStore.data.first()
        val existingCipher = preferences[PreferencesKeys.KEY_ENCRYPTED_API_KEY]
        if (existingCipher.isNullOrEmpty() && BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            saveEncryptedApiKey(BuildConfig.GEMINI_API_KEY)
        }
    }

    /**
     * Encrypts the provided API key at rest using Android Keystore AES-256-GCM.
     */
    suspend fun saveEncryptedApiKey(apiKey: String) {
        val encrypted = cryptoManager.encrypt(apiKey.trim())
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_ENCRYPTED_API_KEY] = encrypted.ciphertextBase64
            preferences[PreferencesKeys.KEY_API_KEY_IV] = encrypted.ivBase64
        }
    }

    /**
     * Decrypts the API key in memory only for the moment GenerativeModel is instantiated.
     */
    suspend fun getDecryptedApiKey(): String {
        val preferences = context.dataStore.data.first()
        val ciphertext = preferences[PreferencesKeys.KEY_ENCRYPTED_API_KEY] ?: ""
        val iv = preferences[PreferencesKeys.KEY_API_KEY_IV] ?: ""

        if (ciphertext.isNotEmpty() && iv.isNotEmpty()) {
            return try {
                cryptoManager.decrypt(EncryptedPayload(ciphertext, iv))
            } catch (e: Exception) {
                // Fallback to BuildConfig if decryption of corrupt state fails
                BuildConfig.GEMINI_API_KEY
            }
        }
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun updatePreferences(userName: String, model: String, tone: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_USER_NAME] = userName.trim()
            preferences[PreferencesKeys.KEY_SELECTED_MODEL] = model
            preferences[PreferencesKeys.KEY_TONE] = tone
        }
    }
}
