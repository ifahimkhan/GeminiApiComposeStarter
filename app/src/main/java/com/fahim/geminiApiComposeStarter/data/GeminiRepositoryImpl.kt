package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import com.google.ai.client.generativeai.type.content
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val primaryModel = GenerativeModel(modelName = modelName, apiKey = apiKey)

    // Fallback models if primary model experiences temporary 503 high-demand spikes
    private val fallbackModelNames = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        .filter { it != modelName }

    override suspend fun generateText(prompt: String): Result<String> {
        return generateText(prompt, emptyList())
    }

    override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
        // 1. Try the primary configured model with history
        val primaryResult = executeGeneration(primaryModel, prompt, history)
        if (primaryResult.isSuccess) {
            return primaryResult
        }

        val primaryError = primaryResult.exceptionOrNull()
        val errorMsg = primaryError?.message.orEmpty()

        // 2. Check if the error is temporary model overload (503 / high demand)
        val isHighDemand = errorMsg.contains("503") ||
            errorMsg.contains("high demand", ignoreCase = true) ||
            errorMsg.contains("UNAVAILABLE", ignoreCase = true)

        if (isHighDemand) {
            // Briefly wait 800ms before attempting fallback
            delay(800)

            for (fallbackName in fallbackModelNames) {
                Log.w(TAG, "Primary model overloaded (503), trying fallback model $fallbackName")
                val fallbackModel = GenerativeModel(modelName = fallbackName, apiKey = apiKey)
                val fallbackResult = executeGeneration(fallbackModel, prompt, history)
                if (fallbackResult.isSuccess) {
                    return fallbackResult
                }
            }
        }

        // 3. Return clean, user-friendly error message (no raw JSON or internal MissingFieldException)
        val cleanMessage = formatCleanErrorMessage(errorMsg)
        return Result.failure(Exception(cleanMessage, primaryError))
    }

    private suspend fun executeGeneration(
        model: GenerativeModel,
        prompt: String,
        history: List<ChatMessage> = emptyList(),
    ): Result<String> = try {
        val contents = buildConversationContents(history, prompt)
        val response = model.generateContent(*contents.toTypedArray())
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed on ${model.modelName}", e)
        Result.failure(e)
    }

    private fun buildConversationContents(
        history: List<ChatMessage>,
        latestPrompt: String,
    ): List<Content> {
        val contents = mutableListOf<Content>()

        // Take up to the last 20 messages for conversational context
        val recentHistory = history.takeLast(20)

        for (msg in recentHistory) {
            val trimmedText = msg.text.trim()
            if (trimmedText.isEmpty()) continue

            val role = if (msg.participant == Participant.USER) "user" else "model"

            // Gemini API requires the first turn to be from "user"
            if (contents.isEmpty() && role != "user") {
                continue
            }

            // Gemini API requires alternating roles: merge if same role consecutively
            if (contents.isNotEmpty() && contents.last().role == role) {
                val previous = contents.removeAt(contents.size - 1)
                val mergedText = previous.parts.filterIsInstance<TextPart>().joinToString("\n") { it.text } + "\n" + trimmedText
                contents.add(content(role = role) { text(mergedText) })
            } else {
                contents.add(content(role = role) { text(trimmedText) })
            }
        }

        // Now append the current user prompt
        val currentPromptTrimmed = latestPrompt.trim()
        if (contents.isNotEmpty() && contents.last().role == "user") {
            val previous = contents.removeAt(contents.size - 1)
            val mergedText = previous.parts.filterIsInstance<TextPart>().joinToString("\n") { it.text } + "\n" + currentPromptTrimmed
            contents.add(content(role = "user") { text(mergedText) })
        } else {
            contents.add(content(role = "user") { text(currentPromptTrimmed) })
        }

        return contents
    }

    private fun formatCleanErrorMessage(rawMessage: String): String {
        if (rawMessage.isBlank()) {
            return "Check your connection and try again."
        }

        if (rawMessage.contains("503") ||
            rawMessage.contains("high demand", ignoreCase = true) ||
            rawMessage.contains("UNAVAILABLE", ignoreCase = true)
        ) {
            return "This model is currently experiencing high demand. Please tap Retry to try again."
        }

        if (rawMessage.contains("429") ||
            rawMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            rawMessage.contains("quota", ignoreCase = true)
        ) {
            return "Rate limit or quota exceeded. Please wait a moment before trying again."
        }

        if (rawMessage.contains("403") ||
            rawMessage.contains("PERMISSION_DENIED", ignoreCase = true) ||
            rawMessage.contains("API_KEY_INVALID", ignoreCase = true)
        ) {
            return "Invalid or unauthorized API key. Check your GEMINI_API_KEY in local.properties."
        }

        if (rawMessage.contains("Unable to resolve host", ignoreCase = true) ||
            rawMessage.contains("ConnectException", ignoreCase = true) ||
            rawMessage.contains("SocketTimeoutException", ignoreCase = true) ||
            rawMessage.contains("timeout", ignoreCase = true)
        ) {
            return "Network connection error. Check your internet connection and try again."
        }

        // Extract "message": "..." if SDK threw MissingFieldException containing server JSON
        val messageRegex = Regex(""""message"\s*:\s*"([^"]+)"""")
        val match = messageRegex.find(rawMessage)
        if (match != null) {
            return match.groupValues[1]
        }

        return rawMessage
    }
}
