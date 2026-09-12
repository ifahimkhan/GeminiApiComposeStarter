package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_sessions",
)
data class ChatSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
