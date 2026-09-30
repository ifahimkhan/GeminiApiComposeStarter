package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private val FALLBACK_MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash", "gemini-3.8-flash", "gemini-flash-latest")

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        val modelsToTry = if (modelName == DEFAULT_MODEL) {
            FALLBACK_MODELS
        } else {
            listOf(modelName) + FALLBACK_MODELS.filter { it != modelName }
        }

        var lastException: Exception? = null
        for (candidateModel in modelsToTry) {
            try {
                val model = GenerativeModel(modelName = candidateModel, apiKey = apiKey)
                val response = model.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() }
                if (text != null) {
                    return Result.success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "generateContent failed with model $candidateModel: ${e.message}")
                lastException = e
            }
        }
        Log.e(TAG, "All models failed", lastException)
        return Result.failure(lastException ?: IllegalStateException("Failed to generate response"))
    }
}
