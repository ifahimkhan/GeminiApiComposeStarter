package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.RequestTimeoutException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(modelName = modelName, apiKey = apiKey)

    override suspend fun generateText(
        prompt: String,
        history: List<ConversationMessage>,
    ): Result<String> = try {
        val chat = model.startChat(history = history.map { it.toContent() })
        val response = chat.sendMessage(prompt)
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
        Result.failure(IllegalStateException(e.toUserMessage(), e))
    }

    private fun ConversationMessage.toContent(): Content = content(role.toGeminiRole()) {
        text(this@toContent.text)
    }

    private fun ConversationRole.toGeminiRole(): String = when (this) {
        ConversationRole.USER -> "user"
        ConversationRole.MODEL -> "model"
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is InvalidAPIKeyException -> "Gemini rejected the API key. Check GEMINI_API_KEY in local.properties and rebuild."
        is QuotaExceededException -> "Gemini quota is exhausted for this API key. Try again later or use another key."
        is RequestTimeoutException -> "Gemini took too long to respond. Check your connection and try again."
        is UnsupportedUserLocationException -> "Gemini API is not available from this location."
        is PromptBlockedException -> message ?: "Gemini blocked this prompt for safety reasons."
        is ResponseStoppedException -> message ?: "Gemini stopped generating the response."
        is ServerException -> readableCauseMessage("Gemini server error")
        is GoogleGenerativeAIException -> readableCauseMessage("Gemini request failed")
        else -> readableCauseMessage("Gemini request failed")
    }

    private fun Throwable.readableCauseMessage(prefix: String): String {
        val details = generateSequence(this) { it.cause }
            .mapNotNull { throwable -> throwable.message?.takeIf(String::isNotBlank) }
            .filterNot { it == "Something unexpected happened." }
            .distinct()
            .joinToString(separator = "\n")
        return if (details.isBlank()) {
            "$prefix. Check your internet connection, API key, and Logcat for details."
        } else {
            "$prefix:\n$details"
        }
    }
}
