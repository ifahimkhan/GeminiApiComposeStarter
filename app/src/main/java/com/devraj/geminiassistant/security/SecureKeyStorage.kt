package com.devraj.geminiassistant.security

import android.content.Context
import android.content.SharedPreferences
import com.devraj.geminiassistant.BuildConfig

class SecureKeyStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "devraj_secure_prefs"
        private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_api_key"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keystoreHelper = KeystoreHelper()

    init {
        initializeKeyIfAvailable()
    }

    private fun initializeKeyIfAvailable() {
        val storedEncryptedKey = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        if (storedEncryptedKey.isNullOrEmpty() && BuildConfig.GEMINI_API_KEY.isNotEmpty()) {
            saveApiKey(BuildConfig.GEMINI_API_KEY)
        }
    }

    fun saveApiKey(plainApiKey: String) {
        if (plainApiKey.isBlank()) return
        val encrypted = keystoreHelper.encrypt(plainApiKey)
        prefs.edit().putString(KEY_ENCRYPTED_API_KEY, encrypted).apply()
    }

    fun getDecryptedApiKey(): String {
        val encrypted = prefs.getString(KEY_ENCRYPTED_API_KEY, null) ?: return ""
        return try {
            keystoreHelper.decrypt(encrypted)
        } catch (e: Exception) {
            ""
        }
    }

    fun hasApiKey(): Boolean {
        val encrypted = prefs.getString(KEY_ENCRYPTED_API_KEY, null)
        return !encrypted.isNullOrEmpty() || BuildConfig.GEMINI_API_KEY.isNotEmpty()
    }
}
