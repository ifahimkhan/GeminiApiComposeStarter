package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/** Abstraction over Gemini text generation & local Room persistence. */
interface GeminiRepository {
    fun getChatHistory(): Flow<List<ChatMessageEntity>>
    suspend fun saveMessage(sender: String, content: String)
    suspend fun clearHistory()
    suspend fun generateText(prompt: String): Result<String>
}
