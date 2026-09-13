package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    indices = [Index("conversationId")],
)
data class ChatMessageEntity(
    @PrimaryKey val id: Long,
    val conversationId: Long,
    val text: String,
    val author: String,
    val timestamp: Long,
)