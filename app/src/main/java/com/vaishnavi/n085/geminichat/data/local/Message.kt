package com.vaishnavi.n085.geminichat.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageRole { USER, ASSISTANT }

/** A single chat message persisted in Room so history survives app restarts. */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
