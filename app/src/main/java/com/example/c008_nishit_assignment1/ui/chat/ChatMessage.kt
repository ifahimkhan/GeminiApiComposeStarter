package com.example.c008_nishit_assignment1.ui.chat

/**
 * UI model representing a chat message.
 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
