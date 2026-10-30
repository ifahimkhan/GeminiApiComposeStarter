package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.SecureKeyStore
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val secureKeyStore: SecureKeyStore,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun isApiKeyConfigured(): Boolean {
        return secureKeyStore.hasApiKey()
    }

    override suspend fun generateText(prompt: String): Result<String> {
        val apiKey = secureKeyStore.getApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("API key not configured. Please add GEMINI_API_KEY to local.properties and rebuild."))
        }
        return try {
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName} - ${e.message}")
            Result.failure(e)
        }
    }
}
