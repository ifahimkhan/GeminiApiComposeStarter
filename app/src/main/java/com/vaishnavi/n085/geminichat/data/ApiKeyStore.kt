package com.vaishnavi.n085.geminichat.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.vaishnavi.n085.geminichat.data.prefs.AppPreferences
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

/**
 * Encrypts the Gemini API key at rest with an AES-256-GCM key held in the Android
 * Keystore. Only the ciphertext + IV are persisted (in DataStore); the plaintext
 * is decrypted in memory only at the moment the GenerativeModel is created, and
 * is never logged, displayed or stored.
 */
class ApiKeyStore(private val preferences: AppPreferences) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /**
     * Returns the working API key. On first launch the build-config key is encrypted
     * and stored; on later launches the stored ciphertext is decrypted.
     */
    suspend fun getOrStoreApiKey(buildConfigKey: String): String {
        val stored = preferences.apiKeyCipher.first()
        if (stored != null) {
            return try {
                decrypt(stored.iv, stored.ciphertext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt stored API key; re-encrypting", e)
                if (buildConfigKey.isBlank()) "" else encryptAndStore(buildConfigKey)
            }
        }
        if (buildConfigKey.isBlank()) return ""
        return encryptAndStore(buildConfigKey)
    }

    private suspend fun encryptAndStore(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        preferences.saveApiKeyCipher(
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
        )
        return plaintext
    }

    private fun decrypt(ivBase64: String, ciphertextBase64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plaintext = cipher.doFinal(Base64.decode(ciphertextBase64, Base64.NO_WRAP))
        return plaintext.toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "vaishnavi_gemini_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val TAG = "ApiKeyStore"
    }
}
