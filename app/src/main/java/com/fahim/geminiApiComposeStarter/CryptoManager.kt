package com.fahim.geminiApiComposeStarter

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager(context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "gemini_secure_key"
    private val prefs = context.getSharedPreferences("secure_gemini_prefs", Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val existingKey = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveEncryptedKey(rawKey: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(rawKey.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString("iv", Base64.encodeToString(iv, Base64.DEFAULT))
            .putString("ciphertext", Base64.encodeToString(ciphertext, Base64.DEFAULT))
            .apply()
    }

    fun getDecryptedKey(): String {
        val ivBase64 = prefs.getString("iv", null)
        val ciphertextBase64 = prefs.getString("ciphertext", null)

        if (ivBase64.isNullOrEmpty() || ciphertextBase64.isNullOrEmpty()) return ""

        val iv = Base64.decode(ivBase64, Base64.DEFAULT)
        val ciphertext = Base64.decode(ciphertextBase64, Base64.DEFAULT)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun hasEncryptedKey(): Boolean {
        return prefs.contains("ciphertext") && prefs.contains("iv")
    }
}