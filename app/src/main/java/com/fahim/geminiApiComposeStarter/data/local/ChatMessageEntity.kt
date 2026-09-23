package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageParticipant {
    USER,
    MODEL,
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val text: String,
    val participant: MessageParticipant,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)
