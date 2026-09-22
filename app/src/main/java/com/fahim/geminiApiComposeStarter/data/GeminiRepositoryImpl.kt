package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank() || key == "your_api_key_here" || key == "your_gemini_api_key_here") {
            return@withContext Result.failure(
                IllegalArgumentException("GEMINI_API_KEY is missing. Please add your key to local.properties.")
            )
        }

        try {
            val model = GenerativeModel(modelName = modelName, apiKey = key)
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e(TAG, "generateContent failed", e)
            Result.failure(
                Exception(e.message ?: "Failed to generate response. Check your API key and network connection.")
            )
        }
    }
}
