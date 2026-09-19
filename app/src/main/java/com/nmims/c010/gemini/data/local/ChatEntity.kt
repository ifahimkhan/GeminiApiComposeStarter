package com.nmims.c010.gemini.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nmims.c010.gemini.data.Author
import com.nmims.c010.gemini.data.ChatMessage

@Entity(tableName = "messages")
data class ChatEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String,
    val timestamp: Long
)

fun ChatEntity.toModel() = ChatMessage(id, text, Author.valueOf(author), timestamp)

fun ChatMessage.toEntity() = ChatEntity(id, text, author.name, timestamp)
