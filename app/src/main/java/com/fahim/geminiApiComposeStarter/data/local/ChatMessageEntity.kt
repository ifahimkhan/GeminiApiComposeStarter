package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant

/**
 * Room entity representing a persistent chat message in the SQLite database.
 * Transient UI states (loading, errors, Snackbars) are deliberately excluded.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val participant: String,
    val timestamp: Long,
) {
    fun toChatMessage(): ChatMessage {
        val role = when (participant) {
            Participant.USER.name -> Participant.USER
            else -> Participant.ASSISTANT
        }
        return ChatMessage(
            id = id,
            text = text,
            participant = role,
            timestamp = timestamp,
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                text = message.text,
                participant = message.participant.name,
                timestamp = message.timestamp,
            )
        }
    }
}
