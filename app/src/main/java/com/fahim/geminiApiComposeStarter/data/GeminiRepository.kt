package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call and local message persistence. */
interface GeminiRepository {
    fun observeConversation(): Flow<List<MessageEntity>>
    suspend fun sendPrompt(prompt: String): Result<String>
    suspend fun generateText(prompt: String): Result<String>
    suspend fun clearConversation()
}
