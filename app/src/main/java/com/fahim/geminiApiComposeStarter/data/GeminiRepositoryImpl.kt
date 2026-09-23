package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-2.5-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        modelName = modelName,
    )

    override suspend fun generateText(prompt: String): Result<String> = try {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Result.failure(IllegalStateException("API key is empty"))
        } else {
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }

    override suspend fun generateChatResponse(
        history: List<ChatMessageEntity>,
        newPrompt: String,
    ): Result<String> = try {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            Result.failure(IllegalStateException("API key is empty"))
        } else {
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
            // Filter non-empty valid messages and construct history
            val validHistory = history.filter { it.text.isNotBlank() && !it.isError }
            val chatHistory = validHistory.map { entity ->
                content(role = if (entity.participant == MessageParticipant.USER) "user" else "model") {
                    text(entity.text)
                }
            }
            val chat = model.startChat(history = chatHistory)
            val response = chat.sendMessage(newPrompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateChatResponse failed", e)
        Result.failure(e)
    }
}
