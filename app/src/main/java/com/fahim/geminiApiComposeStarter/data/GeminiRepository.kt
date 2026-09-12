package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateConversation(messages: List<com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage>): Result<String> =
        generateText(messages.last().text)
}
