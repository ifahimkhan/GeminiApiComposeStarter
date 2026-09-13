package com.fahim.geminiApiComposeStarter.data

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fahim.geminiApiComposeStarter.data.crypto.KeystoreCipher
import kotlinx.coroutines.flow.first

/**
 * Holds the Gemini API key encrypted at rest.
 *
 * The build-time value from BuildConfig is only a seed: on first launch it is sealed with a
 * Keystore AES-256-GCM key and only the ciphertext plus IV are persisted in DataStore. Every
 * later launch decrypts in memory, at the moment the GenerativeModel is created, and the
 * plaintext is never logged, shown in the UI or written anywhere else.
 */
class ApiKeyStore(
    private val dataStore: DataStore<Preferences>,
    private val buildTimeKey: String,
) {

    suspend fun apiKey(): String {
        val stored = dataStore.data.first()
        val ciphertext = stored[KEY_CIPHERTEXT]
        val iv = stored[KEY_IV]

        if (ciphertext != null && iv != null) {
            val decrypted = runCatching {
                KeystoreCipher.decrypt(
                    KeystoreCipher.Sealed(ciphertext = ciphertext.decode(), iv = iv.decode())
                )
            }.getOrNull()
            if (decrypted != null) return decrypted

            // The Keystore key was invalidated (app data cleared, device reset, backup
            // restored onto another device). Throw the unreadable blob away and re-seed.
            KeystoreCipher.deleteKey()
            clear()
        }

        return seedFromBuildConfig()
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_CIPHERTEXT)
            prefs.remove(KEY_IV)
        }
    }

    private suspend fun seedFromBuildConfig(): String {
        val plaintext = buildTimeKey.trim()
        if (plaintext.isEmpty()) return ""

        val sealed = KeystoreCipher.encrypt(plaintext)
        dataStore.edit { prefs ->
            prefs[KEY_CIPHERTEXT] = sealed.ciphertext.encode()
            prefs[KEY_IV] = sealed.iv.encode()
        }
        return plaintext
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        val KEY_CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        val KEY_IV = stringPreferencesKey("gemini_api_key_iv")
    }
}
