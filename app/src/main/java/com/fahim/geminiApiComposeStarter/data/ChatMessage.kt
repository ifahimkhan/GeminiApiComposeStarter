package com.fahim.geminiApiComposeStarter.data

import java.util.UUID

enum class MessageSender {
    USER,
    GEMINI,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)
