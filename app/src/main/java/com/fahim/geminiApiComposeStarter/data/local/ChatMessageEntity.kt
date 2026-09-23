package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole {
    USER,
    MODEL
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "USER" or "MODEL"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)
