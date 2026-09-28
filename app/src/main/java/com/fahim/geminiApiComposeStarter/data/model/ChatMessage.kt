package com.fahim.geminiApiComposeStarter.data.model

/**
 * Sender role of the chat message.
 */
enum class MessageSender {
    USER,
    GEMINI
}

/**
 * Delivery or generation status of the chat message.
 */
enum class MessageStatus {
    SUCCESS,
    LOADING,
    ERROR
}

/**
 * Domain model representing a single chat message in the conversation.
 *
 * @param id Unique identifier (used as stable key in Compose LazyColumn).
 * @param text Content of the message.
 * @param sender Whether the message came from USER or GEMINI.
 * @param timestamp Epoch timestamp in milliseconds.
 * @param status Delivery/generation status (SUCCESS, LOADING, or ERROR).
 */
data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SUCCESS,
)
