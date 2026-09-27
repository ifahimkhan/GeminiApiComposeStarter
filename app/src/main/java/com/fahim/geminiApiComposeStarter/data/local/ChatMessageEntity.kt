package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus

@Entity(tableName = "orbit_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val status: String = "SENT"
) {
    fun toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id,
            text = text,
            isUser = isUser,
            timestamp = timestamp,
            status = when (status) {
                "GENERATING" -> MessageStatus.GENERATING
                "ERROR" -> MessageStatus.ERROR
                else -> MessageStatus.SENT
            }
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
                status = message.status.name
            )
        }
    }
}
