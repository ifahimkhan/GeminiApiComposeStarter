package com.example.n089arieasampatassignment1.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    val isUser: Boolean,

    val timestamp: Long = System.currentTimeMillis()
)
