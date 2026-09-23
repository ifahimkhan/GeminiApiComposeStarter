package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MemoryCategory {
    PROFILE,
    PREFERENCE,
    ACADEMIC,
    GOAL,
    GENERAL,
}

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String = MemoryCategory.GENERAL.name,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
