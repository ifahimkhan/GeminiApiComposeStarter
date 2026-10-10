package com.fahim.geminiApiComposeStarter.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "DRIVER" or "PITWALL"
    val text: String,
    val timestamp: String,
    val isPending: Boolean = false,
    val isError: Boolean = false
)
