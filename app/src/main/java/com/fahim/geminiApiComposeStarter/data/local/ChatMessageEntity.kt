package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val participant: String,
    val timestamp: Long,
    val isError: Boolean = false,
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        participant = try {
            Participant.valueOf(participant)
        } catch (_: Exception) {
            Participant.GEMINI
        },
        timestamp = timestamp,
        isError = isError,
    )

    companion object {
        fun fromDomain(msg: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = msg.id,
            text = msg.text,
            participant = msg.participant.name,
            timestamp = msg.timestamp,
            isError = msg.isError,
        )
    }
}
