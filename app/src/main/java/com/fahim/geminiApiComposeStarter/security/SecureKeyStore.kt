package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fahim.geminiApiComposeStarter.BuildConfig
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_key_store")
private const val TAG = "SecureKeyStore"

class SecureKeyStore(private val context: Context) {

    private val cipherTextKey = stringPreferencesKey("gemini_api_key_ciphertext")
    private val ivKey = stringPreferencesKey("gemini_api_key_iv")

    /**
     * Decrypts and returns the Gemini API key in memory only when requested.
     * If stored key is stale/invalid, clears it and re-encrypts from BuildConfig.
     * Never caches or logs the plaintext value.
     */
    suspend fun getApiKey(): String {
        val prefs = context.dataStore.data.first()
        val cipherText = prefs[cipherTextKey]
        val iv = prefs[ivKey]

        if (!cipherText.isNullOrEmpty() && !iv.isNullOrEmpty()) {
            try {
                val decrypted = decrypt(cipherText, iv)
                if (decrypted.isNotBlank()) {
                    return decrypted
                }
            } catch (e: Exception) {
                Log.w(TAG, "Decryption failed (${e.javaClass.simpleName}), clearing stale credentials")
                clearStoredKey()
            }
        }

        val buildConfigKey = BuildConfig.GEMINI_API_KEY.trim()
        if (buildConfigKey.isNotBlank()) {
            try {
                val (encryptedText, encryptedIv) = encrypt(buildConfigKey)
                context.dataStore.edit { preferences ->
                    preferences[cipherTextKey] = encryptedText
                    preferences[ivKey] = encryptedIv
                }
                return buildConfigKey
            } catch (e: Exception) {
                Log.e(TAG, "Encryption failed: ${e.javaClass.simpleName} - ${e.message}")
                return buildConfigKey
            }
        }

        return ""
    }

    suspend fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }

    private suspend fun clearStoredKey() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(cipherTextKey)
                preferences.remove(ivKey)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear stored key: ${e.javaClass.simpleName}")
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): Pair<String, String> {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBytes = cipher.iv
        val cipherTextBase64 = Base64.encodeToString(cipherTextBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        return Pair(cipherTextBase64, ivBase64)
    }

    private fun decrypt(cipherTextBase64: String, ivBase64: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipherTextBytes = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, ivBytes)
        val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val decryptedBytes = cipher.doFinal(cipherTextBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    companion object {
        private const val KEY_ALIAS = "gemini_key_alias"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
    }
}
