package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Storage interface for managing the Gemini API key securely.
 */
interface ApiKeyStorage {
    fun getApiKey(): String?
    fun saveApiKey(key: String)
    fun clearApiKey()
    fun hasApiKey(): Boolean
}

/**
 * Implementation of [ApiKeyStorage] using hardware-backed [EncryptedSharedPreferences].
 * Keys and values are encrypted using AES-256-GCM and AES-256-SIV via Android Keystore.
 */
class SecureApiKeyStorage(context: Context) : ApiKeyStorage {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun getApiKey(): String? {
        val key = sharedPreferences.getString(KEY_GEMINI_API_KEY, null)?.trim()
        return key.takeIf { !it.isNullOrBlank() }
    }

    override fun saveApiKey(key: String) {
        sharedPreferences.edit()
            .putString(KEY_GEMINI_API_KEY, key.trim())
            .apply()
    }

    override fun clearApiKey() {
        sharedPreferences.edit()
            .remove(KEY_GEMINI_API_KEY)
            .apply()
    }

    override fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    companion object {
        private const val PREFS_FILENAME = "secure_gemini_prefs"
        private const val KEY_GEMINI_API_KEY = "encrypted_gemini_api_key"
    }
}
