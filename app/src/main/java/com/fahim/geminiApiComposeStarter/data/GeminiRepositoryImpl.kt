package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val getApiKey: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this({ apiKey }, modelName)

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey().trim()
        if (apiKey.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("GEMINI_API_KEY is missing or invalid. Please check local.properties.")
            )
        }

        try {
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response received from Gemini API"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "generateContent failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}
