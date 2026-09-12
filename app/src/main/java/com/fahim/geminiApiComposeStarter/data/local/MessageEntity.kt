package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.MessageSender

@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val text: String,
    val sender: String, // "USER" or "GEMINI"
    val timestamp: Long
) {
    fun toChatMessage(): ChatMessage {
        val messageSender = try {
            MessageSender.valueOf(sender)
        } catch (e: Exception) {
            MessageSender.GEMINI
        }
        return ChatMessage(
            id = id,
            text = text,
            sender = messageSender,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage): MessageEntity {
            return MessageEntity(
                id = message.id,
                text = message.text,
                sender = message.sender.name,
                timestamp = message.timestamp
            )
        }
    }
}
