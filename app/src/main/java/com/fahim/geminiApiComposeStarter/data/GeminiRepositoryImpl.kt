package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepositoryImpl(private val apiKeyVault: ApiKeyVault) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = apiKeyVault.readForModel()
                ?: return@withContext Result.failure(IllegalStateException("Add GEMINI_API_KEY to local.properties and rebuild."))
            val model = GenerativeModel(modelName = "gemini-3.6-flash", apiKey = key)
            val response = model.generateContent(prompt).text
            if (response.isNullOrBlank()) Result.failure(IllegalStateException("Gemini returned an empty response."))
            else Result.success(response)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
