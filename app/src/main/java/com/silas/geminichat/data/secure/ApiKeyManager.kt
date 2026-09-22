package com.silas.geminichat.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private const val KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "silas_gemini_api_key"
private const val PREFERENCES_NAME = "secure_storage"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128

private val Context.secureDataStore by preferencesDataStore(name = PREFERENCES_NAME)

/**
 * Encrypts the Gemini API key with an AES-256-GCM key held inside the Android
 * Keystore. Only the ciphertext (never the plaintext key) is persisted, in a
 * Preferences DataStore. The plaintext is decrypted in memory only when the
 * repository needs to build a GenerativeModel.
 *
 * @author Silas · n091 (MBA Tech CE, E1)
 */
class ApiKeyManager(private val context: Context) {

    private val cipherTextKey = stringPreferencesKey("encrypted_api_key")
    private val ivKey = stringPreferencesKey("encrypted_api_key_iv")

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Returns the decrypted API key (held in memory only). On first use it
     * encrypts [rawApiKey] with the Keystore key and persists only the
     * ciphertext, so the plaintext never touches durable storage.
     */
    suspend fun decryptApiKey(rawApiKey: String?): String? {
        if (rawApiKey.isNullOrBlank()) return null
        val prefs = context.secureDataStore.data.first()
        val storedCipher = prefs[cipherTextKey]
        val storedIv = prefs[ivKey]

        if (!storedCipher.isNullOrBlank() && !storedIv.isNullOrBlank()) {
            return decrypt(storedCipher, storedIv)
        }

        // First launch: encrypt the build-time key and persist only the ciphertext.
        val (cipherText, iv) = encrypt(rawApiKey)
        context.secureDataStore.edit { prefs ->
            prefs[cipherTextKey] = cipherText
            prefs[ivKey] = iv
        }
        return rawApiKey
    }

    private fun encrypt(plainText: String): Pair<String, String> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(cipherText, Base64.NO_WRAP) to
            Base64.encodeToString(iv, Base64.NO_WRAP)
    }

    private fun decrypt(cipherText: String, iv: String): String? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        val plain = cipher.doFinal(Base64.decode(cipherText, Base64.NO_WRAP))
        String(plain, Charsets.UTF_8)
    }.getOrNull()
}
