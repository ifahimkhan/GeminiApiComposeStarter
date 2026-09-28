package com.fahim.geminiApiComposeStarter.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole {
    USER,
    GEMINI
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)
