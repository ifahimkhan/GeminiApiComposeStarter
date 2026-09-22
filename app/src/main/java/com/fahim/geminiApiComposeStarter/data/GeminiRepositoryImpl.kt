package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.ApiKeyProvider
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"

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

        // Attempt generation with configured model
        val primaryResult = invokeModel(modelName, apiKey, prompt)
        if (primaryResult.isSuccess) {
            return primaryResult
        }

        // If primary model failed due to high demand / 503, try fallback model
        if (modelName != FALLBACK_MODEL) {
            val fallbackResult = invokeModel(FALLBACK_MODEL, apiKey, prompt)
            if (fallbackResult.isSuccess) {
                return fallbackResult
            }
        }

        return primaryResult
    }

    private suspend fun invokeModel(model: String, apiKey: String, prompt: String): Result<String> = try {
        val generativeModel = GenerativeModel(modelName = model, apiKey = apiKey)
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
        Log.e(TAG, "generateContent failed on $model: ${e.javaClass.simpleName}")
        val friendlyMessage = when {
            e.message?.contains("503") == true || e.message?.contains("high demand") == true ->
                "Gemini is currently experiencing high demand. Please try again shortly."
            e.message?.contains("API key not valid") == true ->
                "API key not valid. Please pass a valid API key."
            else -> e.message ?: "Failed to generate response"
        }
        Result.failure(IllegalStateException(friendlyMessage, e))
    }
}
