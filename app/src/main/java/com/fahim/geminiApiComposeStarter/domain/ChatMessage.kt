package com.fahim.geminiApiComposeStarter.domain

/** A single turn in the conversation, shown as one chat bubble. */
data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
