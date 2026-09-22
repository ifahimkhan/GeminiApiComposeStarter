package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"
private val CANDIDATE_MODELS = listOf("gemini-3.6-flash", "gemini-3.5-flash", "gemini-flash-latest")

class GeminiRepositoryImpl(
    private val keyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        keyProvider = { apiKey },
        modelName = modelName,
    )

    override suspend fun generateText(prompt: String, systemInstruction: String?): Result<String> =
        withContext(Dispatchers.IO) {
            val apiKey = keyProvider()
            if (apiKey.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties.")
                )
            }

            val modelsToTry = if (modelName == DEFAULT_MODEL) {
                CANDIDATE_MODELS
            } else {
                listOf(modelName) + CANDIDATE_MODELS.filter { it != modelName }
            }

            var lastException: Throwable? = null

            for (candidateModel in modelsToTry) {
                for (attempt in 1..2) {
                    try {
                        val model = GenerativeModel(
                            modelName = candidateModel,
                            apiKey = apiKey,
                            systemInstruction = systemInstruction?.takeIf { it.isNotBlank() }?.let {
                                content { text(it) }
                            },
                        )

                        val response = model.generateContent(prompt)
                        val text = response.text?.takeIf { it.isNotBlank() }
                        if (text != null) {
                            return@withContext Result.success(text)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Attempt $attempt with model $candidateModel failed: ${e.localizedMessage}")
                        if (attempt < 2) {
                            delay(500)
                        }
                    }
                }
            }

            val friendlyError = formatFriendlyError(lastException)
            Result.failure(IllegalStateException(friendlyError, lastException))
        }

    private fun formatFriendlyError(e: Throwable?): String {
        if (e == null) return "Unknown error occurred"
        val message = e.localizedMessage ?: e.message ?: "Failed to generate response"

        return when {
            message.contains("503") || message.contains("high demand", ignoreCase = true) ->
                "Gemini is currently experiencing high demand. Please try sending your query again in a moment."
            message.contains("404") || message.contains("not found", ignoreCase = true) ->
                "The requested Gemini model is currently not available. Please try again."
            message.contains("API_KEY_INVALID") || message.contains("API key not valid") ->
                "Invalid API Key. Please verify your GEMINI_API_KEY in local.properties."
            else ->
                message.substringBefore("\nkotlinx.serialization")
                    .removePrefix("Unexpected Response:")
                    .trim()
        }
    }
}
