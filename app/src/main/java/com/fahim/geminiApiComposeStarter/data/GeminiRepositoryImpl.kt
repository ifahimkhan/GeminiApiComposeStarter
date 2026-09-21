package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Secondary constructor for static key backwards compatibility
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        modelName = modelName
    )

    private fun getModel(): GenerativeModel {
        val key = apiKeyProvider()
        return GenerativeModel(modelName = modelName, apiKey = key)
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val key = apiKeyProvider()
        if (key.isBlank()) {
            Result.failure(IllegalStateException("Gemini API key is not configured."))
        } else {
            val model = getModel()
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Received empty response from Gemini."))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val sanitizedMessage = sanitizeErrorMessage(e)
        Result.failure(Exception(sanitizedMessage))
    }

    private fun sanitizeErrorMessage(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("API_KEY_INVALID", ignoreCase = true) || msg.contains("403", ignoreCase = true) ->
                "Invalid Gemini API key. Please verify your configuration."
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("429", ignoreCase = true) ->
                "Quota exceeded. Please check your Gemini rate limits and try again later."
            msg.contains("Unable to resolve host", ignoreCase = true) || msg.contains("timeout", ignoreCase = true) ->
                "Network error. Please check your internet connection and try again."
            else -> "Failed to generate response from Gemini. Please try again."
        }
    }
}

