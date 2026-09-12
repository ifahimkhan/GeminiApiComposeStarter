package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

/**
 * Gemini implementation that reads the decrypted key only when a request begins.
 *
 * The real API key is never logged, toasted, displayed, or written as plaintext.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val apiKey = apiKeyProvider()
                ?.trim()
                .orEmpty()

            if (apiKey.isBlank()) {
                return Result.failure(
                    IllegalStateException(
                        "GEMINI_API_KEY is unavailable."
                    )
                )
            }

            val model = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
            )

            val response = model.generateContent(prompt)
            val text = response.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(
                    IllegalStateException("Empty response from Gemini")
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    companion object {
        private const val DEFAULT_MODEL = "gemini-3.6-flash"
    }
}
