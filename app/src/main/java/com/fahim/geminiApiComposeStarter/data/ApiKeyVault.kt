package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.credentialsDataStore by preferencesDataStore(name = "gemini_credentials")

class ApiKeyVault(
    context: Context,
    private val credentialStore: DataStore<Preferences> = context.credentialsDataStore
) {
    private val ciphertextKey = stringPreferencesKey("ciphertext")
    private val ivKey = stringPreferencesKey("iv")

    val hasKey: Flow<Boolean> = credentialStore.data.map { it[ciphertextKey] != null }

    suspend fun storeOnFirstLaunch(apiKey: String) {
        if (apiKey.isBlank() || hasKey.first()) return
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        credentialStore.edit {
            if (it[ciphertextKey] == null) {
                it[ciphertextKey] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
                it[ivKey] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            }
        }
    }

    suspend fun readForModel(): String? {
        val preferences = credentialStore.data.first()
        val encoded = preferences[ciphertextKey] ?: return null
        val iv = preferences[ivKey] ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey("gemini_api_key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    "gemini_api_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }
}
