package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateTextWithHistory(history: List<Pair<String, Boolean>>, prompt: String): Result<String>
}
