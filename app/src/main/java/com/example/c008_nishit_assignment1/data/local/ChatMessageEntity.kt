package com.example.c008_nishit_assignment1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a chat message stored in the database.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val sender: String, // "USER" or "GEMINI"
    val timestamp: Long = System.currentTimeMillis()
)
