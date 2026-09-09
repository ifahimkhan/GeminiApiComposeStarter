package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
    )

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            if (apiKey.isBlank()) {
                return Result.failure(
                    IllegalStateException("Gemini API key is empty")
                )
            }

            if (prompt.isBlank()) {
                return Result.failure(
                    IllegalArgumentException("Prompt is empty")
                )
            }

            Log.d(TAG, "Sending request to Gemini")

            val response = model.generateContent(prompt)

            val text = response.text?.trim()

            if (!text.isNullOrBlank()) {
                Log.d(TAG, "Gemini response received")
                Result.success(text)
            } else {
                Log.e(TAG, "Gemini returned an empty response")

                Result.failure(
                    IllegalStateException("Gemini returned an empty response")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Gemini request failed: ${e::class.java.name} | ${e.message}"
            )

            var cause: Throwable? = e
            while (cause != null) {
                Log.e(
                    TAG,
                    "CAUSE: ${cause::class.java.name} | ${cause.message}"
                )
                cause = cause.cause
            }

            Result.failure(
                Exception(
                    "${e::class.simpleName}: ${e.message ?: "Unknown error"}",
                    e
                )
            )
        }
    }
}