package com.fahim.geminiApiComposeStarter.core.memory

import com.fahim.geminiApiComposeStarter.data.local.MemoryCategory
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryDao
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.data.repository.UserMemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MemoryEngineTest {

    private lateinit var fakeDao: FakeUserMemoryDao
    private lateinit var repository: UserMemoryRepository
    private lateinit var memoryEngine: MemoryEngine

    @Before
    fun setup() {
        fakeDao = FakeUserMemoryDao()
        repository = UserMemoryRepository(fakeDao)
        memoryEngine = MemoryEngine(repository)
    }

    @Test
    fun `remember command saves memory with correct category`() = runTest {
        val result = memoryEngine.processNaturalCommand("Remember that my name is Shubh")
        assertTrue(result is MemoryCommandResult.Saved)
        val saved = result as MemoryCommandResult.Saved
        assertEquals("my name is Shubh", saved.fact)
        assertEquals(MemoryCategory.PROFILE, saved.category)

        assertEquals(1, fakeDao.stored.size)
        assertEquals("my name is Shubh", fakeDao.stored.first().content)
    }

    @Test
    fun `what do you remember command returns all saved memories`() = runTest {
        repository.saveMemory("I study computer engineering", MemoryCategory.ACADEMIC)
        repository.saveMemory("I prefer concise answers", MemoryCategory.PREFERENCE)

        val result = memoryEngine.processNaturalCommand("What do you remember about me?")
        assertTrue(result is MemoryCommandResult.ListAll)
        val listResult = result as MemoryCommandResult.ListAll
        assertEquals(2, listResult.memories.size)
    }

    @Test
    fun `forget command deletes matching memory`() = runTest {
        repository.saveMemory("I have an exam on Friday", MemoryCategory.ACADEMIC)
        assertEquals(1, fakeDao.stored.size)

        val result = memoryEngine.processNaturalCommand("Forget that I have an exam on Friday")
        assertTrue(result is MemoryCommandResult.Forgotten)
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `forget everything command clears all memories`() = runTest {
        repository.saveMemory("Fact 1", MemoryCategory.GENERAL)
        repository.saveMemory("Fact 2", MemoryCategory.GENERAL)
        assertEquals(2, fakeDao.stored.size)

        val result = memoryEngine.processNaturalCommand("Forget everything you remember about me")
        assertTrue(result is MemoryCommandResult.ClearedAll)
        assertEquals(0, fakeDao.stored.size)
    }

    @Test
    fun `getMemoryContextPrompt formats memories for system instruction`() = runTest {
        repository.saveMemory("User prefers bullet points", MemoryCategory.PREFERENCE)

        val prompt = memoryEngine.getMemoryContextPrompt()
        assertTrue(prompt.contains("Persistent User Profile"))
        assertTrue(prompt.contains("User prefers bullet points"))
    }

    @Test
    fun `implicit name statement saves name as PROFILE memory`() = runTest {
        val result = memoryEngine.processNaturalCommand("My name is Shubh.")
        assertTrue(result is MemoryCommandResult.Saved)
        val saved = result as MemoryCommandResult.Saved
        assertEquals("Name: Shubh", saved.fact)
        assertEquals(MemoryCategory.PROFILE, saved.category)

        assertEquals(1, fakeDao.stored.size)
        assertEquals("Name: Shubh", fakeDao.stored.first().content)
        assertEquals(MemoryCategory.PROFILE.name, fakeDao.stored.first().category)
    }

    @Test
    fun `name update replaces existing name preventing stale duplicate memory`() = runTest {
        // Step 1: User says "My name is Shubh."
        memoryEngine.processNaturalCommand("My name is Shubh.")
        assertEquals(1, fakeDao.stored.size)
        assertEquals("Name: Shubh", fakeDao.stored.first().content)

        // Step 2: Later, user says "My name is Rahul."
        val updateResult = memoryEngine.processNaturalCommand("My name is Rahul.")
        assertTrue(updateResult is MemoryCommandResult.Saved)

        // Verify single source of truth: only 1 memory exists and it is Rahul
        assertEquals(1, fakeDao.stored.size)
        assertEquals("Name: Rahul", fakeDao.stored.first().content)
    }

    @Test
    fun `forget my name removes name from memory and prevents stale memory`() = runTest {
        memoryEngine.processNaturalCommand("My name is Shubh.")
        assertEquals(1, fakeDao.stored.size)

        val forgetResult = memoryEngine.processNaturalCommand("Forget my name.")
        assertTrue(forgetResult is MemoryCommandResult.Forgotten)
        assertEquals(0, fakeDao.stored.size)

        // Subsequent context contains no stale name
        val prompt = memoryEngine.getMemoryContextPrompt()
        assertEquals("", prompt)
    }

    @Test
    fun `persistence across repository recreation simulates app restart`() = runTest {
        memoryEngine.processNaturalCommand("My name is Shubh.")
        assertEquals(1, fakeDao.stored.size)

        // Simulate app restart / ViewModel recreation with new repository and engine pointing to same DAO
        val restartedRepository = UserMemoryRepository(fakeDao)
        val restartedEngine = MemoryEngine(restartedRepository)

        val snapshot = restartedEngine.getMemoriesSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals("Name: Shubh", snapshot.first().content)

        val contextPrompt = restartedEngine.getMemoryContextPrompt()
        assertTrue(contextPrompt.contains("USER MEMORY:\nName: Shubh"))
        assertTrue(contextPrompt.contains("Information in LOCAL USER MEMORY represents facts explicitly stored by the user"))
    }
}

class FakeUserMemoryDao : UserMemoryDao {
    val stored = mutableListOf<UserMemoryEntity>()
    private val flow = MutableStateFlow<List<UserMemoryEntity>>(emptyList())

    override fun getAllMemories(): Flow<List<UserMemoryEntity>> = flow

    override fun getMemoriesByCategory(category: String): Flow<List<UserMemoryEntity>> =
        MutableStateFlow(stored.filter { it.category == category })

    override fun searchMemories(query: String): Flow<List<UserMemoryEntity>> =
        MutableStateFlow(stored.filter { it.content.contains(query, ignoreCase = true) })

    override suspend fun getMemoriesSnapshot(): List<UserMemoryEntity> = stored.toList()

    override suspend fun insertMemory(memory: UserMemoryEntity) {
        stored.add(memory)
        flow.value = stored.toList()
    }

    override suspend fun insertMemories(memories: List<UserMemoryEntity>) {
        stored.addAll(memories)
        flow.value = stored.toList()
    }

    override suspend fun updateMemory(memory: UserMemoryEntity) {
        val index = stored.indexOfFirst { it.id == memory.id }
        if (index != -1) {
            stored[index] = memory
            flow.value = stored.toList()
        }
    }

    override suspend fun deleteMemoryById(id: String) {
        stored.removeAll { it.id == id }
        flow.value = stored.toList()
    }

    override suspend fun deleteMemoriesByKeyword(keyword: String): Int {
        val countBefore = stored.size
        stored.removeAll { it.content.contains(keyword, ignoreCase = true) }
        flow.value = stored.toList()
        return countBefore - stored.size
    }

    override suspend fun clearAll() {
        stored.clear()
        flow.value = emptyList()
    }
}
