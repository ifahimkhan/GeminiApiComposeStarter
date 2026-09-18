package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.ApiKeyProvider
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: ApiKeyProvider,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    /**
     * Secondary constructor for direct API key string usage.
     */
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = object : ApiKeyProvider {
            override suspend fun getApiKey(): String = apiKey
            override suspend fun saveApiKey(apiKey: String) {}
            override suspend fun hasApiKey(): Boolean = apiKey.isNotBlank()
        },
        modelName = modelName,
    )

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is not available"))
        }

        try {
            // Decrypt in memory only at the moment GenerativeModel is created
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
            val response = model.generateContent(prompt)
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
