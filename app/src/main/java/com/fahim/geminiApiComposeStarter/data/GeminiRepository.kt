package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call and local history persistence. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    fun getMessagesFlow(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
}
