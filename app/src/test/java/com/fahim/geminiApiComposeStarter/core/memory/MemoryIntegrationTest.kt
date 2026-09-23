package com.fahim.geminiApiComposeStarter.core.memory

import com.fahim.geminiApiComposeStarter.core.ai.AICore
import com.fahim.geminiApiComposeStarter.core.ai.AICoreResponse
import com.fahim.geminiApiComposeStarter.core.ai.IntentRouter
import com.fahim.geminiApiComposeStarter.core.ai.TaskPlanner
import com.fahim.geminiApiComposeStarter.core.context.ContextEngine
import com.fahim.geminiApiComposeStarter.core.tools.ToolRegistry
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ConversationDao
import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity
import com.fahim.geminiApiComposeStarter.data.local.MemoryCategory
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryDao
import com.fahim.geminiApiComposeStarter.data.local.UserMemoryEntity
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.repository.UserMemoryRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.StudyMode
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var memoryDao: TestMemoryDao
    private lateinit var messageDao: TestChatMessageDao
    private lateinit var conversationDao: TestConversationDao
    private lateinit var memoryRepository: UserMemoryRepository
    private lateinit var chatHistoryRepository: ChatHistoryRepository
    private lateinit var memoryEngine: MemoryEngine
    private lateinit var contextEngine: ContextEngine
    private lateinit var fakeGeminiRepo: InspectableGeminiRepository
    private lateinit var aiCore: AICore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        memoryDao = TestMemoryDao()
        messageDao = TestChatMessageDao()
        conversationDao = TestConversationDao()
        memoryRepository = UserMemoryRepository(memoryDao)
        chatHistoryRepository = ChatHistoryRepository(messageDao, conversationDao)
        memoryEngine = MemoryEngine(memoryRepository)
        contextEngine = ContextEngine()
        fakeGeminiRepo = InspectableGeminiRepository()
        aiCore = AICore(
            intentRouter = IntentRouter(),
            memoryEngine = memoryEngine,
            taskPlanner = TaskPlanner(),
            toolRegistry = ToolRegistry(),
            contextEngine = contextEngine,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test exact scenario - name stored, app restart, new session, what is my name returns name`() = runTest {
        // Step 1: User says "My name is Shubh." in Session A
        val sessionAId = "session_a"
        val step1Result = aiCore.execute(
            prompt = "My name is Shubh.",
            history = emptyList(),
            mode = StudyMode.CHAT,
            geminiRepository = fakeGeminiRepo,
        )
        assertTrue(step1Result is AICoreResponse.Direct)

        // Step 2: Verify memory is actually inserted into Room
        val memoriesAfterStep1 = memoryDao.getMemoriesSnapshot()
        assertEquals(1, memoriesAfterStep1.size)
        assertEquals("Name: Shubh", memoriesAfterStep1.first().content)
        assertEquals(MemoryCategory.PROFILE.name, memoriesAfterStep1.first().category)

        // Step 3: Simulate kill/restart of the app (new repository and AICore instances with same database)
        val restartedMemoryRepo = UserMemoryRepository(memoryDao)
        val restartedMemoryEngine = MemoryEngine(restartedMemoryRepo)
        val restartedAICore = AICore(
            intentRouter = IntentRouter(),
            memoryEngine = restartedMemoryEngine,
            taskPlanner = TaskPlanner(),
            toolRegistry = ToolRegistry(),
            contextEngine = contextEngine,
        )

        // Step 4: Start a NEW conversation / session (Session B with completely empty history)
        val sessionBHistory = emptyList<ChatMessage>()

        // Configure Gemini fake to reply using whatever is in systemInstruction
        fakeGeminiRepo.onGenerateChat = { prompt, _, systemInstruction ->
            if (systemInstruction?.contains("Name: Shubh") == true) {
                Result.success("Your name is Shubh.")
            } else {
                Result.success("I don't know your name.")
            }
        }

        // Step 5: Ask "What is my name?"
        val step5Result = restartedAICore.execute(
            prompt = "What is my name?",
            history = sessionBHistory,
            mode = StudyMode.CHAT,
            geminiRepository = fakeGeminiRepo,
        )

        // Inspect constructed context before Gemini request was executed
        assertNotNull(fakeGeminiRepo.lastSystemInstruction)
        val systemInstruction = fakeGeminiRepo.lastSystemInstruction!!

        // Context MUST contain stored memory equivalent to "USER MEMORY:\nName: Shubh"
        assertTrue("Context must contain USER MEMORY header", systemInstruction.contains("USER MEMORY:"))
        assertTrue("Context must contain Name: Shubh", systemInstruction.contains("Name: Shubh"))
        assertTrue(
            "Must contain mandatory system instruction",
            systemInstruction.contains("Information in LOCAL USER MEMORY represents facts explicitly stored by the user")
        )

        // Application reliably answers "Your name is Shubh."
        assertTrue(step5Result is AICoreResponse.Success)
        assertEquals("Your name is Shubh.", (step5Result as AICoreResponse.Success).message)
    }

    @Test
    fun `test memory operations - create, update, delete, clear, and stale memory prevention`() = runTest {
        // 1. "My name is Shubh." -> Name = Shubh
        memoryEngine.processNaturalCommand("My name is Shubh.")
        var snapshot = memoryRepository.getMemoriesSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals("Name: Shubh", snapshot.first().content)

        // 2. "What is my name?" -> Shubh in context
        var context = contextEngine.buildContext(emptyList(), "What is my name?", StudyMode.CHAT, userMemories = snapshot)
        assertTrue(context.systemInstruction.contains("Name: Shubh"))

        // 3. "My name is Rahul." -> Name updated to Rahul (no duplicate/stale memory)
        val updateResult = memoryEngine.processNaturalCommand("My name is Rahul.")
        assertTrue(updateResult is MemoryCommandResult.Saved)
        snapshot = memoryRepository.getMemoriesSnapshot()
        assertEquals("Single memory record must remain", 1, snapshot.size)
        assertEquals("Name must be updated to Rahul", "Name: Rahul", snapshot.first().content)

        // 4. "What is my name?" -> Rahul in context
        context = contextEngine.buildContext(emptyList(), "What is my name?", StudyMode.CHAT, userMemories = snapshot)
        assertTrue(context.systemInstruction.contains("Name: Rahul"))
        assertFalse("Stale name Shubh must NOT be in context", context.systemInstruction.contains("Shubh"))

        // 5. "Forget my name." -> Name removed
        val forgetResult = memoryEngine.processNaturalCommand("Forget my name.")
        assertTrue(forgetResult is MemoryCommandResult.Forgotten)
        snapshot = memoryRepository.getMemoriesSnapshot()
        assertEquals("Memory database must have 0 names", 0, snapshot.size)

        // 6. "What is my name?" -> Must NOT return Shubh/Rahul from stale memory
        context = contextEngine.buildContext(emptyList(), "What is my name?", StudyMode.CHAT, userMemories = snapshot)
        assertFalse(context.systemInstruction.contains("USER MEMORY:"))
        assertFalse(context.systemInstruction.contains("Rahul"))
        assertFalse(context.systemInstruction.contains("Shubh"))

        // 7. "Remember that I prefer concise answers." -> preference stored
        val prefResult = memoryEngine.processNaturalCommand("Remember that I prefer concise answers.")
        assertTrue(prefResult is MemoryCommandResult.Saved)
        snapshot = memoryRepository.getMemoriesSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals(MemoryCategory.PREFERENCE.name, snapshot.first().category)

        // 8. "What do you remember about me?" -> actual stored memories returned
        val listResult = memoryEngine.processNaturalCommand("What do you remember about me?")
        assertTrue(listResult is MemoryCommandResult.ListAll)
        assertEquals(1, (listResult as MemoryCommandResult.ListAll).memories.size)

        // 9. "Forget everything you remember about me." -> memory database cleared
        val clearResult = memoryEngine.processNaturalCommand("Forget everything you remember about me.")
        assertTrue(clearResult is MemoryCommandResult.ClearedAll)
        snapshot = memoryRepository.getMemoriesSnapshot()
        assertEquals(0, snapshot.size)
    }

    @Test
    fun `test memory vs chat history separation`() = runTest {
        // Save global user memory
        memoryRepository.saveName("Shubh")
        assertEquals(1, memoryDao.getMemoriesSnapshot().size)

        // Create Session A and add messages
        val sessionA = chatHistoryRepository.createConversation("Session A")
        chatHistoryRepository.insertMessage(
            ChatMessage(conversationId = sessionA.id, role = MessageRole.USER, content = "Question 1")
        )
        chatHistoryRepository.insertMessage(
            ChatMessage(conversationId = sessionA.id, role = MessageRole.GEMINI, content = "Answer 1")
        )

        // Create Session B
        val sessionB = chatHistoryRepository.createConversation("Session B")

        // Deleting Session A or clearing chat messages must NOT delete global user memory
        chatHistoryRepository.deleteConversation(sessionA.id)

        // Messages in Session A are gone
        val sessionAMessages = messageDao.storedMessages.filter { it.conversationId == sessionA.id }
        assertEquals(0, sessionAMessages.size)

        // Global User Memory is completely intact
        val memories = memoryDao.getMemoriesSnapshot()
        assertEquals(1, memories.size)
        assertEquals("Name: Shubh", memories.first().content)
    }
}

// ── Test Fakes ─────────────────────────────────────────────────────────────────

class InspectableGeminiRepository : GeminiRepository {
    var lastPrompt: String? = null
    var lastSystemInstruction: String? = null
    var lastHistory: List<ChatMessage> = emptyList()

    var onGenerateChat: ((prompt: String, history: List<ChatMessage>, systemInstruction: String?) -> Result<String>)? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return Result.success("Generated Text")
    }

    override suspend fun generateChat(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String?,
    ): Result<String> {
        lastPrompt = prompt
        lastHistory = history
        lastSystemInstruction = systemInstruction
        return onGenerateChat?.invoke(prompt, history, systemInstruction)
            ?: Result.success("Default Chat Response")
    }

    override suspend fun generateMultimodal(
        prompt: String,
        imageBitmap: android.graphics.Bitmap,
        systemInstruction: String?,
    ): Result<String> {
        lastPrompt = prompt
        lastSystemInstruction = systemInstruction
        return Result.success("Multimodal Response")
    }
}

class TestMemoryDao : UserMemoryDao {
    val stored = mutableListOf<UserMemoryEntity>()
    private val flow = MutableStateFlow<List<UserMemoryEntity>>(emptyList())

    override fun getAllMemories(): Flow<List<UserMemoryEntity>> = flow

    override fun getMemoriesByCategory(category: String): Flow<List<UserMemoryEntity>> =
        MutableStateFlow(stored.filter { it.category == category })

    override fun searchMemories(query: String): Flow<List<UserMemoryEntity>> =
        MutableStateFlow(stored.filter { it.content.contains(query, ignoreCase = true) })

    override suspend fun getMemoriesSnapshot(): List<UserMemoryEntity> = stored.toList()

    override suspend fun insertMemory(memory: UserMemoryEntity) {
        stored.removeAll { it.id == memory.id }
        stored.add(memory)
        flow.value = stored.toList()
    }

    override suspend fun insertMemories(memories: List<UserMemoryEntity>) {
        memories.forEach { insertMemory(it) }
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

class TestChatMessageDao : ChatMessageDao {
    val storedMessages = mutableListOf<ChatMessageEntity>()
    private val flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow

    override fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> =
        MutableStateFlow(storedMessages.filter { it.conversationId == conversationId })

    override fun getSavedMessages(): Flow<List<ChatMessageEntity>> =
        MutableStateFlow(storedMessages.filter { it.isSaved })

    override suspend fun insertMessage(message: ChatMessageEntity) {
        storedMessages.add(message)
        flow.value = storedMessages.toList()
    }

    override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        storedMessages.addAll(messages)
        flow.value = storedMessages.toList()
    }

    override suspend fun updateSavedStatus(messageId: String, isSaved: Boolean) {
        val index = storedMessages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            storedMessages[index] = storedMessages[index].copy(isSaved = isSaved)
            flow.value = storedMessages.toList()
        }
    }

    override suspend fun clearConversation(conversationId: String) {
        storedMessages.removeAll { it.conversationId == conversationId }
        flow.value = storedMessages.toList()
    }

    override suspend fun clearAll() {
        storedMessages.clear()
        flow.value = emptyList()
    }
}

class TestConversationDao : ConversationDao {
    val storedConversations = mutableListOf<ConversationEntity>()
    private val flow = MutableStateFlow<List<ConversationEntity>>(emptyList())

    override fun getAllConversations(): Flow<List<ConversationEntity>> = flow

    override suspend fun getConversationById(id: String): ConversationEntity? =
        storedConversations.find { it.id == id }

    override suspend fun insertConversation(conversation: ConversationEntity) {
        storedConversations.add(conversation)
        flow.value = storedConversations.toList()
    }

    override suspend fun updateTitle(id: String, newTitle: String, updatedAt: Long) {
        val index = storedConversations.indexOfFirst { it.id == id }
        if (index != -1) {
            storedConversations[index] = storedConversations[index].copy(title = newTitle, updatedAt = updatedAt)
            flow.value = storedConversations.toList()
        }
    }

    override suspend fun updateTimestamp(id: String, updatedAt: Long) {
        val index = storedConversations.indexOfFirst { it.id == id }
        if (index != -1) {
            storedConversations[index] = storedConversations[index].copy(updatedAt = updatedAt)
            flow.value = storedConversations.toList()
        }
    }

    override suspend fun deleteConversation(id: String) {
        storedConversations.removeAll { it.id == id }
        flow.value = storedConversations.toList()
    }
}
