package com.fahim.geminiApiComposeStarter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: Long,
    val text: String,
    val isUser: Boolean,
)