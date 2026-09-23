package com.rannbir.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a message in the chat conversation history.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)
