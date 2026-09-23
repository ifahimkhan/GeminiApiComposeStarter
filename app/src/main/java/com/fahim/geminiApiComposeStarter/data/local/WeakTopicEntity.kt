package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "weak_topics")
data class WeakTopicEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val topic: String,
    val subject: String = "General",
    val failureCount: Int = 1,
    val successCount: Int = 0,
    val lastPracticedAt: Long = System.currentTimeMillis(),
)
