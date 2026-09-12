package com.fahim.geminiApiComposeStarter.data

/**
 * Abstraction over Gemini so ChatViewModel remains unit-testable.
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
}
