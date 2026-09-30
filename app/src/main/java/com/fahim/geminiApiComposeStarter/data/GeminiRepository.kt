package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String> = generateText(prompt, emptyList())
    suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> = generateText(prompt)
}
