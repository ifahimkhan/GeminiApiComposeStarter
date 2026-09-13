package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    fun getChatHistory(): Flow<List<ChatEntity>>
    suspend fun saveMessage(role: String, content: String)
    suspend fun clearHistory()
}
