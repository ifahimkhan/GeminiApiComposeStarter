package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class GeminiRepositoryImpl(private val keyStore: SecureApiKeyStore) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val model = GenerativeModel(modelName = MODEL_NAME, apiKey = keyStore.decryptForModel())
            val response = withTimeout(60_000) { model.generateContent(prompt) }
            val text = response.text?.takeIf { it.isNotBlank() }
                ?: return@withContext Result.failure(IllegalStateException("Gemini returned no text. Try another prompt."))
            Result.success(text)
        } catch (error: TimeoutCancellationException) {
            Result.failure(IllegalStateException("The request timed out. Please try again."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(IllegalStateException("Unable to reach Gemini. Check your connection, API access and quota, then retry."))
        }
    }

    companion object {
        const val MODEL_NAME = "gemini-3.6-flash"
    }
}
