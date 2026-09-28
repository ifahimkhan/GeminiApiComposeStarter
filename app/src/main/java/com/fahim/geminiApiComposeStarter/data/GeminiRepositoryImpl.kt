package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

/**
 * Production implementation of [GeminiRepository] backed by the Google Generative AI SDK.
 *
 * Supports passing the API key directly or providing a dynamic provider function
 * to decrypt the key in-memory only when required.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(
        apiKey: String,
        modelName: String = DEFAULT_MODEL,
    ) : this(apiKeyProvider = { apiKey }, modelName = modelName)

    override suspend fun generateText(prompt: String): Result<String> {
        val key = apiKeyProvider().trim()
        if (key.isBlank()) {
            return Result.failure(
                IllegalStateException("GEMINI_API_KEY is missing. Please configure it in local.properties.")
            )
        }

        return try {
            val model = GenerativeModel(modelName = modelName, apiKey = key)
            val response = model.generateContent(prompt)
            val text = response.text?.trim()?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response received from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini text generation failed", e)
            val userFriendlyMessage = when {
                e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                e.message?.contains("API key not valid", ignoreCase = true) == true ->
                    "Invalid Gemini API key. Please check your key in local.properties."
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ||
                e.message?.contains("quota", ignoreCase = true) == true ->
                    "Gemini API rate limit or quota exceeded. Please try again later."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                e.message?.contains("ConnectException", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection and try again."
                else -> e.message ?: "An unexpected error occurred while communicating with Gemini."
            }
            Result.failure(RuntimeException(userFriendlyMessage, e))
        }
    }
}
