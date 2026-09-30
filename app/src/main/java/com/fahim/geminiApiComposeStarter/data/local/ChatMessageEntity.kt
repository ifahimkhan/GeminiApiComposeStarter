package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant

@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["conversationId"])],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String = "default_conversation",
    val text: String,
    val participant: String,
    val timestamp: Long,
    val isError: Boolean = false,
)

fun ChatMessageEntity.toDomainModel(): ChatMessage {
    return ChatMessage(
        id = id,
        conversationId = conversationId,
        text = text,
        participant = if (participant == Participant.USER.name) Participant.USER else Participant.MODEL,
        timestamp = timestamp,
        isError = isError,
    )
}

fun ChatMessage.toEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        conversationId = conversationId,
        text = text,
        participant = participant.name,
        timestamp = timestamp,
        isError = isError,
    )
}
