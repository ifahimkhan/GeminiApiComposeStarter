package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle

/** Abstraction over the Gemini call so the ViewModel can be unit tested with a fake. */
interface GeminiRepository {
    /**
     * Sends [prompt] together with the earlier [history] (multi-turn chat) and returns the reply.
     */
    suspend fun generateReply(
        history: List<ChatMessage>,
        prompt: String,
        style: ResponseStyle,
    ): Result<String>
}

/** Thrown when no API key was configured in local.properties / the environment. */
class MissingApiKeyException : IllegalStateException("Gemini API key is not configured")
