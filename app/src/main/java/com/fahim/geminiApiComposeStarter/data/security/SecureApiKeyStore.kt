package com.fahim.geminiApiComposeStarter.data.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.Base64

/**
 * Keeps the Gemini API key encrypted at rest.
 *
 * On first launch [provision] encrypts the build-time key with [cipher] and persists only the
 * ciphertext (plus a SHA-256 fingerprint used to notice when the build key changes).
 * [decrypt] is called only when the GenerativeModel is created; the plaintext is never
 * persisted, logged or shown.
 */
class SecureApiKeyStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: SecretCipher,
    private val buildTimeKey: String,
) {
    private val mutex = Mutex()

    /** Encrypts and stores [buildTimeKey] unless an up-to-date ciphertext already exists. */
    suspend fun provision() = mutex.withLock {
        if (buildTimeKey.isBlank()) return@withLock
        val fingerprint = fingerprint(buildTimeKey)
        val prefs = dataStore.data.first()
        if (prefs[CIPHERTEXT] != null && prefs[FINGERPRINT] == fingerprint) return@withLock

        val ciphertext = cipher.encrypt(buildTimeKey.toByteArray(Charsets.UTF_8))
        dataStore.edit {
            it[CIPHERTEXT] = Base64.getEncoder().encodeToString(ciphertext)
            it[FINGERPRINT] = fingerprint
        }
    }

    /** Returns the decrypted key, or null if none is stored or it can no longer be decrypted. */
    suspend fun decrypt(): String? {
        provision()
        tryDecrypt()?.let { return it }
        // Keystore key lost (e.g. restored onto a new device) or data corrupted: drop the
        // stale blob, re-encrypt from the build-time key and try once more.
        dataStore.edit { it.remove(CIPHERTEXT); it.remove(FINGERPRINT) }
        provision()
        return tryDecrypt()
    }

    private suspend fun tryDecrypt(): String? {
        val encoded = dataStore.data.first()[CIPHERTEXT] ?: return null
        return try {
            String(cipher.decrypt(Base64.getDecoder().decode(encoded)), Charsets.UTF_8)
        } catch (e: Exception) {
            null // Deliberately not logged: the exception could echo key material.
        }
    }

    private fun fingerprint(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
        val FINGERPRINT = stringPreferencesKey("gemini_api_key_fingerprint")
    }
}
