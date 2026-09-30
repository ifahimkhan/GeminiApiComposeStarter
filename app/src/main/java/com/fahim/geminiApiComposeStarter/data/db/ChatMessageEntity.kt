package com.fahim.geminiApiComposeStarter.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "is_user")
    val isUser: Boolean,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
