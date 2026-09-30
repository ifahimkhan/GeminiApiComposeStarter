package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.db.MessageEntity
import kotlinx.coroutines.flow.Flow

/** Interface defining Gemini API requests and persistent chat storage. */
interface GeminiRepository {
    fun getChatHistory(): Flow<List<MessageEntity>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun generateText(prompt: String): Result<String>
    suspend fun clearHistory()
}
