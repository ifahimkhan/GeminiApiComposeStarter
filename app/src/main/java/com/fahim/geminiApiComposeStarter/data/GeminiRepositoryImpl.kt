package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"

// gemini-1.5-flash is the model documented for the legacy com.google.ai.client.generativeai
// Android SDK (0.9.0) that this project depends on.
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(modelName = modelName, apiKey = apiKey)

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = model.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Gemini returned an empty response. Please try again."))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(IllegalStateException(e.toUserMessage(), e))
    }

    /**
     * Maps low-level SDK/network exceptions to messages that are safe and useful to show,
     * without ever exposing a raw stack trace or the API key to the user. Matched by simple
     * class name (rather than importing the SDK's exception subclasses directly) so this stays
     * resilient to minor SDK version differences.
     */
    private fun Exception.toUserMessage(): String {
        val typeName = this::class.simpleName.orEmpty()
        val lowerMessage = message?.lowercase().orEmpty()

        return when {
            this is UnknownHostException || this is IOException ->
                "No internet connection. Check your network and try again."
            typeName.contains("InvalidAPIKey", ignoreCase = true) ||
                lowerMessage.contains("api key") ->
                "The Gemini API key is invalid. Check local.properties."
            typeName.contains("QuotaExceeded", ignoreCase = true) ||
                lowerMessage.contains("quota") ->
                "Gemini quota exceeded. Please try again later."
            typeName.contains("PromptBlocked", ignoreCase = true) ||
                typeName.contains("ResponseStopped", ignoreCase = true) ->
                "That request couldn't be completed. Try rephrasing it."
            typeName.contains("Server", ignoreCase = true) ->
                "Gemini's servers had a problem. Please try again."
            !message.isNullOrBlank() -> message!!
            else -> "Something went wrong. Please try again."
        }
    }
}
