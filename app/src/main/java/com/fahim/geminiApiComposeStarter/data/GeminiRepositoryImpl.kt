package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel

class GeminiRepositoryImpl(
    private val apiKey: String
) : GeminiRepository {

    // Fixed: Valid, fast model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = apiKey
    )

    override suspend fun generateText(prompt: String): Result<String> {
        return try {
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "No response")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}