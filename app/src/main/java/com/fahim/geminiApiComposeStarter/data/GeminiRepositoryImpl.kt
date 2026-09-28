package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Convenience constructor for fixed string key (used in tests or direct instantiation)
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        modelName = modelName,
    )

    override suspend fun generateText(prompt: String): Result<String> {
        val resolvedKey = apiKeyProvider()
        if (resolvedKey.isBlank()) {
            return Result.failure(
                IllegalStateException("Gemini API key is not configured. Please supply a valid API key.")
            )
        }

        return try {
            // Decrypted in memory only at the moment GenerativeModel is instantiated
            val model = GenerativeModel(
                modelName = modelName,
                apiKey = resolvedKey,
            )
            val response = model.generateContent(prompt)
            val text = response.text?.trim()
            if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Received empty response from Gemini API."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val rawMessage = e.message.orEmpty()
            val sanitizedMessage = when {
                rawMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
                        rawMessage.contains("invalid api key", ignoreCase = true) ->
                    "Invalid Gemini API key. Please check your credentials."
                rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        rawMessage.contains("quota", ignoreCase = true) ||
                        rawMessage.contains("429", ignoreCase = true) ->
                    "Gemini API rate limit or quota exceeded. Please wait a moment and try again."
                rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
                        rawMessage.contains("timeout", ignoreCase = true) ||
                        rawMessage.contains("ConnectException", ignoreCase = true) ->
                    "Network error. Please check your internet connection."
                else ->
                    "Gemini service error: ${e.localizedMessage ?: "Unable to complete request"}"
            }
            Log.e(TAG, "Gemini generation failed: $sanitizedMessage")
            Result.failure(Exception(sanitizedMessage))
        }
    }
}
