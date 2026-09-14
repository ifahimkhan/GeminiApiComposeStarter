package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long = 0,
    val prompt: String,
    val response: String,
    val modelName: String = "gemini-3.6-flash",
    val timestamp: Long = System.currentTimeMillis()
)
