package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val role: String,
    @ColumnInfo(index = true)
    val timestamp: Long,
) {
    fun toChatMessage(): ChatMessage {
        val parsedRole = try {
            ChatRole.valueOf(role)
        } catch (e: IllegalArgumentException) {
            ChatRole.USER
        }
        return ChatMessage(
            id = id,
            text = text,
            role = parsedRole,
            timestamp = timestamp,
        )
    }

    companion object {
        fun fromChatMessage(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                text = message.text,
                role = message.role.name,
                timestamp = message.timestamp,
            )
        }
    }
}
