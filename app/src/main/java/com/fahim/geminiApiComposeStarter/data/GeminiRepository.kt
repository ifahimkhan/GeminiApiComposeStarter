package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.db.ChatMessage
import kotlinx.coroutines.flow.Flow

/** Abstraction over Gemini text generation and chat persistence. */
interface GeminiRepository {
    /** Send a user prompt and get a Gemini response. Both are persisted to Room. */
    suspend fun sendMessage(prompt: String): Result<String>

    /** Observe the full chat history as a live stream from Room. */
    fun getChatHistory(): Flow<List<ChatMessage>>

    /** Wipe all stored chat messages. */
    suspend fun clearHistory()
}
