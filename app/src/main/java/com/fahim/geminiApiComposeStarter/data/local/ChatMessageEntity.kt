package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String = "default",
    val role: String,
    val content: String,
    val timestamp: Long,
    val isSaved: Boolean = false,
    val imageUri: String? = null,
)
