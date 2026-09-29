package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyProvider
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "GeminiRepository"

class GeminiRepositoryImpl(
    private val apiKeyProvider: ApiKeyProvider,
    private val modelName: String = GeminiConfig.MODEL_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    @Volatile
    private var generativeModel: GenerativeModel? = null
    @Volatile
    private var cachedApiKey: String? = null

    override suspend fun generateText(prompt: String): Result<String> = withContext(ioDispatcher) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties and rebuild.")
            )
        }

        try {
            val model = getOrCreateModel(apiKey)
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Received empty response from Gemini."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val friendlyError = mapExceptionToUserFriendlyError(e)
            Log.e(TAG, "Gemini generation failed: ${friendlyError.message}")
            Result.failure(friendlyError)
        }
    }

    private fun getOrCreateModel(apiKey: String): GenerativeModel {
        val currentModel = generativeModel
        if (currentModel != null && cachedApiKey == apiKey) {
            return currentModel
        }
        return synchronized(this) {
            val existing = generativeModel
            if (existing != null && cachedApiKey == apiKey) {
                existing
            } else {
                cachedApiKey = apiKey
                GenerativeModel(modelName = modelName, apiKey = apiKey).also {
                    generativeModel = it
                }
            }
        }
    }

    private fun mapExceptionToUserFriendlyError(e: Exception): Exception {
        val message = e.message.orEmpty()
        return when {
            e is UnknownHostException || e is SocketTimeoutException || e is IOException -> {
                IOException("Network connection error. Please check your internet connection and try again.", e)
            }
            message.contains("API_KEY_INVALID", ignoreCase = true) ||
            message.contains("401", ignoreCase = true) ||
            message.contains("invalid api key", ignoreCase = true) -> {
                IllegalArgumentException("Invalid API key credentials. Please verify your GEMINI_API_KEY.", e)
            }
            message.contains("PERMISSION_DENIED", ignoreCase = true) ||
            message.contains("403", ignoreCase = true) -> {
                SecurityException("Authentication or permission denied for Gemini API.", e)
            }
            message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            message.contains("429", ignoreCase = true) ||
            message.contains("quota", ignoreCase = true) -> {
                IllegalStateException("Rate limit or quota exceeded. Please wait a moment and try again.", e)
            }
            message.contains("NOT_FOUND", ignoreCase = true) ||
            message.contains("404", ignoreCase = true) ||
            message.contains("model not found", ignoreCase = true) -> {
                IllegalStateException("The requested model ($modelName) is currently unavailable.", e)
            }
            else -> {
                Exception(e.message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred while communicating with Gemini.", e)
            }
        }
    }
}
