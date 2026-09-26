package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_FILE_NAME = "secure_gemini_prefs"
private const val PREF_KEY_API_KEY = "gemini_api_key"

/**
 * Persists the Gemini API key as ciphertext, encrypted with an AES-256-GCM key that
 * [MasterKey] generates and keeps inside the Android Keystore. The plaintext key only
 * exists in memory for the moment [getOrSeedApiKey] is called to build the GenerativeModel;
 * it is never logged, toasted or displayed.
 */
class SecureApiKeyStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Returns the previously encrypted key if one is stored; otherwise seeds the encrypted
     * store from [buildTimeKey] (read from BuildConfig, itself sourced from local.properties
     * or a CI environment variable) so only ciphertext is persisted from then on.
     */
    fun getOrSeedApiKey(buildTimeKey: String): String {
        prefs.getString(PREF_KEY_API_KEY, null)?.let { return it }
        if (buildTimeKey.isBlank()) return buildTimeKey
        prefs.edit().putString(PREF_KEY_API_KEY, buildTimeKey).apply()
        return buildTimeKey
    }
}
