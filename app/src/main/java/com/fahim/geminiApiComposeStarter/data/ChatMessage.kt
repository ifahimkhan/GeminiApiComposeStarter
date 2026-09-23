package com.fahim.geminiApiComposeStarter.data

import java.util.UUID

/**
 * Identifies the sender of a chat message.
 */
enum class Participant {
    USER,
    MODEL,
}

/**
 * Domain model representing an individual message in a chat conversation.
 *
 * @property id Unique identifier for the message, suitable for LazyColumn stable keys.
 * @property text Text content of the message.
 * @property participant Whether the message was sent by the USER or the MODEL (Gemini).
 * @property timestamp Unix epoch timestamp in milliseconds when the message was created.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
)
