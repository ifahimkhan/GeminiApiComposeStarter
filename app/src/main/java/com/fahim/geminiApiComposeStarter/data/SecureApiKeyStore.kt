package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.apiKeyDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_key_prefs")

class SecureApiKeyStore(private val context: Context) {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiApiKeyAlias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        
        private val CIPHERTEXT_KEY = stringPreferencesKey("ciphertext")
        private val IV_KEY = stringPreferencesKey("iv")
    }

    private val mutex = Mutex()
    private var inMemoryDecryptedKey: String? = null

    init {
        generateKeyIfNotExists()
    }

    private fun generateKeyIfNotExists() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    suspend fun saveApiKey(apiKey: String) {
        mutex.withLock {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            context.apiKeyDataStore.edit { prefs ->
                prefs[CIPHERTEXT_KEY] = Base64.encodeToString(ciphertext, Base64.DEFAULT)
                prefs[IV_KEY] = Base64.encodeToString(iv, Base64.DEFAULT)
            }
            inMemoryDecryptedKey = apiKey
        }
    }

    suspend fun getApiKey(): String? {
        mutex.withLock {
            if (inMemoryDecryptedKey != null) {
                return inMemoryDecryptedKey
            }

            val prefs = context.apiKeyDataStore.data.first()
            val ciphertextString = prefs[CIPHERTEXT_KEY]
            val ivString = prefs[IV_KEY]

            if (ciphertextString == null || ivString == null) {
                return null
            }

            return try {
                val ciphertext = Base64.decode(ciphertextString, Base64.DEFAULT)
                val iv = Base64.decode(ivString, Base64.DEFAULT)

                val cipher = Cipher.getInstance(TRANSFORMATION)
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
                
                val decrypted = cipher.doFinal(ciphertext)
                val result = String(decrypted, Charsets.UTF_8)
                inMemoryDecryptedKey = result
                result
            } catch (e: Exception) {
                null
            }
        }
    }
    
    suspend fun clear() {
        mutex.withLock {
            context.apiKeyDataStore.edit { prefs ->
                prefs.clear()
            }
            inMemoryDecryptedKey = null
        }
    }
}
