package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

interface GeminiRepository {
    suspend fun generateText(prompt: String, modelName: String = DEFAULT_MODEL): Result<String>
}

class GeminiRepositoryImpl(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) : GeminiRepository {

    override suspend fun generateText(prompt: String, modelName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
                return@withContext Result.failure(IllegalStateException("Gemini API Key missing in local.properties"))
            }

            val model = GenerativeModel(
                modelName = if (modelName.isBlank()) DEFAULT_MODEL else modelName,
                apiKey = apiKey
            )

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
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }
}
