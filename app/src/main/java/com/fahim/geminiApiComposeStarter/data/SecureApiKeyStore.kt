package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private val Context.secretDataStore by preferencesDataStore(name = "encrypted_credentials")

class SecureApiKeyStore(context: Context) {
    private val store = context.applicationContext.secretDataStore
    private val ciphertextKey = stringPreferencesKey("gemini_ciphertext")
    private val mutex = Mutex()

    suspend fun initialize(apiKey: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (apiKey.isBlank()) {
                store.edit { it.remove(ciphertextKey) }
                return@withLock
            }
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            val envelope = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
            store.edit { it[ciphertextKey] = envelope }
        }
    }

    suspend fun decryptForModel(): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encrypted = store.data.first()[ciphertextKey]
                ?: throw IllegalStateException("API key is not configured")
            decrypt(encrypted, getOrCreateKey())
        }
    }

    private fun decrypt(envelope: String, key: SecretKey): String {
        val bytes = Base64.decode(envelope, Base64.NO_WRAP)
        require(bytes.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "gemini_api_key_aes256"
    }
}
