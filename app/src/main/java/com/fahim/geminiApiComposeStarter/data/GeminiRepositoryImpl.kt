package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.util.Log
import com.fahim.geminiApiComposeStarter.data.security.SecureApiKeyManager
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    context: Context,
    private val fallbackApiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val secureApiKeyManager = SecureApiKeyManager(context)

    private var model: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {

        if (model != null) {
            return model!!
        }

        var apiKey = secureApiKeyManager.getApiKey()

        if (apiKey == null && fallbackApiKey.isNotBlank()) {
            secureApiKeyManager.storeApiKey(fallbackApiKey)
            apiKey = fallbackApiKey
        }

        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("Gemini API key is missing")
        }

        val newModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
        )

        model = newModel

        return newModel
    }

    override suspend fun generateText(prompt: String): Result<String> = try {

        val response = getModel().generateContent(prompt)

        val text = response.text?.takeIf { it.isNotBlank() }

        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(
                IllegalStateException("Empty response from Gemini")
            )
        }

    } catch (e: CancellationException) {
        throw e

    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }
}