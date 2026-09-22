package com.fahim.geminiApiComposeStarter.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "gemini_api_key_aes"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_BITS = 128

private val CIPHERTEXT = stringPreferencesKey("gemini_api_key_ciphertext")
private val IV = stringPreferencesKey("gemini_api_key_iv")

/**
 * Encrypts the Gemini API key at rest.
 *
 * On first launch the build-time key (from `local.properties` or the `GEMINI_API_KEY` environment
 * variable) is encrypted with an AES-256-GCM key that is generated inside the Android Keystore and
 * can never be exported from it. Only the ciphertext and IV are persisted, in DataStore. Every
 * later launch decrypts in memory, at the moment the `GenerativeModel` is built.
 *
 * This raises the bar; it does not make the key unextractable from a rooted or instrumented
 * device. See the README for how a production app would proxy Gemini calls through a backend.
 */
class ApiKeyStore(private val dataStore: DataStore<Preferences>) {

    /**
     * Returns the plaintext key, seeding the encrypted store from [buildConfigKey] the first time.
     * Returns an empty string when no key was configured at build time.
     */
    suspend fun apiKey(buildConfigKey: String): String = withContext(Dispatchers.IO) {
        decryptStored()?.let { return@withContext it }
        val seed = buildConfigKey.trim()
        if (seed.isEmpty()) return@withContext ""
        persistEncrypted(seed)
        seed
    }

    private suspend fun decryptStored(): String? {
        val prefs = dataStore.data.first()
        val ciphertext = prefs[CIPHERTEXT] ?: return null
        val iv = prefs[IV] ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(GCM_TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)),
            )
            String(cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    private suspend fun persistEncrypted(plaintext: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        dataStore.edit { prefs ->
            prefs[CIPHERTEXT] = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            prefs[IV] = Base64.encodeToString(iv, Base64.NO_WRAP)
        }
    }

    /** Fetches the Keystore-held AES key, generating it on first use. */
    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
