package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val isFromUser: Boolean,
    val createdAt: Long,
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    text = text,
    author = if (isFromUser) Author.USER else Author.MODEL,
    createdAt = createdAt,
)
