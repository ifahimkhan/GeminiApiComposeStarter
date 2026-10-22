package com.example.geminiapicomposestarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

val Context.dataStore by preferencesDataStore(name = "secure_gemini_prefs")

class KeyStoreManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiMasterKey_AES256"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        private val ENCRYPTED_KEY_PREF = stringPreferencesKey("encrypted_api_key")
        private val IV_PREF = stringPreferencesKey("encryption_iv")
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        createKeyIfNotExists()
    }

    private fun createKeyIfNotExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(iv, Base64.NO_WRAP),
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
        )
    }

    fun decrypt(ivBase64: String, cipherTextBase64: String): String {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val cipherText = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    suspend fun saveEncryptedApiKey(rawKey: String) {
        val (iv, cipherText) = encrypt(rawKey)
        context.dataStore.edit { prefs ->
            prefs[IV_PREF] = iv
            prefs[ENCRYPTED_KEY_PREF] = cipherText
        }
    }

    suspend fun getDecryptedApiKey(): String? {
        val prefs = context.dataStore.data.first()
        val iv = prefs[IV_PREF] ?: return null
        val cipherText = prefs[ENCRYPTED_KEY_PREF] ?: return null
        return try {
            decrypt(iv, cipherText)
        } catch (_: Exception) {
            null
        }
    }
}
