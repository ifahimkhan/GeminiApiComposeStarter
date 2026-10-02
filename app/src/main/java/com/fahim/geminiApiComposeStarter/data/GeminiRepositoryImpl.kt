package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestTimeoutException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-1.5-flash"

/** Only the most recent turns are sent as context, to bound request size and cost. */
private const val MAX_HISTORY_MESSAGES = 20

const val MISSING_API_KEY_MESSAGE =
    "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

class GeminiRepositoryImpl(
    /** Supplies the decrypted API key; invoked once, when the model is first created. */
    private val apiKeyProvider: suspend () -> String?,
    private val modelName: String = DEFAULT_MODEL,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeminiRepository {

    private val modelMutex = Mutex()
    private var model: GenerativeModel? = null

    override suspend fun generateText(
        history: List<ChatMessage>,
        prompt: String,
    ): Result<String> = withContext(ioDispatcher) {
        try {
            val model = getOrCreateModel()
                ?: return@withContext Result.failure(GeminiException(MISSING_API_KEY_MESSAGE))
            val chat = model.startChat(history.toContents())
            val text = chat.sendMessage(prompt).text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(GeminiException("Gemini returned an empty response."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Raw SDK errors can include server response bodies; keep them out of release logcat.
            if (BuildConfig.DEBUG) Log.e(TAG, "generateContent failed", e)
            Result.failure(GeminiException(e.toUserMessage(), e))
        }
    }

    private suspend fun getOrCreateModel(): GenerativeModel? = modelMutex.withLock {
        model ?: apiKeyProvider()
            ?.takeIf { it.isNotBlank() }
            ?.let { key -> GenerativeModel(modelName = modelName, apiKey = key) }
            ?.also { model = it }
    }
}

private fun List<ChatMessage>.toContents() =
    takeLast(MAX_HISTORY_MESSAGES)
        // Gemini expects the conversation to open with a user turn.
        .dropWhile { it.role != Role.USER }
        .map { message ->
            content(role = if (message.role == Role.USER) "user" else "model") {
                text(message.text)
            }
        }

private fun Exception.toUserMessage(): String = when (this) {
    is InvalidAPIKeyException -> "The Gemini API key is invalid."
    is QuotaExceededException -> "Quota exceeded. Please try again later."
    is PromptBlockedException -> "This prompt was blocked by Gemini's safety filters."
    is ResponseStoppedException -> "Gemini stopped the response early."
    is RequestTimeoutException -> "The request timed out. Check your connection and try again."
    is UnsupportedUserLocationException -> "Gemini is not available in your region."
    is ServerException -> "Gemini is unavailable right now. Please try again."
    else -> "Something went wrong. Please try again."
}
