package com.fahim.geminiApiComposeStarter.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.BuildConfig

/**
 * Stores the Gemini API key encrypted at rest using AES-256-GCM inside the Android Keystore.
 *
 * Flow:
 *  1. On first call, generate/retrieve a MasterKey backed by the Android Keystore.
 *  2. Open EncryptedSharedPreferences (AES-256-GCM for values, AES-256-SIV for keys).
 *  3. If the key is not yet stored, encrypt and persist the value from BuildConfig.
 *  4. On every subsequent call, decrypt in memory only — never log or display the result.
 *
 * Production note (see README): client-side encryption raises the bar but cannot fully hide a key
 * from a determined attacker. A production app should move Gemini calls behind a server-side proxy
 * or use Firebase App Check with restricted API keys.
 */
object KeystoreEncryption {

    private const val PREF_FILE_NAME = "gemini_secure_prefs"
    private const val KEY_API_KEY   = "encrypted_gemini_api_key"
    private const val KEYSTORE_ALIAS = "GeminiApiKeyMasterKey"

    /**
     * Returns the decrypted Gemini API key. The plaintext exists only in this stack frame.
     * Never assign the return value to a field or log it.
     */
    fun getDecryptedApiKey(context: Context): String {
        val masterKey = buildMasterKey(context)
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        // Persist ciphertext on first launch (BuildConfig value comes from local.properties via Gradle)
        if (!prefs.contains(KEY_API_KEY)) {
            prefs.edit().putString(KEY_API_KEY, BuildConfig.GEMINI_API_KEY).apply()
        }

        // Decrypt — return empty string if key was never set so the app shows a clear error
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    private fun buildMasterKey(context: Context): MasterKey {
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return MasterKey.Builder(context, KEYSTORE_ALIAS)
            .setKeyGenParameterSpec(spec)
            .build()
    }
}
