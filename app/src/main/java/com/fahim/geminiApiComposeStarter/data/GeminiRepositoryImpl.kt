package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val defaultModelName: String = UserPreferencesRepository.DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String, modelName: String?): Result<String> = try {
        val apiKey = apiKeyProvider()?.trim()
        if (apiKey.isNullOrBlank()) {
            Result.failure(IllegalStateException("GEMINI_API_KEY is missing. Add it to local.properties and rebuild."))
        } else {
            val candidate = modelName?.trim()?.takeIf { it.isNotBlank() } ?: defaultModelName
            // Ensure the model is active and supported; fallback to DEFAULT_MODEL if a retired model name is passed
            val resolvedModel = if (UserPreferencesRepository.AVAILABLE_MODELS.contains(candidate)) {
                candidate
            } else {
                defaultModelName
            }

            val model = GenerativeModel(modelName = resolvedModel, apiKey = apiKey)
            val response = model.generateContent(prompt)
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
        Log.e(TAG, "Gemini generation failed: ${e.javaClass.simpleName}: ${e.message}", e)
        // If the SDK wraps the root cause in an UnknownException("Something unexpected happened."),
        // extract the underlying cause for a much more informative and accurate error message.
        val message = e.cause?.message?.takeIf { it.isNotBlank() }
            ?: e.message?.takeIf { it.isNotBlank() }
            ?: "Failed to get response from Gemini."
        Result.failure(Exception(message, e))
    }
}
