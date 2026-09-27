package com.fahim.geminiApiComposeStarter.data.model

import java.util.UUID

enum class MessageStatus {
    SENT,
    GENERATING,
    ERROR
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)
