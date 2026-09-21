package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini calls so the ViewModels can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>

    /** Sends [prompt] to Gemini with the earlier messages of the conversation as context. */
    suspend fun chat(history: List<ChatMessage>, prompt: String): Result<String>
}
