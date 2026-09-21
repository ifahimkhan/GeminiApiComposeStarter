package com.fahim.geminiApiComposeStarter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val participant: String,
    val timestamp: Long,
)

/**
 * Converts a database entity to a UI domain [ChatMessage].
 * Safely defaults to [Participant.USER] if an unrecognized participant value is encountered.
 */
fun ChatMessageEntity.toChatMessage(): ChatMessage {
    val parsedParticipant = try {
        Participant.valueOf(participant)
    } catch (e: IllegalArgumentException) {
        Participant.USER
    }
    return ChatMessage(
        id = id,
        text = text,
        participant = parsedParticipant,
        timestamp = timestamp,
    )
}

/**
 * Converts a UI domain [ChatMessage] to a database entity.
 */
fun ChatMessage.toEntity(): ChatMessageEntity = ChatMessageEntity(
    id = id,
    text = text,
    participant = participant.name,
    timestamp = timestamp,
)
