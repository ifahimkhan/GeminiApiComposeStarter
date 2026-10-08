package com.n149.geminichat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single chat message persisted to the local DB.
 * Survives app restarts so users see their full conversation history.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** "user" or "model" */
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** True if this message is a loading placeholder */
    val isLoading: Boolean = false,
    /** Non-null means this message represents an error */
    val errorMessage: String? = null,
    /** Groups messages into distinct chat sessions for the slidebar history */
    val sessionId: String = "default"
)
