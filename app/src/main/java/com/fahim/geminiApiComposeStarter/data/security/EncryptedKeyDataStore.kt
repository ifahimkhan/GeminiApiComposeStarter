package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.encryptedKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_gemini_key_prefs")

class EncryptedKeyDataStore(
    private val context: Context,
    private val secureKeyStore: SecureKeyStore = SecureKeyStore(),
) {
    private val ivKey = stringPreferencesKey("gemini_iv")
    private val ciphertextKey = stringPreferencesKey("gemini_ciphertext")

    suspend fun saveApiKey(plainKey: String) {
        if (plainKey.isBlank()) return
        val encrypted = secureKeyStore.encrypt(plainKey)
        val ivBase64 = Base64.encodeToString(encrypted.iv, Base64.NO_WRAP)
        val ciphertextBase64 = Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP)
        context.encryptedKeyDataStore.edit { prefs ->
            prefs[ivKey] = ivBase64
            prefs[ciphertextKey] = ciphertextBase64
        }
    }

    suspend fun hasStoredKey(): Boolean {
        val prefs = context.encryptedKeyDataStore.data.first()
        return !prefs[ivKey].isNullOrBlank() && !prefs[ciphertextKey].isNullOrBlank()
    }

    suspend fun getDecryptedKey(): String? {
        val prefs = context.encryptedKeyDataStore.data.first()
        val ivBase64 = prefs[ivKey] ?: return null
        val ciphertextBase64 = prefs[ciphertextKey] ?: return null
        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            secureKeyStore.decrypt(iv, ciphertext)
        } catch (e: Exception) {
            null
        }
    }
}
