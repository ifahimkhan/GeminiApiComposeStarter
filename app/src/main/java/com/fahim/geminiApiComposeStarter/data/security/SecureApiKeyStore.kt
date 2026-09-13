package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class SecureApiKeyStore(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
) {

    private companion object {
        val KEY_ENCRYPTED_API = stringPreferencesKey("encrypted_api_key")
    }

    suspend fun getApiKey(fallback: String): String = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.secureKeyDataStore.data.first()
        val stored = prefs[KEY_ENCRYPTED_API]
        when {
            stored != null -> runCatching { cryptoManager.decrypt(stored) }
                .getOrElse { fallback }
            fallback.isNotBlank() -> {
                val encrypted = cryptoManager.encrypt(fallback)
                context.applicationContext.secureKeyDataStore.edit {
                    it[KEY_ENCRYPTED_API] = encrypted
                }
                fallback
            }
            else -> ""
        }
    }

    fun getApiKeyBlocking(fallback: String): String = runBlocking { getApiKey(fallback) }
}