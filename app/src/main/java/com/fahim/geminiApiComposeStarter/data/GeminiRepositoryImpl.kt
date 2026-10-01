package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"


class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        modelName = modelName
    )

    @Volatile
    private var model: GenerativeModel? = null
    private val mutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun getOrCreateModel(): GenerativeModel {
        return model ?: mutex.withLock {
            model ?: run {
                // Decrypt key in memory strictly when creating the GenerativeModel
                val key = apiKeyProvider()
                GenerativeModel(modelName = modelName, apiKey = key).also {
                    model = it
                }
            }
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val generativeModel = getOrCreateModel()
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
            Log.e(TAG, "generateContent failed", e)
            Result.failure(e)
        }
    }
}
