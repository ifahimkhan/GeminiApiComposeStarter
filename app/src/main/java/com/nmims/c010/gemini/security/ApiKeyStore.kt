package com.nmims.c010.gemini.security

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nmims.c010.gemini.BuildConfig
import kotlinx.coroutines.flow.first

private val Context.secureStore by preferencesDataStore("secure_prefs")

/**
 * On first launch the BuildConfig value is encrypted and only the ciphertext
 * is persisted. Afterwards the key is decrypted in memory, at the single
 * moment the GenerativeModel is constructed. It is never logged or shown.
 */
class ApiKeyStore(private val context: Context) {

    private val cipherKey = stringPreferencesKey("gemini_key_ct")
    private val ivKey = stringPreferencesKey("gemini_key_iv")

    suspend fun apiKey(): String {
        val prefs = context.secureStore.data.first()
        val ct = prefs[cipherKey]
        val iv = prefs[ivKey]

        if (ct != null && iv != null) {
            return CryptoManager.decrypt(
                EncryptedPayload(
                    iv = Base64.decode(iv, Base64.NO_WRAP),
                    cipherText = Base64.decode(ct, Base64.NO_WRAP)
                )
            )
        }

        // First launch: take the build-time value, encrypt, persist ciphertext.
        val raw = BuildConfig.GEMINI_API_KEY
        check(raw.isNotBlank()) { "GEMINI_API_KEY missing - see local.properties.example" }
        val payload = CryptoManager.encrypt(raw)
        context.secureStore.edit {
            it[cipherKey] = Base64.encodeToString(payload.cipherText, Base64.NO_WRAP)
            it[ivKey] = Base64.encodeToString(payload.iv, Base64.NO_WRAP)
        }
        return raw
    }
}
