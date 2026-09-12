package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Protects the Gemini API key at rest.
 *
 * 1. AES-256 key is generated inside Android Keystore.
 * 2. Gemini API key is encrypted with AES/GCM/NoPadding.
 * 3. Only ciphertext + IV are persisted.
 * 4. The persisted values are themselves kept in EncryptedSharedPreferences.
 *
 * No decrypted API key is logged, toasted, displayed, or written to disk.
 */
class ApiKeyVault(
    context: Context,
) {

    private val appContext = context.applicationContext

    private val encryptedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            PREFERENCE_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /**
     * Called at app start with BuildConfig.GEMINI_API_KEY.
     * If a key is already securely stored, it is left untouched.
     */
    fun provision(buildConfigKey: String) {
        val cleanKey = buildConfigKey.trim()

        if (cleanKey.isBlank() || hasApiKey()) {
            return
        }

        runCatching {
            encryptAndStore(cleanKey)
        }
    }

    fun hasApiKey(): Boolean {
        return encryptedPreferences.contains(KEY_CIPHERTEXT) &&
            encryptedPreferences.contains(KEY_IV)
    }

    fun readApiKey(): String? {
        val ciphertextText =
            encryptedPreferences.getString(KEY_CIPHERTEXT, null)
                ?: return null

        val ivText =
            encryptedPreferences.getString(KEY_IV, null)
                ?: return null

        return runCatching {
            val key = getExistingSecretKey() ?: return null

            val ciphertext = Base64.decode(
                ciphertextText,
                Base64.NO_WRAP,
            )

            val iv = Base64.decode(
                ivText,
                Base64.NO_WRAP,
            )

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(128, iv),
            )

            val plaintext = cipher.doFinal(ciphertext)

            String(
                plaintext,
                StandardCharsets.UTF_8,
            )
        }.getOrNull()
    }

    private fun encryptAndStore(apiKey: String) {
        val secretKey = getOrCreateSecretKey()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
        )

        val ciphertext = cipher.doFinal(
            apiKey.toByteArray(StandardCharsets.UTF_8)
        )

        val iv = cipher.iv

        encryptedPreferences
            .edit()
            .putString(
                KEY_CIPHERTEXT,
                Base64.encodeToString(
                    ciphertext,
                    Base64.NO_WRAP,
                ),
            )
            .putString(
                KEY_IV,
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP,
                ),
            )
            .apply()
    }

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        return keyStore.getKey(
            KEY_ALIAS,
            null,
        ) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        getExistingSecretKey()?.let {
            return it
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )

        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(
                KeyProperties.BLOCK_MODE_GCM,
            )
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE,
            )
            .setKeySize(256)
            .build()

        keyGenerator.init(specification)

        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "gemini_api_aes_v2"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val PREFERENCE_FILE = "gemini_api_vault_v2"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_IV = "iv"
    }
}
