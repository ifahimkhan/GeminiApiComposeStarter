package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    /** Sends [prompt] with the earlier turns in [history] as conversational context. */
    suspend fun generateText(history: List<ChatMessage>, prompt: String): Result<String>
}

/** Failure whose [message] is safe to show to the user (no raw server or SDK details). */
class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)
