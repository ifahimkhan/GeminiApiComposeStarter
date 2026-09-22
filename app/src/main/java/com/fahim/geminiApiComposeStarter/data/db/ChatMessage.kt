package com.fahim.geminiApiComposeStarter.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Roles for chat messages. */
enum class MessageRole { USER, GEMINI }

/**
 * A single chat message persisted in Room.
 * Survives app restarts so the full conversation history is always available.
 */
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
)
