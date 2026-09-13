package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Secondary constructor for direct String key (for tests or simple initialization)
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this({ apiKey }, modelName)

    @Volatile
    private var generativeModel: GenerativeModel? = null

    private suspend fun getOrCreateModel(): GenerativeModel? {
        val currentModel = generativeModel
        if (currentModel != null) return currentModel

        val key = apiKeyProvider()?.trim()
        if (key.isNullOrBlank()) return null

        return synchronized(this) {
            val existing = generativeModel
            if (existing != null) return@synchronized existing

            val newModel = GenerativeModel(modelName = modelName, apiKey = key)
            generativeModel = newModel
            newModel
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val model = getOrCreateModel()
            ?: return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is missing or invalid."))

        try {
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
            Log.e(TAG, "generateContent failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    override suspend fun generateTextWithHistory(
        history: List<Pair<String, Boolean>>,
        prompt: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val model = getOrCreateModel()
            ?: return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is missing or invalid."))

        try {
            val chat = model.startChat(
                history = history.map { (text, isUser) ->
                    content(role = if (isUser) "user" else "model") {
                        text(text)
                    }
                }
            )
            val response = chat.sendMessage(prompt)
            val responseText = response.text?.takeIf { it.isNotBlank() }
            if (responseText != null) {
                Result.success(responseText)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
}
