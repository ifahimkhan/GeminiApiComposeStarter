package com.example.geministarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted chat turn. Storing both roles in one table (rather than
 * separate user/model tables) keeps ordering trivial via [timestamp].
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)
