package com.n149.geminichat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a free-text feedback note the user typed about a Gemini response.
 *
 * No ratings or emoji — just the user's own words.
 * Linked to [MessageEntity] via foreign key (cascade-deletes with message).
 */
@Entity(
    tableName = "feedback",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("messageId", unique = true)]
)
data class FeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val messageId: Long,
    /** The user's own words about this answer — drives personalisation */
    val userText: String,
    val timestamp: Long = System.currentTimeMillis()
)
