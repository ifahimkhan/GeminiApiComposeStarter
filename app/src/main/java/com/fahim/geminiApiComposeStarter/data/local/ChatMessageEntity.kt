package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage

@Entity(tableName = "messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val isUser: Boolean,
    val createdAt: Long,
)

fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id = id,
    text = text,
    isUser = isUser,
    createdAt = createdAt,
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    text = text,
    isUser = isUser,
    createdAt = createdAt,
)
