package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepositoryImpl(
    private val apiKey: () -> String,
    private val modelName: () -> String,
) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> =
        generateConversation(listOf(ChatMessage(0, ChatRole.USER, prompt)))

    override suspend fun generateConversation(messages: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = apiKey()
            require(key.isNotBlank()) { "Add your Gemini API key in Settings before sending." }
            val model = GenerativeModel(modelName = modelName(), apiKey = key)
            val contents = messages.map { message ->
                content(role = if (message.role == ChatRole.USER) "user" else "model") { text(message.text) }
            }
            val response = model.generateContent(*contents.toTypedArray())
            val answer = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned an empty response. Try again.")
            Result.success(answer)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Never log raw request/SDK exceptions: they can contain credentials or prompt text.
            Result.failure(IllegalStateException(
                if (error is IllegalArgumentException) error.message
                else "Could not get a response. Check your connection, API key, model name, and quota, then retry."
            ))
        }
    }
}
