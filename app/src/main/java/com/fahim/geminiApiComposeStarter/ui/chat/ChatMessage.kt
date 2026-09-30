package com.fahim.geminiApiComposeStarter.ui.chat

/** A single message in the conversation. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)