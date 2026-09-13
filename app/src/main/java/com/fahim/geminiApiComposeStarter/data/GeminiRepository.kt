package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

const val MISSING_API_KEY_MESSAGE =
    "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

/** Abstraction over the Gemini call so the ViewModel can be unit tested. */
interface GeminiRepository {
    /**
     * Sends [prompt] as the next user turn, with [history] as prior context.
     * Failures are returned as [Result.failure] rather than thrown.
     */
    suspend fun generateReply(history: List<ChatMessage>, prompt: String): Result<String>
}

class MissingApiKeyException : IllegalStateException(MISSING_API_KEY_MESSAGE)

class EmptyResponseException : IllegalStateException("Gemini returned an empty response")
