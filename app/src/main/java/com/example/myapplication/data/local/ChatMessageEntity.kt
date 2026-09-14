package com.example.myapplication.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
