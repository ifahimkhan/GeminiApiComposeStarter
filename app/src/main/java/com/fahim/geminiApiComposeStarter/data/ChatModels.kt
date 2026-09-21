package com.fahim.geminiApiComposeStarter.data

data class ChatMessage(val id: Long, val text: String, val isUser: Boolean)

data class Conversation(
    val id: Long,
    val title: String,
    val updatedAt: Long,
    val messages: List<ChatMessage>,
)
