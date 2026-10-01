package com.fahim.geminiApiComposeStarter.model

/**
 * Domain representation of a chat message in the conversation.
 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
