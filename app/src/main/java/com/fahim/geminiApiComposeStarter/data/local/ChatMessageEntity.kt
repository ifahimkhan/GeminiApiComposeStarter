package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val text: String,

    val sender: String,

    val timestamp: Long = System.currentTimeMillis(),
)