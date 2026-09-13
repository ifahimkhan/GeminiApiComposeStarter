package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.ui.chat.ChatAuthor
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(modelName = modelName, apiKey = apiKey)

    override suspend fun generateReply(conversation: List<ChatMessage>): Result<String> = try {
        require(conversation.isNotEmpty()) { "Conversation is empty" }
        val lastMessage = conversation.last()

        // Prior turns become the chat's history; the newest user message is sent separately.
        val history = conversation
            .dropLast(1)
            .map { message ->
                Content(
                    role = if (message.author == ChatAuthor.USER) "user" else "model",
                    parts = listOf(TextPart(message.text)),
                )
            }

        val chat = model.startChat(history = history)
        val response = chat.sendMessage(lastMessage.text)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateReply failed", e)
        Result.failure(e)
    }
}