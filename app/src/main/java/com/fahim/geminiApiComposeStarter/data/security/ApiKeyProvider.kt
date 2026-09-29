package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException

import com.fahim.geminiApiComposeStarter.data.preferences.appDataStore

private const val TAG = "ApiKeyProvider"

val KEY_CIPHERTEXT = stringPreferencesKey("encrypted_api_key_ciphertext")
val KEY_IV = stringPreferencesKey("encrypted_api_key_iv")

/**
 * Interface providing access to the decrypted API key in memory.
 */
interface ApiKeyProvider {
    suspend fun getApiKey(): String?
}

/**
 * Implementation of [ApiKeyProvider] that encrypts the API key at rest using AndroidKeyStore (AES-256-GCM)
 * and stores ciphertext and IV inside Preferences DataStore. Plaintext is only decrypted in memory on demand.
 */
class SecureApiKeyProvider(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
    private val dataStore: DataStore<Preferences> = context.appDataStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ApiKeyProvider {

    override suspend fun getApiKey(): String? = withContext(ioDispatcher) {
        val prefs = dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Failed to read Preferences DataStore", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .first()

        val ciphertext = prefs[KEY_CIPHERTEXT]
        val iv = prefs[KEY_IV]

        if (!ciphertext.isNullOrBlank() && !iv.isNullOrBlank()) {
            try {
                val decrypted = cryptoManager.decrypt(ciphertext, iv)
                if (decrypted.isNotBlank()) {
                    return@withContext decrypted
                }
            } catch (e: Exception) {
                when (e) {
                    is AEADBadTagException,
                    is java.security.InvalidKeyException,
                    is GeneralSecurityException,
                    is IllegalArgumentException -> {
                        Log.e(TAG, "Decryption error occurred. Attempting recovery and re-seeding.", e)
                    }
                    else -> {
                        Log.e(TAG, "Unexpected error during API key decryption.", e)
                    }
                }
                // Corrupted ciphertext, invalid tag, or lost key: safely re-seed
                return@withContext reseedFromBuildConfig()
            }
        }

        // First launch or missing encrypted key: seed from BuildConfig if present
        reseedFromBuildConfig()
    }

    private suspend fun reseedFromBuildConfig(): String? {
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildKey.isBlank()) {
            // Clear any stale ciphertext if BuildConfig key is empty
            dataStore.edit { preferences ->
                preferences.remove(KEY_CIPHERTEXT)
                preferences.remove(KEY_IV)
            }
            return null
        }

        return try {
            cryptoManager.generateSecretKey()
            val payload = cryptoManager.encrypt(buildKey)
            dataStore.edit { preferences ->
                preferences[KEY_CIPHERTEXT] = payload.ciphertextBase64
                preferences[KEY_IV] = payload.ivBase64
            }
            buildKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt and persist API key at rest", e)
            buildKey
        }
    }
}
