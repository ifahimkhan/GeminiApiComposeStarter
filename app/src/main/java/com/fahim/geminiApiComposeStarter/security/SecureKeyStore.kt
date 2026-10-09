package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "key_vault")

/**
 * Encrypt-at-rest flow for the Gemini API key.
 *
 *  1. First launch: BuildConfig.GEMINI_API_KEY -> AES-256-GCM (Keystore key) -> ciphertext.
 *  2. Only the Base64 ciphertext (+ a short, irreversible SHA-256 fingerprint used to
 *     detect when the key in local.properties changes) is written to DataStore.
 *  3. [decryptApiKey] is called only when the GenerativeModel is created.
 *
 * The plaintext is never logged, toasted or displayed.
 */
class SecureKeyStore(
    context: Context,
    private val cipher: AesGcmKeystore = AesGcmKeystore(),
) {
    private val store = context.secureDataStore
    private val mutex = Mutex()

    suspend fun ensureEncrypted(buildKey: String = BuildConfig.GEMINI_API_KEY) =
        mutex.withLock { ensureEncryptedLocked(buildKey) }

    suspend fun decryptApiKey(): String? = mutex.withLock {
        ensureEncryptedLocked(BuildConfig.GEMINI_API_KEY)
        val stored = store.data.first()[CIPHERTEXT] ?: return@withLock null
        try {
            cipher.decrypt(stored)
        } catch (e: Exception) {
            // Keystore key lost (e.g. data restored from a backup onto a new device):
            // throw away the unreadable ciphertext and re-encrypt once.
            store.edit { it.remove(CIPHERTEXT); it.remove(FINGERPRINT) }
            ensureEncryptedLocked(BuildConfig.GEMINI_API_KEY)
            store.data.first()[CIPHERTEXT]?.let(cipher::decrypt)
        }
    }

    private suspend fun ensureEncryptedLocked(buildKey: String) {
        if (buildKey.isBlank()) return
        val fingerprint = fingerprint(buildKey)
        val prefs = store.data.first()
        if (prefs[CIPHERTEXT] != null && prefs[FINGERPRINT] == fingerprint) return

        val encrypted = cipher.encrypt(buildKey)
        store.edit {
            it[CIPHERTEXT] = encrypted
            it[FINGERPRINT] = fingerprint
        }
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val CIPHERTEXT = stringPreferencesKey("enc_api_key")
        val FINGERPRINT = stringPreferencesKey("enc_api_key_fp")
    }
}
