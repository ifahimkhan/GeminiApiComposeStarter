package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model: GenerativeModel? by lazy {
        if (apiKey.isNotBlank()) {
            try {
                GenerativeModel(modelName = modelName, apiKey = apiKey)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GenerativeModel", e)
                null
            }
        } else {
            null
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val currentModel = model
        if (currentModel == null) {
            Result.failure(IllegalStateException("GEMINI_API_KEY is missing. Please add it to local.properties and rebuild."))
        } else {
            val response = currentModel.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }
}
