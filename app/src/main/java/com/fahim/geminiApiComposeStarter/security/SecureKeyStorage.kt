package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.BuildConfig

class SecureKeyStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_shared_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun storeApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_GEMINI_API, apiKey).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_GEMINI_API, null)
            ?: BuildConfig.GEMINI_API_KEY
    }

    companion object {
        private const val KEY_GEMINI_API = "encrypted_gemini_api_key"
    }
}