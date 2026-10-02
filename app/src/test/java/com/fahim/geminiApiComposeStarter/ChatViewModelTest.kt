package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.PreferencesRepository
import com.fahim.geminiApiComposeStarter.data.local.UserPreferences
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatDao
    private lateinit var fakePreferences: FakePreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatDao()
        fakePreferences = FakePreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            repository = fakeRepository,
            chatDao = fakeDao,
            preferencesRepository = fakePreferences,
            hasApiKey = hasApiKey
        )
    }

    @Test
    fun `initial state collects messages and preferences`() = runTest(testDispatcher) {
        val existingMessages = listOf(
            ChatMessageEntity(id = 1, text = "Hello", isUser = true),
            ChatMessageEntity(id = 2, text = "Hi there!", isUser = false)
        )
        fakeDao.messagesFlow.value = existingMessages
        fakePreferences.preferencesFlow.value = UserPreferences(
            userName = "Alice",
            systemPrompt = "Custom instructions"
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
        assertEquals("Alice", state.userName)
        assertEquals("Custom instructions", state.systemPrompt)
        assertTrue(state.hasApiKey)
    }

    @Test
    fun `onPromptChange updates prompt and clears prompt error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("What is Compose?")
        assertEquals("What is Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend with blank prompt sets promptError to EMPTY`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onSend when missing API key sets error message`() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Test question")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onSend success stores user message and assistant reply in Room`() = runTest(testDispatcher) {
        fakeRepository.resultToReturn = Result.success("Gemini answer")
        val viewModel = createViewModel(hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        // Prompt input should be cleared immediately
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        // User message and Assistant response should both be persisted in DAO
        val messages = fakeDao.messagesFlow.value
        assertEquals(2, messages.size)
        assertEquals("Hello Gemini", messages[0].text)
        assertTrue(messages[0].isUser)
        assertEquals("Gemini answer", messages[1].text)
        assertFalse(messages[1].isUser)

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend failure sets errorMessage and resets loading`() = runTest(testDispatcher) {
        fakeRepository.resultToReturn = Result.failure(RuntimeException("Network quota exceeded"))
        val viewModel = createViewModel(hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Question")
        viewModel.onSend()

        advanceUntilIdle()

        assertEquals("Network quota exceeded", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onClearChat clears all messages in Room`() = runTest(testDispatcher) {
        fakeDao.messagesFlow.value = listOf(
            ChatMessageEntity(id = 1, text = "Message 1", isUser = true)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(fakeDao.messagesFlow.value.isEmpty())
    }

    @Test
    fun `onDeleteMessage removes specific message from Room`() = runTest(testDispatcher) {
        fakeDao.messagesFlow.value = listOf(
            ChatMessageEntity(id = 1, text = "First", isUser = true),
            ChatMessageEntity(id = 2, text = "Second", isUser = false)
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onDeleteMessage(1)
        advanceUntilIdle()

        assertEquals(1, fakeDao.messagesFlow.value.size)
        assertEquals(2L, fakeDao.messagesFlow.value[0].id)
    }

    @Test
    fun `onVoiceInputResult sets prompt or appends to existing text`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceInputResult("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)

        viewModel.onVoiceInputResult("world")
        assertEquals("Hello world", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onUpdateUserName and onUpdateSystemPrompt update preferences`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUpdateUserName("Bob")
        viewModel.onUpdateSystemPrompt("Act like a pirate")
        advanceUntilIdle()

        assertEquals("Bob", fakePreferences.preferencesFlow.value.userName)
        assertEquals("Act like a pirate", fakePreferences.preferencesFlow.value.systemPrompt)
    }
}

// ======================== Test Fakes ========================

class FakeGeminiRepository : GeminiRepository {
    var resultToReturn: Result<String> = Result.success("Default test response")
    var lastPromptSent: String? = null
    var lastSystemInstructionSent: String? = null

    override suspend fun generateText(prompt: String, systemInstruction: String?): Result<String> {
        lastPromptSent = prompt
        lastSystemInstructionSent = systemInstruction
        return resultToReturn
    }
}

class FakeChatDao : ChatDao {
    val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        val id = if (message.id == 0L) nextId++ else message.id
        val newMessage = message.copy(id = id)
        messagesFlow.value = messagesFlow.value + newMessage
        return id
    }

    override suspend fun clearMessages(): Int {
        val count = messagesFlow.value.size
        messagesFlow.value = emptyList()
        return count
    }

    override suspend fun deleteMessage(id: Long): Int {
        val before = messagesFlow.value.size
        messagesFlow.value = messagesFlow.value.filter { it.id != id }
        return before - messagesFlow.value.size
    }
}

class FakePreferencesRepository : PreferencesRepository {
    override val preferencesFlow = MutableStateFlow(UserPreferences())

    override suspend fun updateUserName(name: String) {
        preferencesFlow.value = preferencesFlow.value.copy(userName = name)
    }

    override suspend fun updateSystemPrompt(prompt: String) {
        preferencesFlow.value = preferencesFlow.value.copy(systemPrompt = prompt)
    }
}
