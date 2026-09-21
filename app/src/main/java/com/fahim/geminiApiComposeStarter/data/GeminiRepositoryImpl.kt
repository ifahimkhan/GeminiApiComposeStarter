package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKey: String = "",
    private val apiKeyProvider: (suspend () -> String)? = null,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private var generativeModel: GenerativeModel? = null

    init {
        if (apiKey.isNotBlank()) {
            generativeModel = GenerativeModel(modelName = modelName, apiKey = apiKey)
        }
    }

    private suspend fun getModel(): GenerativeModel {
        generativeModel?.let { return it }
        val resolvedKey = if (apiKey.isNotBlank()) {
            apiKey
        } else {
            apiKeyProvider?.invoke().orEmpty()
        }
        val model = GenerativeModel(modelName = modelName, apiKey = resolvedKey)
        generativeModel = model
        return model
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = getModel()
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("No response text returned by Gemini."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed: ${e.javaClass.simpleName}")
            val sanitizedMessage = when {
                e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ->
                    "Invalid API key. Please check your Gemini API key."
                e.message?.contains("QUOTA_EXCEEDED", ignoreCase = true) == true ||
                e.message?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true ->
                    "Quota exceeded. Please try again later."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "Network error. Please check your internet connection."
                else -> e.message ?: "Failed to generate response. Please try again."
            }
            Result.failure(Exception(sanitizedMessage))
        }
    }
}
