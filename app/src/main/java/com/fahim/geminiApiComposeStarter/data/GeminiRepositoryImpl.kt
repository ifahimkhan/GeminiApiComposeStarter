package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val keyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    @Volatile
    private var model: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {
        model?.let { return it }
        val apiKey = keyProvider()
        val created = GenerativeModel(modelName = modelName, apiKey = apiKey)
        model = created
        return created
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val response = getModel().generateContent(prompt)
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