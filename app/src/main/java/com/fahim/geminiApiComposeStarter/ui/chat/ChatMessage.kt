package com.fahim.geminiApiComposeStarter.ui.chat

enum class MessageRole {
    USER,
    GEMINI,
}

data class ChatMessage(
    val id: Long = 0,
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)
