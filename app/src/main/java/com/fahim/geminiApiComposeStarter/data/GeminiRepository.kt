package com.fahim.geminiApiComposeStarter.data

/** Data model for conversation history items. */
data class HistoryItem(
    val text: String,
    val isFromUser: Boolean
)

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(
        prompt: String,
        history: List<HistoryItem> = emptyList()
    ): Result<String>
}
