package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiKeyDataStore by preferencesDataStore("api_key_vault")

/** The AES key never leaves Android Keystore. DataStore holds only ciphertext and IV. */
class ApiKeyVault(context: Context) {
    private val store = context.applicationContext.apiKeyDataStore
    private val ciphertextKey = stringPreferencesKey("ciphertext")
    private val ivKey = stringPreferencesKey("iv")
    companion object { private val lock = Mutex() }
    private var initialized = false
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("gemini_api_key_aes", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("gemini_api_key_aes",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    suspend fun initialize(buildKey: String) = lock.withLock {
        if (initialized || buildKey.isBlank()) return@withLock
        // Refresh once per app instance so replacing the build key rotates stored ciphertext.
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(buildKey.toByteArray(Charsets.UTF_8))
        store.edit {
            it[ciphertextKey] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            it[ivKey] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        }
        initialized = true
    }
    suspend fun decrypt(): String = lock.withLock {
        val stored = store.data.first()
        val ciphertext = stored[ciphertextKey] ?: error("API key is not configured")
        val iv = stored[ivKey] ?: error("API key is not configured")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
    }
}
