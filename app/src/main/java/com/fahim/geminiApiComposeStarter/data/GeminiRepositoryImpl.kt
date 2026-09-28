package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
const val DEFAULT_GEMINI_MODEL = "gemini-1.5-flash"

/**
 * Production implementation of [GeminiRepository] communicating with the Google Generative AI SDK.
 * Decrypted API key is received in-memory at initialization and used strictly for GenerativeModel.
 */
class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_GEMINI_MODEL,
) : GeminiRepository {

    private val model: GenerativeModel? = if (apiKey.isNotBlank()) {
        GenerativeModel(modelName = modelName, apiKey = apiKey)
    } else {
        null
    }

    override suspend fun generateText(prompt: String): Result<String> {
        if (model == null) {
            return Result.failure(
                IllegalStateException("Gemini API key is not configured. Add GEMINI_API_KEY to local.properties.")
            )
        }

        return try {
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("Received empty response from Gemini."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
            val friendlyError = mapToUserFriendlyError(e)
            Result.failure(friendlyError)
        }
    }

    private fun mapToUserFriendlyError(error: Exception): Exception {
        val msg = error.message.orEmpty()
        return when {
            msg.contains("API_KEY_INVALID", ignoreCase = true) ||
            msg.contains("API key not valid", ignoreCase = true) ->
                IllegalStateException("Invalid Gemini API key. Please verify your key in Google AI Studio.")

            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            msg.contains("quota", ignoreCase = true) ||
            msg.contains("429") ->
                IllegalStateException("Gemini API rate limit or quota exceeded. Please wait a moment and try again.")

            msg.contains("Unable to resolve host", ignoreCase = true) ||
            msg.contains("Failed to connect", ignoreCase = true) ||
            msg.contains("SocketTimeoutException", ignoreCase = true) ->
                IllegalStateException("Network connection error. Please check your internet connectivity.")

            else ->
                IllegalStateException(error.message ?: "Failed to generate response. Please try again.")
        }
    }
}
