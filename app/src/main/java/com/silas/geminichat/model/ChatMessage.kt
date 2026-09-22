package com.silas.geminichat.model

/** Who authored a message in the conversation. */
enum class MessageRole { USER, ASSISTANT }

/** A single message shown in the chat. */
data class ChatMessage(
    val id: Long = 0L,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
