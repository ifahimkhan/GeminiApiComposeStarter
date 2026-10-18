package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    /**
     * Backward-compatible secondary constructor for tests or callers supplying a direct key.
     */
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this({ apiKey }, modelName)

    /**
     * Lazily initialized GenerativeModel. Decryption occurs only at the moment this model
     * is created in memory. The decrypted key exists strictly in memory.
     */
    private val model: GenerativeModel? by lazy {
        val key = apiKeyProvider()
        if (!key.isNullOrBlank()) {
            GenerativeModel(modelName = modelName, apiKey = key)
        } else {
            null
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val generativeModel = model
            ?: return Result.failure(IllegalStateException("Gemini API key is missing or could not be decrypted."))
        val response = generativeModel.generateContent(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName}")
        Result.failure(e)
    }
}
