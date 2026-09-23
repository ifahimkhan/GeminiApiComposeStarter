package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call and chat message persistence. */
interface GeminiRepository {
    val messagesStream: Flow<List<ChatMessage>>
    suspend fun generateText(prompt: String): Result<String>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun clearHistory()
}
