package com.fahim.geminiApiComposeStarter.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SenderRole {
    USER,
    ASSISTANT,
    SYSTEM
}

enum class MessageDeliveryStatus {
    SENT,
    DELIVERED,
    ERROR
}

/**
 * Persistent Room Entity for chat history messages.
 * Survives process death, orientation changes, and device reboots.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderRole: SenderRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageDeliveryStatus = MessageDeliveryStatus.DELIVERED
)
