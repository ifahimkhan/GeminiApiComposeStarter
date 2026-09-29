package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val modelName: String = DEFAULT_MODEL,
    private val contentGenerator: (suspend (model: GenerativeModel, prompt: String) -> String?)? = null,
) : GeminiRepository {

    constructor(
        apiKey: String,
        modelName: String = DEFAULT_MODEL,
    ) : this(apiKeyProvider = { apiKey }, modelName = modelName)

    @Volatile
    private var model: GenerativeModel? = null

    private fun getModel(): GenerativeModel {
        return model ?: synchronized(this) {
            model ?: run {
                val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Gemini API key is not available or failed decryption")
                GenerativeModel(modelName = modelName, apiKey = apiKey).also {
                    model = it
                }
            }
        }
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val generativeModel = getModel()
        val text = if (contentGenerator != null) {
            contentGenerator.invoke(generativeModel, prompt)
        } else {
            generativeModel.generateContent(prompt).text
        }?.takeIf { it.isNotBlank() }
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
