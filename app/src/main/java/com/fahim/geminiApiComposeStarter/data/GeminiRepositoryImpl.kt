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
    private val fallbackModelName: String = GeminiConfig.FALLBACK_MODEL_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    @Volatile
    private var generativeModel: GenerativeModel? = null
    @Volatile
    private var fallbackGenerativeModel: GenerativeModel? = null
    @Volatile
    private var cachedApiKey: String? = null

    override suspend fun generateText(prompt: String): Result<String> = withContext(ioDispatcher) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties and rebuild.")
            )
        }

        val primaryModel = getOrCreateModel(apiKey)
        var lastException: Exception? = null

        // Attempt 1-2 on primary model (gemini-3.8-flash)
        for (attempt in 1..2) {
            try {
                val response = primaryModel.generateContent(prompt)
                val text = response.text?.takeIf { it.isNotBlank() }
                if (text != null) {
                    return@withContext Result.success(text)
                } else {
                    return@withContext Result.failure(IllegalStateException("Received empty response from Gemini."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                val msg = e.message.orEmpty()
                val isTransientCapacityIssue = msg.contains("503", ignoreCase = true) ||
                        msg.contains("UNAVAILABLE", ignoreCase = true) ||
                        msg.contains("high demand", ignoreCase = true) ||
                        msg.contains("MissingFieldException", ignoreCase = true)
                if (isTransientCapacityIssue && attempt < 2) {
                    kotlinx.coroutines.delay(1000L)
                } else if (!isTransientCapacityIssue) {
                    break
                }
            }
        }

        // If primary model hit capacity spike (503), attempt fallback within Gemini 3 family
        if (lastException != null) {
            val msg = lastException.message.orEmpty()
            val isCapacityIssue = msg.contains("503", ignoreCase = true) ||
                    msg.contains("UNAVAILABLE", ignoreCase = true) ||
                    msg.contains("high demand", ignoreCase = true) ||
                    msg.contains("MissingFieldException", ignoreCase = true)
            if (isCapacityIssue && fallbackModelName.isNotBlank() && fallbackModelName != modelName) {
                try {
                    Log.w(TAG, "Primary model $modelName high demand; falling back to $fallbackModelName")
                    val fallbackModel = getOrCreateFallbackModel(apiKey)
                    val response = fallbackModel.generateContent(prompt)
                    val text = response.text?.takeIf { it.isNotBlank() }
                    if (text != null) {
                        return@withContext Result.success(text)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastException = e
                }
            }
        }

        val friendlyError = mapExceptionToUserFriendlyError(
            lastException ?: IllegalStateException("An unexpected error occurred while communicating with Gemini.")
        )
        Log.e(TAG, "Gemini generation failed: ${friendlyError.message}")
        Result.failure(friendlyError)
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

    private fun getOrCreateFallbackModel(apiKey: String): GenerativeModel {
        val currentFallback = fallbackGenerativeModel
        if (currentFallback != null && cachedApiKey == apiKey) {
            return currentFallback
        }
        return synchronized(this) {
            val existing = fallbackGenerativeModel
            if (existing != null && cachedApiKey == apiKey) {
                existing
            } else {
                GenerativeModel(modelName = fallbackModelName, apiKey = apiKey).also {
                    fallbackGenerativeModel = it
                }
            }
        }
    }

    internal fun mapExceptionToUserFriendlyError(e: Exception): Exception {
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
            message.contains("503", ignoreCase = true) ||
            message.contains("UNAVAILABLE", ignoreCase = true) ||
            message.contains("high demand", ignoreCase = true) ||
            message.contains("MissingFieldException", ignoreCase = true) -> {
                IllegalStateException("The model is currently experiencing high demand. Please try again in a moment.", e)
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
