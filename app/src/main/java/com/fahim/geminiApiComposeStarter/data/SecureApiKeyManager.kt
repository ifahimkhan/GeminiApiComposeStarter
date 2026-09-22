package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.util.Log
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException

/** Keeps only AES-GCM ciphertext in DataStore; the Keystore key never leaves Android Keystore. */
class SecureApiKeyManager(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = PreferenceDataStoreFactory.create {
        appContext.preferencesDataStoreFile("secure_api_key.preferences_pb")
    }

    suspend fun getOrInitializeKey(sourceKey: String): String? {
        return try {
        val stored = dataStore.data.first()
        val cipherText = stored[CIPHER_TEXT]
        val iv = stored[IV]
        if (!cipherText.isNullOrBlank() && !iv.isNullOrBlank()) return decrypt(cipherText, iv)
        if (sourceKey.isBlank()) {
            Log.w(TAG, "Gemini key source is not configured")
            return null
        }
        val encrypted = encrypt(sourceKey)
        dataStore.edit { prefs ->
            prefs[CIPHER_TEXT] = encrypted.first
            prefs[IV] = encrypted.second
        }
        sourceKey
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Log.e(TAG, "Secure API-key operation failed: ${exception.javaClass.simpleName}")
        null
    }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    private fun encrypt(value: String): Pair<String, String> {
        // Android Keystore requires its provider to generate the randomized GCM nonce.
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP) to
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String, iv: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), javax.crypto.spec.GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        }
        return cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gemini_api_key_aes_gcm"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        val CIPHER_TEXT = stringPreferencesKey("ciphertext")
        val IV = stringPreferencesKey("iv")
        const val TAG = "SecureApiKeyManager"
    }
}
