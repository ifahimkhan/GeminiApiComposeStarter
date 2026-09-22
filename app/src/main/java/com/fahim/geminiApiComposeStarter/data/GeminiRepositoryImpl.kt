package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.0-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank() || key == "your_api_key_here" || key == "your_gemini_api_key_here") {
            return@withContext Result.failure(
                IllegalArgumentException("GEMINI_API_KEY is missing. Please add your key to local.properties.")
            )
        }

        // Try selected model first, followed by standard fallback models
        val candidateModels = listOf(modelName, "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b")
            .distinct()

        var lastException: Throwable? = null

        for (currModel in candidateModels) {
            try {
                val model = GenerativeModel(modelName = currModel, apiKey = key)
                val response = model.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() }
                if (text != null) {
                    return@withContext Result.success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "generateContent failed for model $currModel", e)
                lastException = e
            }
        }

        val errMsg = lastException?.message ?: ""
        val friendlyMessage = when {
            errMsg.contains("503") || errMsg.contains("experiencing high demand") || errMsg.contains("UNAVAILABLE") ->
                "The Gemini model is currently experiencing high demand (503). Please try sending your prompt again."
            else ->
                errMsg.ifBlank { "Failed to generate response. Please check your network connection and API key." }
        }

        Result.failure(Exception(friendlyMessage))
    }
}
