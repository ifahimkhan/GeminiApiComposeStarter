package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    isFromUser = isFromUser,
    timestamp = timestamp,
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    text = text,
    isFromUser = isFromUser,
    timestamp = timestamp,
)
