package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.credentialStore by preferencesDataStore("credential_vault")
class MissingCredentialException : Exception("Add your Gemini API key to local.properties and rebuild to start chatting.")

class ApiKeyVault(context: Context, private val bootstrap: () -> String, private val version: String,
    private val store: DataStore<Preferences> = context.applicationContext.credentialStore,
    private val alias: String = "gemini_api_key_aes_v1",
) {
    private val mutex = Mutex()
    private val ciphertext = stringPreferencesKey("ciphertext")
    private val iv = stringPreferencesKey("iv")
    private val storedVersion = stringPreferencesKey("version")
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(): SecretKey {
        val existing = keyStore().getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private suspend fun seed() {
        val value = bootstrap().trim()
        if (value.isBlank() || value == "your_api_key_here") throw MissingCredentialException()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val bytes = value.toByteArray(Charsets.UTF_8)
        val encrypted = try { cipher.doFinal(bytes) } finally { bytes.fill(0) }
        store.edit {
            it[ciphertext] = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            it[iv] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            it[storedVersion] = version
        }
    }
    suspend fun initialize() = withContext(Dispatchers.IO) { mutex.withLock {
        val data = store.data.first()
        if (data[ciphertext] == null || data[storedVersion] != version || !keyStore().containsAlias(alias)) seed()
    } }
    suspend fun <T> withCredential(create: (String) -> T): T = withContext(Dispatchers.IO) { mutex.withLock {
        var data = store.data.first()
        if (data[ciphertext] == null || data[storedVersion] != version || !keyStore().containsAlias(alias)) { seed(); data = store.data.first() }
        fun decrypt(): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(data[iv], Base64.NO_WRAP)))
            return cipher.doFinal(Base64.decode(data[ciphertext], Base64.NO_WRAP))
        }
        val plain = try { decrypt() } catch (_: Exception) {
            // A restored/corrupted envelope cannot be used with a missing or invalid device key.
            keyStore().deleteEntry(alias); seed(); data = store.data.first(); decrypt()
        }
        try { create(String(plain, Charsets.UTF_8)) } finally { plain.fill(0) }
    } }
}
