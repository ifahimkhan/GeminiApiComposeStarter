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
    private fun ciphertextKey(provider: AiProvider) = stringPreferencesKey(
        if (provider == AiProvider.GEMINI) "ciphertext" else "${provider.name.lowercase()}_ciphertext"
    )
    private fun ivKey(provider: AiProvider) = stringPreferencesKey(
        if (provider == AiProvider.GEMINI) "iv" else "${provider.name.lowercase()}_iv"
    )

    val availableProviders: Flow<Set<AiProvider>> = credentialStore.data.map { preferences ->
        AiProvider.entries.filter { preferences[ciphertextKey(it)] != null }.toSet()
    }

    suspend fun storeOnFirstLaunch(provider: AiProvider, apiKey: String) {
        if (apiKey.isBlank() || provider in availableProviders.first()) return
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(provider))
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))
        credentialStore.edit {
            if (it[ciphertextKey(provider)] == null) {
                it[ciphertextKey(provider)] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
                it[ivKey(provider)] = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            }
        }
    }

    suspend fun readForModel(provider: AiProvider): String? {
        val preferences = credentialStore.data.first()
        val encoded = preferences[ciphertextKey(provider)] ?: return null
        val iv = preferences[ivKey(provider)] ?: return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(provider), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    private fun key(provider: AiProvider): SecretKey {
        val alias = if (provider == AiProvider.GEMINI) "gemini_api_key" else "${provider.name.lowercase()}_api_key"
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }
}
