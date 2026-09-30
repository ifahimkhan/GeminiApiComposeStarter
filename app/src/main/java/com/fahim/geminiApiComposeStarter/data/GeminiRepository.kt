package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/** Abstraction over Gemini text generation and chat history persistence. */
interface GeminiRepository {
    fun getChatHistory(): Flow<List<ChatMessageEntity>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun generateText(prompt: String): Result<String>
    suspend fun clearHistory()
}
