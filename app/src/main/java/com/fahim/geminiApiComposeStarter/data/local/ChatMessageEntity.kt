package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.MessageSender

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        sender = if (sender == MessageSender.USER.name) MessageSender.USER else MessageSender.GEMINI,
        text = text,
        timestamp = timestamp,
    )

    companion object {
        fun fromDomain(message: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = message.id,
            sender = message.sender.name,
            text = message.text,
            timestamp = message.timestamp,
        )
    }
}
