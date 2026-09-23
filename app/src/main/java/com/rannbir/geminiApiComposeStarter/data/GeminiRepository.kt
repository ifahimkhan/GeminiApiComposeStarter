package com.rannbir.geminiApiComposeStarter.data

import com.rannbir.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interacting with the Gemini generative model and
 * the local Room chat memory.
 */
interface GeminiRepository {
    /**
     * Flow of stored conversation history from Room database.
     */
    fun getChatMessages(): Flow<List<ChatMessageEntity>>

    /**
     * Sends a prompt to Gemini with conversational memory (past turns),
     * stores the prompt and model response in Room, and returns the response.
     */
    suspend fun sendMessage(prompt: String): Result<String>

    /**
     * Convenience method for single-turn prompt generation.
     */
    suspend fun generateText(prompt: String): Result<String> = sendMessage(prompt)

    /**
     * Clears all stored messages from local Room memory.
     */
    suspend fun clearChatHistory()
}
