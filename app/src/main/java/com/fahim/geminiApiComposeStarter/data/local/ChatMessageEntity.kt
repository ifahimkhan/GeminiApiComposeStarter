package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.domain.ChatMessage

/** Room row backing one [ChatMessage] so conversation history survives app restarts. */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
)

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    text = text,
    isFromUser = isFromUser,
    timestamp = timestamp,
)

fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    text = text,
    isFromUser = isFromUser,
    timestamp = timestamp,
)
