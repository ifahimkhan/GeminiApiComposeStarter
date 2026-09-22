package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.data.db.ChatDao
import com.fahim.geminiApiComposeStarter.data.db.ChatMessage
import com.fahim.geminiApiComposeStarter.data.db.MessageRole
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private const val TAG = "GeminiRepository"
// Use gemini-2.0-flash-lite — free-tier, fast, multi-turn capable
private const val DEFAULT_MODEL = "gemini-2.0-flash-lite"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
    private val chatDao: ChatDao,
) : GeminiRepository {

    private val model = GenerativeModel(modelName = modelName, apiKey = apiKey)

    // Multi-turn chat session — keeps conversation context across messages
    private val chatSession = model.startChat(history = emptyList())

    override suspend fun sendMessage(prompt: String): Result<String> {
        // Persist user message first so it appears immediately in the UI
        chatDao.insertMessage(ChatMessage(role = MessageRole.USER, text = prompt))

        return try {
            val response = chatSession.sendMessage(
                content(role = "user") { text(prompt) }
            )
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                chatDao.insertMessage(ChatMessage(role = MessageRole.GEMINI, text = text))
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            Result.failure(e)
        }
    }

    override fun getChatHistory(): Flow<List<ChatMessage>> = chatDao.getAllMessages()

    override suspend fun clearHistory() = chatDao.deleteAll()
}
