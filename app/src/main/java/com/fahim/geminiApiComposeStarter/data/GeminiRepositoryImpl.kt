package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private const val SECURE_PREFS_NAME = "secure_gemini_prefs"
private const val KEY_ENCRYPTED_API_KEY = "encrypted_gemini_api_key"

class GeminiRepositoryImpl(
    context: Context,
    apiKey: String = BuildConfig.GEMINI_API_KEY,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val generativeModel: GenerativeModel

    init {
        val decryptedKey = getOrSaveEncryptedApiKey(context, apiKey)
        generativeModel = GenerativeModel(modelName = modelName, apiKey = decryptedKey)
    }

    private fun getOrSaveEncryptedApiKey(context: Context, key: String): String {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            if (key.isNotBlank()) {
                prefs.edit { putString(KEY_ENCRYPTED_API_KEY, key) }
            }

            prefs.getString(KEY_ENCRYPTED_API_KEY, "") ?: key
        } catch (e: Exception) {
            Log.d(TAG, "Keystore unavailable, fallback to in-memory key", e)
            key
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }
}
