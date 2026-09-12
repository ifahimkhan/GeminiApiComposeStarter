package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureApiKeyStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveFromBuildConfig(apiKey: String) {
        if (apiKey.isBlank() || prefs.contains(KEY_CIPHERTEXT)) return

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        prefs.edit()
            .putString(KEY_CIPHERTEXT, cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8)).base64())
            .putString(KEY_IV, cipher.iv.base64())
            .apply()
    }

    fun getApiKey(): String {
        val ciphertext = prefs.getString(KEY_CIPHERTEXT, null) ?: return ""
        val iv = prefs.getString(KEY_IV, null) ?: return ""

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv.fromBase64()),
        )
        return cipher.doFinal(ciphertext.fromBase64()).toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val GCM_TAG_LENGTH_BITS = 128
        const val KEY_ALIAS = "gemini_api_key"
        const val KEY_CIPHERTEXT = "gemini_api_key_ciphertext"
        const val KEY_IV = "gemini_api_key_iv"
        const val PREFS_NAME = "secure_api_key_store"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
