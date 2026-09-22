package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
)

fun ChatMessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    author = if (isUser) Author.USER else Author.GEMINI,
    timestamp = timestamp,
)

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    text = text,
    isUser = author == Author.USER,
    timestamp = timestamp,
)
