package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

/**
 * [apiKeyProvider] decrypts the API key. It is called only once, at the moment the
 * GenerativeModel is created, so the plain key is never stored anywhere else.
 */
class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private var model: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {
        model?.let { return it }
        val newModel = GenerativeModel(modelName = modelName, apiKey = apiKeyProvider())
        model = newModel
        return newModel
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