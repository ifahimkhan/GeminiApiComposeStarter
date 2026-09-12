package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

enum class MessageSender {
    USER, GEMINI
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis()
)
