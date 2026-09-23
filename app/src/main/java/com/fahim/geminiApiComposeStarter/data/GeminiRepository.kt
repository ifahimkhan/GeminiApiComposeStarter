package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateChatResponse(
        history: List<ChatMessageEntity>,
        newPrompt: String,
    ): Result<String> = generateText(newPrompt)
}
