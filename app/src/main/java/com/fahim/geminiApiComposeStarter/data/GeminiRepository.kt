package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.db.ConversationMessage
import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun getChatHistory(): Flow<List<ConversationMessage>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun generateText(prompt: String): Result<String>
    suspend fun clearHistory()
}
