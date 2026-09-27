package com.fahim.geminiApiComposeStarter.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.Base64

/**
 * Keeps the Gemini API key encrypted at rest.
 *
 * Flow:
 *  1. First launch: [ensureEncrypted] encrypts the build-time key with a Keystore AES-256-GCM key
 *     and persists ONLY the IV + ciphertext (Base64) in a Preferences DataStore.
 *  2. [decryptApiKey] is called only by the Gemini repository at the moment it builds the
 *     GenerativeModel. The plaintext lives in memory only; it is never logged or displayed.
 *
 * A SHA-256 fingerprint of the build key is stored so that if the developer changes the key in
 * local.properties, the stored ciphertext is replaced on the next launch.
 */
class SecureApiKeyStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
    private val buildTimeKeyProvider: () -> String,
) {
    private val mutex = Mutex()

    suspend fun ensureEncrypted(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val buildKey = buildTimeKeyProvider().trim()
            if (buildKey.isEmpty()) return@withLock
            val fingerprint = sha256(buildKey)
            val prefs = dataStore.data.first()
            if (prefs[KEY_CIPHERTEXT] != null && prefs[KEY_FINGERPRINT] == fingerprint) return@withLock

            val payload = cipher.encrypt(buildKey.toByteArray(Charsets.UTF_8))
            dataStore.edit {
                it[KEY_IV] = b64(payload.iv)
                it[KEY_CIPHERTEXT] = b64(payload.ciphertext)
                it[KEY_FINGERPRINT] = fingerprint
            }
            Unit
        }
    }

    suspend fun hasApiKey(): Boolean {
        ensureEncrypted()
        return dataStore.data.first()[KEY_CIPHERTEXT] != null
    }

    /** Returns the decrypted key, or null when none is configured. Never log the result. */
    suspend fun decryptApiKey(): String? = withContext(Dispatchers.IO) {
        ensureEncrypted()
        try {
            decryptStored()
        } catch (e: GeneralSecurityException) {
            // Keystore key was lost (e.g. data restored onto a new device, or keys wiped).
            // Discard the unusable ciphertext and re-encrypt from the build-time key once.
            dataStore.edit { it.remove(KEY_IV); it.remove(KEY_CIPHERTEXT); it.remove(KEY_FINGERPRINT) }
            ensureEncrypted()
            decryptStored()
        }
    }

    private suspend fun decryptStored(): String? {
        val prefs = dataStore.data.first()
        val iv = prefs[KEY_IV] ?: return null
        val ct = prefs[KEY_CIPHERTEXT] ?: return null
        val plainBytes = cipher.decrypt(EncryptedPayload(unb64(iv), unb64(ct)))
        return try {
            String(plainBytes, Charsets.UTF_8)
        } finally {
            plainBytes.fill(0) // wipe the byte copy
        }
    }

    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun unb64(value: String): ByteArray = Base64.getDecoder().decode(value)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val KEY_IV = stringPreferencesKey("gemini_key_iv")
        val KEY_CIPHERTEXT = stringPreferencesKey("gemini_key_ciphertext")
        val KEY_FINGERPRINT = stringPreferencesKey("gemini_key_fingerprint")
    }
}
