package com.example.c001manavassignment1.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.c001manavassignment1.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "api_key_prefs")

interface IApiKeyManager {
    suspend fun initializeApiKey()
    suspend fun getApiKey(): String
}

class ApiKeyManager(private val context: Context) : IApiKeyManager {

    private val keystoreManager = KeystoreManager()

    private val ciphertextKey = stringPreferencesKey("encrypted_api_key")
    private val ivKey = stringPreferencesKey("api_key_iv")

    override suspend fun initializeApiKey() {
        val currentEncryptedKey = context.dataStore.data.map { it[ciphertextKey] }.first()
        if (currentEncryptedKey == null && BuildConfig.GEMINI_API_KEY.isNotEmpty()) {
            val encrypted = keystoreManager.encrypt(BuildConfig.GEMINI_API_KEY)
            context.dataStore.edit { prefs ->
                prefs[ciphertextKey] = Base64.encodeToString(encrypted.ciphertext, Base64.DEFAULT)
                prefs[ivKey] = Base64.encodeToString(encrypted.iv, Base64.DEFAULT)
            }
        }
    }

    override suspend fun getApiKey(): String {
        val prefs = context.dataStore.data.first()
        val ciphertextBase64 = prefs[ciphertextKey] ?: return ""
        val ivBase64 = prefs[ivKey] ?: return ""

        val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)
        val iv = Base64.decode(ivBase64, Base64.DEFAULT)

        return keystoreManager.decrypt(ciphertext, iv)
    }
}
