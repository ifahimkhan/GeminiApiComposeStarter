package com.example.c008_nishit_assignment1.data

import com.example.c008_nishit_assignment1.data.security.ApiKeyManager
import com.google.genai.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of GeminiRepository using the official Google GenAI SDK (com.google.genai).
 */
class GeminiRepositoryImpl(
    private val apiKeyManager: ApiKeyManager
) : GeminiRepository {

    override suspend fun generateResponse(
        prompt: String
    ): Result<String> = withContext(Dispatchers.IO) {

        val apiKey = apiKeyManager
            .getDecryptedApiKey()
            .trim()

        if (apiKey.isBlank() || apiKey == "your_api_key_here") {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties."
                )
            )
        }

        try {
            val client = Client.builder()
                .apiKey(apiKey)
                .build()

            val response = client.models.generateContent(
                "gemini-3.6-flash",
                prompt,
                null
            )

            val text = response.text()?.trim()

            if (text.isNullOrBlank()) {
                Result.failure(
                    Exception("Gemini returned an empty response.")
                )
            } else {
                Result.success(text)
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: ""

            when {
                errorMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
                errorMessage.contains("API key not valid", ignoreCase = true) ||
                errorMessage.contains("UNAUTHENTICATED", ignoreCase = true) -> {
                    Result.failure(
                        Exception(
                            "Gemini authentication failed. Please check your Gemini API key and API configuration.",
                            e
                        )
                    )
                }

                else -> {
                    Result.failure(
                        Exception(
                            errorMessage.ifBlank {
                                "Failed to generate a Gemini response."
                            },
                            e
                        )
                    )
                }
            }
        }
    }
}
