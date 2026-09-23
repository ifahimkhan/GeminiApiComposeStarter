package com.fahim.geminiApiComposeStarter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val messageId: String,

    val text: String,

    val isUser: Boolean,

    val timestamp: Long
)