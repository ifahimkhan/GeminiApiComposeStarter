package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.MemoryCategory
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryDao
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class UserMemoryRepository(
    private val memoryDao: UserMemoryDao,
) {
    val allMemories: Flow<List<UserMemoryEntity>> = memoryDao.getAllMemories()

    fun getMemoriesByCategory(category: MemoryCategory): Flow<List<UserMemoryEntity>> =
        memoryDao.getMemoriesByCategory(category.name)

    fun searchMemories(query: String): Flow<List<UserMemoryEntity>> =
        memoryDao.searchMemories(query)

    suspend fun getMemoriesSnapshot(): List<UserMemoryEntity> =
        memoryDao.getMemoriesSnapshot()

    /**
     * Saves or updates the user's name in memory.
     * Prevents stale duplicate memories when the name is updated (e.g. Shubh -> Rahul).
     */
    suspend fun saveName(name: String): UserMemoryEntity {
        val cleanName = name.trim().trimEnd('.', '!', '?', ',').replaceFirstChar { it.uppercase() }
        val snapshot = memoryDao.getMemoriesSnapshot()
        val existingNameMem = snapshot.firstOrNull { mem -> isNameMemory(mem) }

        return if (existingNameMem != null) {
            val updated = existingNameMem.copy(
                content = "Name: $cleanName",
                category = MemoryCategory.PROFILE.name,
                updatedAt = System.currentTimeMillis(),
            )
            memoryDao.updateMemory(updated)
            updated
        } else {
            val newMemory = UserMemoryEntity(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.PROFILE.name,
                content = "Name: $cleanName",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            memoryDao.insertMemory(newMemory)
            newMemory
        }
    }

    /**
     * Deletes any user memory representing the user's name/identity.
     */
    suspend fun deleteName(): Int {
        val snapshot = memoryDao.getMemoriesSnapshot()
        val toDelete = snapshot.filter { mem -> isNameMemory(mem) }
        toDelete.forEach { memoryDao.deleteMemoryById(it.id) }
        return toDelete.size
    }

    suspend fun saveMemory(
        content: String,
        category: MemoryCategory = MemoryCategory.GENERAL,
    ): UserMemoryEntity {
        val trimmed = content.trim()
        val snapshot = memoryDao.getMemoriesSnapshot()

        // Check if this fact is a name statement
        val isName = (category == MemoryCategory.PROFILE && (trimmed.contains("name is", ignoreCase = true) || trimmed.startsWith("name:", ignoreCase = true))) ||
            trimmed.startsWith("my name is", ignoreCase = true) ||
            trimmed.startsWith("name:", ignoreCase = true)

        if (isName) {
            val existingName = snapshot.firstOrNull { isNameMemory(it) }
            if (existingName != null) {
                val updated = existingName.copy(
                    content = trimmed,
                    category = MemoryCategory.PROFILE.name,
                    updatedAt = System.currentTimeMillis(),
                )
                memoryDao.updateMemory(updated)
                return updated
            }
        }

        // Deduplication: if an existing memory has the exact same content, update timestamp
        val existing = snapshot.firstOrNull { it.content.equals(trimmed, ignoreCase = true) }
        return if (existing != null) {
            val updated = existing.copy(updatedAt = System.currentTimeMillis())
            memoryDao.updateMemory(updated)
            updated
        } else {
            val newMemory = UserMemoryEntity(
                id = UUID.randomUUID().toString(),
                category = category.name,
                content = trimmed,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            memoryDao.insertMemory(newMemory)
            newMemory
        }
    }

    suspend fun deleteMemory(id: String) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun deleteByKeyword(keyword: String): Int {
        val cleaned = keyword.trim().trimEnd('.', '!', '?', ',').lowercase()
        if (cleaned == "my name" || cleaned == "name") {
            return deleteName()
        }
        val snapshot = memoryDao.getMemoriesSnapshot()
        val toDelete = snapshot.filter { it.content.contains(cleaned, ignoreCase = true) }
        toDelete.forEach { memoryDao.deleteMemoryById(it.id) }
        return toDelete.size
    }

    suspend fun saveMemory(
        content: String,
        categoryName: String,
    ): UserMemoryEntity {
        val cat = try {
            MemoryCategory.valueOf(categoryName.uppercase())
        } catch (_: Exception) {
            MemoryCategory.GENERAL
        }
        return saveMemory(content, cat)
    }

    suspend fun clearAllMemories() {
        clearAll()
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }

    private fun isNameMemory(mem: UserMemoryEntity): Boolean {
        val c = mem.content.lowercase().trim()
        return (mem.category == MemoryCategory.PROFILE.name && (c.contains("name") || c.startsWith("name:"))) ||
            c.startsWith("name:") ||
            c.startsWith("my name is")
    }
}
