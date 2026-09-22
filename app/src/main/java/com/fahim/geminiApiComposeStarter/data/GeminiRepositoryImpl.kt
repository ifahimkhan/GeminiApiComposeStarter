package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.ApiKeyProvider
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: ApiKeyProvider,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Secondary constructor for direct key (e.g. testing)
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = object : ApiKeyProvider {
            override fun getApiKey(): String = apiKey
            override fun hasApiKey(): Boolean = apiKey.isNotBlank()
        },
        modelName = modelName,
    )

    override suspend fun generateText(prompt: String): Result<String> {
        val apiKey = apiKeyProvider.getApiKey()?.takeIf { it.isNotBlank() }
            ?: return Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY to local.properties.")
            )

        return try {
            // GenerativeModel is created strictly in memory with the decrypted key
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
            // Log only error class name to prevent leaking request details or tokens
            Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }
}
