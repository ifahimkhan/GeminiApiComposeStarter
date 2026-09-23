package com.fahim.geminiApiComposeStarter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages"
)
data class ChatMessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val text: String,

    val isUser: Boolean,

    val timestamp: Long =
        System.currentTimeMillis()
)