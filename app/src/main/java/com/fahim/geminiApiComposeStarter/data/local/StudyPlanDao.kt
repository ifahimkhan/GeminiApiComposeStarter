package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActivePlan(): Flow<StudyPlanEntity?>

    @Query("SELECT * FROM study_plans WHERE id = :id")
    suspend fun getPlanById(id: String): StudyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: StudyPlanEntity)

    @Update
    suspend fun updatePlan(plan: StudyPlanEntity)

    @Query("UPDATE study_plans SET isActive = 0 WHERE id != :activeId")
    suspend fun setActivePlanOnly(activeId: String)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deletePlan(id: String)

    @Query("DELETE FROM study_plans")
    suspend fun clearAll()
}
