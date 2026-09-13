package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.keyDataStore by preferencesDataStore(name = "secure_key_store")

class SecureKeyStore(private val context: Context) {

    private val androidKeyStoreAlias = "gemini_chat_master_key"
    private val transformation = "AES/GCM/NoPadding"
    private val tagLength = 128
    private val ivKey = stringPreferencesKey("gemini_api_key_iv")
    private val cipherTextKey = stringPreferencesKey("gemini_api_key_ciphertext")
    private val isCustomKeyFlag = booleanPreferencesKey("gemini_api_key_is_custom")

    val isCustomKey: Flow<Boolean> = context.keyDataStore.data.map { it[isCustomKeyFlag] ?: false }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val existing = keyStore.getKey(androidKeyStoreAlias, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        val spec = KeyGenParameterSpec.Builder(
            androidKeyStoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private suspend fun encryptAndStore(plainTextKey: String, isCustom: Boolean) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val cipherBytes = cipher.doFinal(plainTextKey.toByteArray(Charsets.UTF_8))
        val ivEncoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val cipherEncoded = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        context.keyDataStore.edit { prefs ->
            prefs[ivKey] = ivEncoded
            prefs[cipherTextKey] = cipherEncoded
            prefs[isCustomKeyFlag] = isCustom
        }
    }

    suspend fun persistDefaultKeyIfEmpty(defaultKey: String) {
        val prefs = context.keyDataStore.data.first()
        if (prefs[cipherTextKey] == null && defaultKey.isNotBlank()) {
            encryptAndStore(defaultKey, isCustom = false)
        }
    }

    suspend fun saveCustomKey(key: String) {
        encryptAndStore(key, isCustom = true)
    }

    suspend fun resetToDefaultKey(defaultKey: String) {
        encryptAndStore(defaultKey, isCustom = false)
    }

    suspend fun getDecryptedApiKey(fallback: String): String {
        val prefs = context.keyDataStore.data.first()
        val ivEncoded = prefs[ivKey] ?: return fallback
        val cipherEncoded = prefs[cipherTextKey] ?: return fallback
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(transformation)
            val spec = GCMParameterSpec(tagLength, Base64.decode(ivEncoded, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(Base64.decode(cipherEncoded, Base64.NO_WRAP))
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun getMaskedPreview(fallback: String): String {
        val key = getDecryptedApiKey(fallback)
        if (key.length < 8) return "No key configured"
        return "${key.take(4)}••••••${key.takeLast(4)}"
    }
}
