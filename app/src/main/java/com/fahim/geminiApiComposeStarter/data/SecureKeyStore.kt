package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "SecureKeyStore"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "gemini_api_key_alias"
private const val PREFS_NAME = "gemini_secure_prefs"
private const val CIPHERTEXT_KEY_NAME = "gemini_api_key_ciphertext"
private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12

private val Context.secureDataStore by preferencesDataStore(name = PREFS_NAME)

class SecureKeyStore(private val context: Context) {

    private val ciphertextKey = stringPreferencesKey(CIPHERTEXT_KEY_NAME)

    suspend fun getOrCreateKey(fallbackPlaintext: String): String {
        val stored = readCiphertext()
        if (!stored.isNullOrEmpty()) {
            val decrypted = decrypt(stored)
            if (decrypted != null) {
                return decrypted
            }
            Log.w(TAG, "Stored ciphertext could not be decrypted; re-encrypting from BuildConfig.")
            writeCiphertext("")
        }
        val encrypted = encrypt(fallbackPlaintext)
        if (encrypted == null) {
            Log.e(TAG, "Encryption failed; falling back to plaintext for this session only.")
            return fallbackPlaintext
        }
        writeCiphertext(encrypted)
        return fallbackPlaintext
    }

    private suspend fun readCiphertext(): String? =
        context.secureDataStore.data.first()[ciphertextKey]

    private suspend fun writeCiphertext(value: String) {
        context.secureDataStore.edit { prefs ->
            if (value.isEmpty()) prefs.remove(ciphertextKey)
            else prefs[ciphertextKey] = value
        }
    }

    private fun encrypt(plaintext: String): String? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            val iv = cipher.iv
            val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + ct
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (t: Throwable) {
            Log.e(TAG, "encrypt failed", t)
            null
        }
    }

    private fun decrypt(encoded: String): String? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_BYTES) {
                null
            } else {
                val iv = combined.copyOfRange(0, GCM_IV_BYTES)
                val ct = combined.copyOfRange(GCM_IV_BYTES, combined.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    getOrCreateSecretKey(),
                    GCMParameterSpec(GCM_TAG_BITS, iv),
                )
                String(cipher.doFinal(ct), Charsets.UTF_8)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "decrypt failed", t)
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}