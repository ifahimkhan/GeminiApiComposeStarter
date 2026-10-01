package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fahim.geminiApiComposeStarter.model.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
    )

    companion object {
        fun fromDomain(domain: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = domain.id,
            text = domain.text,
            isUser = domain.isUser,
            timestamp = domain.timestamp,
        )
    }
}
