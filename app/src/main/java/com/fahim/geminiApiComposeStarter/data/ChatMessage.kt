package com.fahim.geminiApiComposeStarter.data

/**
 * A single message in the conversation, independent of how it is persisted (Room) or displayed
 * (Compose). [isFromUser] distinguishes the two bubble styles; [isError] marks a message that
 * represents a failed Gemini call rather than real assistant output.
 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
