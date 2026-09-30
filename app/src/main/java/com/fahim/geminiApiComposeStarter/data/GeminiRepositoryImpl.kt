package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.security.ApiKeyProvider
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException

private const val TAG = "GeminiRepository"

class GeminiRepositoryImpl(
    private val apiKeyProvider: ApiKeyProvider,
    private val modelName: String = GeminiConfig.MODEL_NAME,
    private val fallbackModelName: String = GeminiConfig.FALLBACK_MODEL_NAME,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val contentGenerator: ContentGenerator = DefaultContentGenerator(),
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> =
        generateText(prompt, emptyList())

    override suspend fun generateText(
        prompt: String,
        history: List<ChatMessage>,
    ): Result<String> = withContext(ioDispatcher) {
        val apiKey = apiKeyProvider.getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing. Please configure GEMINI_API_KEY in local.properties and rebuild.")
            )
        }

        var lastException: Exception? = null
        val contentHistory = buildContentHistory(history)

        // Attempt 1-2 on primary model (gemini-3.8-flash)
        for (attempt in 1..2) {
            try {
                val text = contentGenerator.generateContent(
                    modelName = modelName,
                    apiKey = apiKey,
                    prompt = prompt,
                    history = contentHistory,
                )
                if (text.isNotBlank()) {
                    return@withContext Result.success(text)
                } else {
                    return@withContext Result.failure(IllegalStateException("Received empty response from Gemini."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                val classified = mapExceptionToUserFriendlyError(e)
                // Only transient overloaded/capacity spikes (503) are retried automatically
                if (classified is GeminiException.Overloaded && attempt < 2) {
                    kotlinx.coroutines.delay(1000L)
                } else if (classified !is GeminiException.Overloaded) {
                    // Non-retryable errors (auth, quota, 404, blocked) abort retry loop immediately
                    break
                }
            }
        }

        // If primary model hit capacity spike (503), quota exhaustion (429), or model unavailable (404), attempt fallback model
        if (lastException != null) {
            val classified = mapExceptionToUserFriendlyError(lastException)
            val canFallback = (classified is GeminiException.Overloaded ||
                    classified is GeminiException.QuotaExceeded ||
                    classified is GeminiException.ModelNotFound)
            if (canFallback && fallbackModelName.isNotBlank() && fallbackModelName != modelName) {
                try {
                    Log.w(TAG, "Primary model $modelName unavailable/throttled; falling back to $fallbackModelName")
                    val text = contentGenerator.generateContent(
                        modelName = fallbackModelName,
                        apiKey = apiKey,
                        prompt = prompt,
                        history = contentHistory,
                    )
                    if (text.isNotBlank()) {
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
        Log.e(TAG, "Gemini generation failed: ${friendlyError.message}", lastException)
        Result.failure(friendlyError)
    }

    internal fun buildContentHistory(history: List<ChatMessage>): List<Content> {
        val result = mutableListOf<Content>()
        for (message in history) {
            val cleanText = message.text.trim()
            if (cleanText.isEmpty()) continue

            val role = if (message.participant == Participant.USER) "user" else "model"
            // Google Gemini API requires conversation history to begin with a user message
            if (result.isEmpty() && role != "user") continue

            val last = result.lastOrNull()
            if (last != null && last.role == role) {
                // If consecutive turns have the same role, combine their text parts
                val prevText = (last.parts.firstOrNull() as? TextPart)?.text.orEmpty()
                result[result.lastIndex] = Content(
                    role = role,
                    parts = listOf(TextPart("$prevText\n$cleanText")),
                )
            } else {
                result.add(Content(role = role, parts = listOf(TextPart(cleanText))))
            }
        }
        // startChat + sendMessage(prompt) will append the new user prompt at the end.
        // Therefore, the history provided to startChat must end with a model turn.
        // If history ends with a user turn, drop it so that user/model strictly alternate.
        while (result.isNotEmpty() && result.last().role == "user") {
            result.removeAt(result.lastIndex)
        }
        return result
    }

    internal fun mapExceptionToUserFriendlyError(e: Exception): Exception {
        if (e is GeminiException) return e

        val causes = generateSequence<Throwable>(e) { it.cause }.toList()
        val allMessages = causes.mapNotNull { it.message }.joinToString(" ")

        val isNetwork = causes.any {
            it is UnknownHostException || it is SocketTimeoutException || it is IOException ||
            it.javaClass.name.contains("ConnectException", ignoreCase = true) ||
            it.javaClass.name.contains("TimeoutException", ignoreCase = true)
        } || allMessages.contains("Unable to resolve host", ignoreCase = true) ||
             allMessages.contains("Failed to connect", ignoreCase = true) ||
             allMessages.contains("network", ignoreCase = true) ||
             allMessages.contains("No address associated with hostname", ignoreCase = true)

        if (isNetwork) {
            return GeminiException.Network("Network connection error. Please check your internet connection and try again.", e)
        }

        if (causes.any { it is InvalidAPIKeyException }) {
            return GeminiException.Authentication("Invalid API key credentials. Please verify your GEMINI_API_KEY.", e)
        }

        if (causes.any { it is QuotaExceededException }) {
            return GeminiException.QuotaExceeded("Rate limit or quota exceeded. Please wait a moment and try again.", e)
        }

        if (causes.any { it is UnsupportedUserLocationException }) {
            return GeminiException.PermissionDenied("User location is not supported for Gemini API.", e)
        }

        if (causes.any { it is PromptBlockedException } || causes.any { it is ResponseStoppedException }) {
            return GeminiException.ContentBlocked("Response was blocked due to safety or policy settings.", e)
        }

        return when {
            allMessages.contains("API_KEY_INVALID", ignoreCase = true) ||
            allMessages.contains("401", ignoreCase = true) ||
            allMessages.contains("invalid api key", ignoreCase = true) -> {
                GeminiException.Authentication("Invalid API key credentials. Please verify your GEMINI_API_KEY.", e)
            }
            allMessages.contains("PERMISSION_DENIED", ignoreCase = true) ||
            allMessages.contains("403", ignoreCase = true) -> {
                GeminiException.PermissionDenied("Authentication or permission denied for Gemini API.", e)
            }
            allMessages.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            allMessages.contains("429", ignoreCase = true) ||
            allMessages.contains("quota", ignoreCase = true) -> {
                GeminiException.QuotaExceeded("Rate limit or quota exceeded. Please wait a moment and try again.", e)
            }
            allMessages.contains("503", ignoreCase = true) ||
            allMessages.contains("UNAVAILABLE", ignoreCase = true) ||
            allMessages.contains("high demand", ignoreCase = true) ||
            allMessages.contains("MissingFieldException", ignoreCase = true) ||
            causes.any { it is SerializationException } -> {
                GeminiException.Overloaded("The model is currently experiencing high demand. Please try again in a moment.", e)
            }
            allMessages.contains("NOT_FOUND", ignoreCase = true) ||
            allMessages.contains("404", ignoreCase = true) ||
            allMessages.contains("model not found", ignoreCase = true) -> {
                GeminiException.ModelNotFound("The requested model ($modelName) is currently unavailable.", e)
            }
            allMessages.contains("SAFETY", ignoreCase = true) ||
            allMessages.contains("blocked", ignoreCase = true) -> {
                GeminiException.ContentBlocked("Response was blocked due to safety or policy settings.", e)
            }
            allMessages.contains("INVALID_ARGUMENT", ignoreCase = true) ||
            allMessages.contains("400", ignoreCase = true) ||
            allMessages.contains("bad request", ignoreCase = true) -> {
                GeminiException.BadRequest("Invalid request parameters or format.", e)
            }
            else -> {
                GeminiException.Unknown(e.message?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred while communicating with Gemini.", e)
            }
        }
    }
}
