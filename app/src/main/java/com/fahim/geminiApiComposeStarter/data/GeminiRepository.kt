package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    /**
     * Sends [prompt] to Gemini. [history] is the conversation so far, oldest first, so the model
     * answers with the multi-turn context rather than treating every prompt as a fresh question.
     */
    suspend fun generateText(prompt: String, history: List<ChatMessage> = emptyList()): Result<String>
}
