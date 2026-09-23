package com.fahim.geminiApiComposeStarter.ui.chat

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ConversationDao
import com.fahim.geminiApiComposeStarter.data.local.ConversationEntity
import com.fahim.geminiApiComposeStarter.data.local.QuizAttemptDao
import com.fahim.geminiApiComposeStarter.data.local.QuizAttemptEntity
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.fahim.geminiApiComposeStarter.model.StudyMode
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeMessageDao: FakeChatMessageDao
    private lateinit var fakeConversationDao: FakeConversationDao
    private lateinit var fakeQuizDao: FakeQuizAttemptDao
    private lateinit var chatHistoryRepository: ChatHistoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeMessageDao = FakeChatMessageDao()
        fakeConversationDao = FakeConversationDao()
        fakeQuizDao = FakeQuizAttemptDao()
        chatHistoryRepository = ChatHistoryRepository(
            messageDao = fakeMessageDao,
            conversationDao = fakeConversationDao,
            quizDao = fakeQuizDao,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            repository = fakeRepository,
            chatHistoryRepository = chatHistoryRepository,
            hasApiKey = hasApiKey,
        )
    }

    // ── Tier 1 Core Tests (Preserved) ────────────────────────────────────────

    @Test
    fun `initial state has empty messages and no loading`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
        assertEquals("", state.prompt)
        assertEquals(StudyMode.CHAT, state.activeMode)
    }

    @Test
    fun `onPromptChange updates prompt text`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets EMPTY error`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `onSend without API key sets missing key error`() = runTest {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend adds user message and sets loading`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertTrue(state.messages.any { it.role == MessageRole.USER && it.content == "Hello" })
        assertTrue(state.isLoading)
        assertEquals("", state.prompt)
    }

    @Test
    fun `successful Gemini response adds gemini message`() = runTest {
        fakeRepository.nextResult = Result.success("Hello from Gemini!")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("Hello", state.messages[0].content)
        assertEquals(MessageRole.GEMINI, state.messages[1].role)
        assertEquals("Hello from Gemini!", state.messages[1].content)
    }

    @Test
    fun `Gemini error sets error message and clears loading`() = runTest {
        fakeRepository.nextResult = Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
    }

    @Test
    fun `onDismissError clears error message`() = runTest {
        fakeRepository.nextResult = Result.failure(RuntimeException("Error"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onDismissError()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onClearHistory removes all messages`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isNotEmpty())

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `messages persist to dao`() = runTest {
        fakeRepository.nextResult = Result.success("Response")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, fakeMessageDao.storedMessages.size)
        assertEquals("USER", fakeMessageDao.storedMessages[0].role)
        assertEquals("GEMINI", fakeMessageDao.storedMessages[1].role)
    }

    @Test
    fun `history is restored from dao on init`() = runTest {
        fakeMessageDao.storedMessages.add(
            ChatMessageEntity(
                id = "pre-1",
                conversationId = "default",
                role = "USER",
                content = "Old message",
                timestamp = 1000L,
            )
        )
        fakeMessageDao.emitUpdate()

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Old message", state.messages[0].content)
    }

    // ── Tier 3 AI Study Workspace Tests ──────────────────────────────────────

    @Test
    fun `setMode updates active study mode`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setMode(StudyMode.EXPLAIN)
        assertEquals(StudyMode.EXPLAIN, viewModel.uiState.value.activeMode)

        viewModel.setMode(StudyMode.QUIZ)
        assertEquals(StudyMode.QUIZ, viewModel.uiState.value.activeMode)
    }

    @Test
    fun `quick action populates prompt and triggers send`() = runTest {
        fakeRepository.nextResult = Result.success("Socratic explanation")
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onQuickAction("Teach me Binary Search Trees")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.any { it.content == "Teach me Binary Search Trees" })
        assertEquals(2, state.messages.size)
    }

    @Test
    fun `toggleSaveNote persists bookmark state in dao`() = runTest {
        val testMessage = ChatMessage(
            id = "test-msg-1",
            conversationId = "default",
            role = MessageRole.GEMINI,
            content = "Valuable formula",
            isSaved = false,
        )
        fakeMessageDao.storedMessages.add(
            ChatMessageEntity(
                id = testMessage.id,
                conversationId = "default",
                role = "GEMINI",
                content = testMessage.content,
                timestamp = testMessage.timestamp,
                isSaved = false,
            )
        )
        fakeMessageDao.emitUpdate()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleSaveNote(testMessage)
        advanceUntilIdle()

        assertTrue(fakeMessageDao.storedMessages.first { it.id == "test-msg-1" }.isSaved)
    }

    @Test
    fun `createNewSession creates conversation in dao and switches to it`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.createNewSession(StudyMode.QUIZ)
        advanceUntilIdle()

        val conversations = fakeConversationDao.storedConversations
        assertEquals(1, conversations.size)
        assertEquals(StudyMode.QUIZ.name, conversations[0].mode)
        assertEquals(conversations[0].id, viewModel.uiState.value.currentConversationId)
    }

    @Test
    fun `deleteSession removes conversation from dao and switches to default`() = runTest {
        fakeConversationDao.storedConversations.add(
            ConversationEntity(
                id = "custom-session",
                title = "DBMS Prep",
                createdAt = 1000L,
                updatedAt = 1000L,
                mode = "STUDY",
            )
        )
        fakeConversationDao.emitUpdate()

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadConversationMessages("custom-session")
        advanceUntilIdle()
        assertEquals("custom-session", viewModel.uiState.value.currentConversationId)

        viewModel.deleteSession("custom-session")
        advanceUntilIdle()

        assertTrue(fakeConversationDao.storedConversations.none { it.id == "custom-session" })
        assertEquals("default", viewModel.uiState.value.currentConversationId)
    }

    @Test
    fun `onRecordQuizScore persists quiz attempt in dao`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRecordQuizScore(
            topic = "OS Scheduling",
            score = 4,
            total = 5,
            strong = "Round Robin, FIFO",
            revision = "Multi-level Feedback Queues",
        )
        advanceUntilIdle()

        val attempts = fakeQuizDao.storedAttempts
        assertEquals(1, attempts.size)
        assertEquals("OS Scheduling", attempts[0].topic)
        assertEquals(4, attempts[0].score)
        assertEquals(80, attempts[0].percentage)
    }

    @Test
    fun `multi-turn conversation passes recent history to repository`() = runTest {
        fakeRepository.nextResult = Result.success("Nice to meet you Shubh!")
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Turn 1
        viewModel.onPromptChange("My name is Shubh")
        viewModel.onSend()
        advanceUntilIdle()

        // Turn 2
        fakeRepository.nextResult = Result.success("Your name is Shubh")
        viewModel.onPromptChange("What is my name?")
        viewModel.onSend()
        advanceUntilIdle()

        // Verify repository received history
        assertTrue(fakeRepository.lastChatHistory.any { it.content == "My name is Shubh" })
    }
}

// ── Fakes ────────────────────────────────────────────────────────────────────

class FakeGeminiRepository : GeminiRepository {
    var nextResult: Result<String> = Result.success("")
    var lastChatHistory: List<ChatMessage> = emptyList()
    var lastPrompt: String = ""

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return nextResult
    }

    override suspend fun generateChat(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String?,
    ): Result<String> {
        lastPrompt = prompt
        lastChatHistory = history
        return nextResult
    }

    override suspend fun generateMultimodal(
        prompt: String,
        imageBitmap: Bitmap,
        systemInstruction: String?,
    ): Result<String> {
        lastPrompt = prompt
        return nextResult
    }
}

class FakeChatMessageDao : ChatMessageDao {
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
            val old = storedMessages[index]
            storedMessages[index] = old.copy(isSaved = isSaved)
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

    fun emitUpdate() {
        flow.value = storedMessages.toList()
    }
}

class FakeConversationDao : ConversationDao {
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
            val old = storedConversations[index]
            storedConversations[index] = old.copy(title = newTitle, updatedAt = updatedAt)
            flow.value = storedConversations.toList()
        }
    }

    override suspend fun updateTimestamp(id: String, updatedAt: Long) {
        val index = storedConversations.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = storedConversations[index]
            storedConversations[index] = old.copy(updatedAt = updatedAt)
            flow.value = storedConversations.toList()
        }
    }

    override suspend fun deleteConversation(id: String) {
        storedConversations.removeAll { it.id == id }
        flow.value = storedConversations.toList()
    }

    fun emitUpdate() {
        flow.value = storedConversations.toList()
    }
}

class FakeQuizAttemptDao : QuizAttemptDao {
    val storedAttempts = mutableListOf<QuizAttemptEntity>()
    private val flow = MutableStateFlow<List<QuizAttemptEntity>>(emptyList())

    override fun getAllQuizAttempts(): Flow<List<QuizAttemptEntity>> = flow

    override suspend fun insertQuizAttempt(attempt: QuizAttemptEntity) {
        storedAttempts.add(attempt)
        flow.value = storedAttempts.toList()
    }

    override suspend fun clearAllQuizAttempts() {
        storedAttempts.clear()
        flow.value = emptyList()
    }
}
