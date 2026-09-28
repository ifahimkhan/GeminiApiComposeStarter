package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageSender
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus

/**
 * Room database entity representing a stored chat message.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val sender: String, // "USER" or "GEMINI"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS", // "SUCCESS", "LOADING", "ERROR"
)

/**
 * Maps a Room entity to a domain ChatMessage model.
 */
fun ChatMessageEntity.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        text = text,
        sender = try {
            MessageSender.valueOf(sender)
        } catch (_: Exception) {
            MessageSender.GEMINI
        },
        timestamp = timestamp,
        status = try {
            MessageStatus.valueOf(status)
        } catch (_: Exception) {
            MessageStatus.SUCCESS
        },
    )
}

/**
 * Maps a domain ChatMessage model to a Room entity.
 */
fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        text = text,
        sender = sender.name,
        timestamp = timestamp,
        status = status.name,
    )
}
