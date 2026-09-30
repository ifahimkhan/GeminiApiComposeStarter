package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_store")

/**
 * Persists ONLY the ciphertext of the Gemini key. The plaintext exists in memory just long
 * enough to build the GenerativeModel. Never log, toast or display the decrypted value.
 */
class SecureApiKeyStore(
    private val context: Context,
    private val buildConfigKey: String,
) {
    private val cipherTextKey = stringPreferencesKey("gemini_key_ct")
    private val ivKey = stringPreferencesKey("gemini_key_iv")

    /** Called on first launch: encrypts the build-time key and stores the ciphertext. */
    suspend fun ensureEncrypted() = withContext(Dispatchers.IO) {
        val prefs = context.secureDataStore.data.first()
        if (prefs[cipherTextKey] == null && buildConfigKey.isNotBlank()) {
            val encrypted = KeystoreCrypto.encrypt(buildConfigKey)
            context.secureDataStore.edit {
                it[cipherTextKey] = encrypted.cipherText.toBase64()
                it[ivKey] = encrypted.iv.toBase64()
            }
        }
    }

    /** Decrypts in memory. Returns "" if nothing is stored. */
    suspend fun decryptApiKey(): String = withContext(Dispatchers.IO) {
        ensureEncrypted()
        val prefs = context.secureDataStore.data.first()
        val ct = prefs[cipherTextKey] ?: return@withContext ""
        val iv = prefs[ivKey] ?: return@withContext ""
        try {
            KeystoreCrypto.decrypt(KeystoreCrypto.EncryptedData(iv.fromBase64(), ct.fromBase64()))
        } catch (e: Exception) {
            // e.g. data restored from a backup on a new device (Keystore key is gone): re-encrypt.
            context.secureDataStore.edit { it.clear() }
            ensureEncrypted()
            buildConfigKey
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
