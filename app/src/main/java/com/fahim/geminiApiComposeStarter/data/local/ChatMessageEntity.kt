package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant

/**
 * Room entity representing a persisted chat message.
 * Strictly persists conversation history; never stores API keys, credentials, or session secrets.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val participant: String,
    val timestamp: Long,
)

fun ChatMessageEntity.toChatMessage(): ChatMessage {
    val role = try {
        Participant.valueOf(participant)
    } catch (e: IllegalArgumentException) {
        Participant.ASSISTANT
    }
    return ChatMessage(
        id = id,
        text = text,
        participant = role,
        isPending = false,
        timestamp = timestamp,
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    text = text,
    participant = participant.name,
    timestamp = timestamp,
)
