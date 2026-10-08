package com.n149.geminichat.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database holding chat history and user feedback.
 * Version 3 adds sessionId to messages for slidebar conversation history.
 */
@Database(
    entities = [MessageEntity::class, FeedbackEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun feedbackDao(): FeedbackDao
}
