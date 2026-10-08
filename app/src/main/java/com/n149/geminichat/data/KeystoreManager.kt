package com.n149.geminichat.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AES-256-GCM encryption of the Gemini API key using Android Keystore.
 *
 * Security model:
 * - The AES-256-GCM key lives in the Android Keystore and never leaves the hardware-backed
 *   secure enclave (on supported devices).
 * - Only the ciphertext + IV are stored in SharedPreferences.
 * - Decryption happens in-memory at the moment the GenerativeModel is created.
 * - The plaintext is never logged, toasted, or written to disk.
 *
 * Production note (see README):
 * Client-side encryption raises the bar but cannot fully hide a key from a determined
 * attacker on a rooted device. A production app should route Gemini calls through a
 * backend proxy or use Firebase App Check with restricted API keys.
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_ALIAS = "gemini_chat_key_n149"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_ENCRYPTED_API = "enc_api_key"
        private const val KEY_IV = "enc_iv"
        private const val TAG = "KeystoreManager"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Generate or retrieve the AES-256-GCM key from Android Keystore. */
    private fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            keyGen.generateKey()
        }
    }

    /**
     * Encrypts [plaintext] and persists ciphertext + IV to SharedPreferences.
     * Call once on first launch with the value from BuildConfig.GEMINI_API_KEY.
     */
    fun encryptAndStore(plaintext: String) {
        if (plaintext.isBlank()) return
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ENCRYPTED_API, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Decrypts and returns the API key in memory.
     * Returns null if no encrypted value is stored yet (first launch before encryption).
     * NEVER log the returned value.
     */
    fun decryptApiKey(): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val encB64 = prefs.getString(KEY_ENCRYPTED_API, null) ?: return null
            val ivB64 = prefs.getString(KEY_IV, null) ?: return null

            val ciphertext = Base64.decode(encB64, Base64.NO_WRAP)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            null
        }
    }

    /** True if an encrypted API key is already stored. */
    fun hasStoredKey(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_ENCRYPTED_API)
    }
}
