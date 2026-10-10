package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/** Abstraction over Gemini text generation and Room persistence for PITWALL. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    fun getMessages(): Flow<List<ChatMessageEntity>>
    suspend fun saveMessage(message: ChatMessageEntity): Long
    suspend fun deleteMessage(id: Long)
    suspend fun clearMessages()
}
