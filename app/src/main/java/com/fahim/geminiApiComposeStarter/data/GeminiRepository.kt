package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over Gemini text generation and streaming multi-turn conversation calls.
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    fun sendMessageStream(prompt: String): Flow<String>
    fun resetChatSession() {}
}
