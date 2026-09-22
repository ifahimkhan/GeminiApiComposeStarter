package com.fahim.geminiApiComposeStarter.data

/** One bubble in the conversation. [id] is assigned by the database and is used as the stable list key. */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
)
