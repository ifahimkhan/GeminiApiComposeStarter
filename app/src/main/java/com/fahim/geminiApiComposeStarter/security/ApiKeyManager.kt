package com.example.geministarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.geministarter.BuildConfig
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "gemini_api_key_wrapper"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128

private val Context.secureKeyStore: DataStore<Preferences> by preferencesDataStore(
    name = "secure_key_store"
)

/**
 * Handles at-rest encryption of the Gemini API key.
 *
 * Flow, matching the assignment's "Encrypt at Rest" requirement:
 *  1. On first launch, generate (or reuse) an AES-256-GCM key inside the
 *     Android Keystore. The raw key material never leaves the secure
 *     hardware/TEE — we only ever get a [Cipher] handle to it.
 *  2. Read the plaintext key that Gradle baked into [BuildConfig] (itself
 *     sourced from local.properties or an env var — see app/build.gradle.kts)
 *     exactly once, encrypt it, and persist ONLY the resulting ciphertext
 *     (+ IV) in DataStore.
 *  3. On every subsequent app start, decrypt from the stored ciphertext.
 *     The decrypted value is held in memory only for as long as it takes
 *     to construct the [com.google.ai.client.generativeai.GenerativeModel]
 *     — it is never logged, toasted, or displayed.
 */
open class ApiKeyManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val ivKey = stringPreferencesKey("gemini_key_iv")
    private val ciphertextKey = stringPreferencesKey("gemini_key_ciphertext")

    /**
     * Returns the decrypted API key, encrypting-and-persisting it first if
     * this is the first run. Returns an empty string if no key is
     * available from any source (BuildConfig, existing ciphertext).
     */
    open suspend fun getApiKey(): String {
        val prefs = context.secureKeyStore.data.first()
        val storedCiphertext = prefs[ciphertextKey]
        val storedIv = prefs[ivKey]

        if (storedCiphertext != null && storedIv != null) {
            return runCatching { decrypt(storedCiphertext, storedIv) }.getOrDefault("")
        }

        // First run: take the plaintext key from BuildConfig (build-time only,
        // never a string literal in Kotlin/XML) and seal it immediately.
        val plaintextFromBuild = BuildConfig.GEMINI_API_KEY
        if (plaintextFromBuild.isBlank()) return ""

        val (ciphertext, iv) = encrypt(plaintextFromBuild)
        context.secureKeyStore.edit { store ->
            store[ciphertextKey] = ciphertext
            store[ivKey] = iv
        }
        return plaintextFromBuild
    }

    private fun getOrCreateSecretKey(): SecretKey {
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

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

    private fun encrypt(plaintext: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertextBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ciphertextB64 = android.util.Base64.encodeToString(ciphertextBytes, android.util.Base64.NO_WRAP)
        val ivB64 = android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP)
        return ciphertextB64 to ivB64
    }

    private fun decrypt(ciphertextB64: String, ivB64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = android.util.Base64.decode(ivB64, android.util.Base64.NO_WRAP)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        val ciphertextBytes = android.util.Base64.decode(ciphertextB64, android.util.Base64.NO_WRAP)
        return String(cipher.doFinal(ciphertextBytes), Charsets.UTF_8)
    }
}
