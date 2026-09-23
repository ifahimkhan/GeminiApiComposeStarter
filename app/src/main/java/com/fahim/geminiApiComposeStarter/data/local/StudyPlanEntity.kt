package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val targetExamDate: String,
    val dailyTimeMinutes: Int,
    val planJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)
