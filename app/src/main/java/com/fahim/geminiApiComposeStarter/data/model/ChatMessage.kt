package com.fahim.geminiApiComposeStarter.data.model

import java.util.UUID

enum class Participant {
    USER,
    GEMINI,
}

/**
 * Domain model representing a single chat message.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)
