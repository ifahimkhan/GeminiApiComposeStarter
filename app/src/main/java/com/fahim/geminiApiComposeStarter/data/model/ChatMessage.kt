package com.fahim.geminiApiComposeStarter.data.model

/**
 * Domain representation of a chat message in the conversation.
 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
