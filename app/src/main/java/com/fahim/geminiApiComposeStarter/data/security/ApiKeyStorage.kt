package com.fahim.geminiApiComposeStarter.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Storage contract for persisting and retrieving the encrypted API key.
 * Only the ciphertext and IV are ever stored.
 */
interface ApiKeyStorage {
    fun saveEncryptedKey(encryptedData: EncryptedData)
    fun getEncryptedKey(): EncryptedData?
    fun hasEncryptedKey(): Boolean
    fun clear()
}

/**
 * Storage implementation using [EncryptedSharedPreferences] to store ciphertext and IV.
 * This provides defense-in-depth: the preferences file itself is encrypted at rest using
 * a Keystore-backed MasterKey, and the payload inside is also encrypted with our AES-256-GCM Keystore key.
 */
@Suppress("DEPRECATION")
class EncryptedSharedPrefsStorage(
    context: Context,
    prefsName: String = PREFS_FILE_NAME,
) : ApiKeyStorage {

    companion object {
        const val PREFS_FILE_NAME = "gemini_secure_prefs"
        private const val KEY_CIPHERTEXT = "encrypted_api_key_ciphertext"
        private const val KEY_IV = "encrypted_api_key_iv"
    }

    private val sharedPreferences: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            prefsName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun saveEncryptedKey(encryptedData: EncryptedData) {
        val encodedCiphertext = Base64.encodeToString(encryptedData.ciphertext, Base64.NO_WRAP)
        val encodedIv = Base64.encodeToString(encryptedData.iv, Base64.NO_WRAP)
        sharedPreferences.edit()
            .putString(KEY_CIPHERTEXT, encodedCiphertext)
            .putString(KEY_IV, encodedIv)
            .apply()
    }

    override fun getEncryptedKey(): EncryptedData? {
        val encodedCiphertext = sharedPreferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val encodedIv = sharedPreferences.getString(KEY_IV, null) ?: return null
        val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
        val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
        return EncryptedData(ciphertext = ciphertext, iv = iv)
    }

    override fun hasEncryptedKey(): Boolean {
        return sharedPreferences.contains(KEY_CIPHERTEXT) && sharedPreferences.contains(KEY_IV)
    }

    override fun clear() {
        sharedPreferences.edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .apply()
    }
}
