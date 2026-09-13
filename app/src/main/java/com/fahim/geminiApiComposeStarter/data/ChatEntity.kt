package com.fahim.geminiApiComposeStarter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val participant: String, // "USER" or "MODEL"
    val timestamp: Long = System.currentTimeMillis()
)