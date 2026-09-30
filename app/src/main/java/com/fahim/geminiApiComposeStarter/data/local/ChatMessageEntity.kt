package com.fahim.geminiApiComposeStarter.data.local

/**
 * Entity representing a persistent chat message stored in the local database.
 * Preserves full conversation history across app restarts.
 */
data class ChatMessageEntity(
    val id: Long = 0,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

enum class MessageSender {
    USER,
    GEMINI,
}
