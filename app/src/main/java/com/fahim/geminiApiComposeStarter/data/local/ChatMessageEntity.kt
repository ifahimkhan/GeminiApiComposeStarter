package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: String = "default",
    val role: String, // "user" or "gemini"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
