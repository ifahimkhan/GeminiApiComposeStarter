package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var fakeChatHistoryRepository: FakeChatHistoryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGeminiRepository = FakeGeminiRepository()
        fakeChatHistoryRepository = FakeChatHistoryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoresPersistedMessagesOnStartWithoutDuplication() = runTest(testDispatcher) {
        val initialMessages = listOf(
            ChatMessage(id = "1", text = "Hello", role = ChatRole.USER, timestamp = 1000L),
            ChatMessage(id = "2", text = "Hi there!", role = ChatRole.ASSISTANT, timestamp = 2000L),
        )
        fakeChatHistoryRepository.setMessages(initialMessages)

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("1", state.messages[0].id)
        assertEquals("Hello", state.messages[0].text)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("2", state.messages[1].id)
        assertEquals("Hi there!", state.messages[1].text)
        assertEquals(ChatRole.ASSISTANT, state.messages[1].role)
    }

    @Test
    fun onSendPersistsUserAndAssistantMessages() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.success("I am Gemini.")

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("What is your name?")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("What is your name?", state.messages[0].text)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("I am Gemini.", state.messages[1].text)
        assertEquals(ChatRole.ASSISTANT, state.messages[1].role)

        // Verify messages persisted in repository
        val persisted = fakeChatHistoryRepository.currentMessages
        assertEquals(2, persisted.size)
        assertEquals("What is your name?", persisted[0].text)
        assertEquals("I am Gemini.", persisted[1].text)
    }

    @Test
    fun onSendDoesNotPersistFailedResponse() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.failure(RuntimeException("Network error"))

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Test query")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals("Test query", state.messages[0].text)

        // Only the user message is persisted; the failed response is NOT persisted
        val persisted = fakeChatHistoryRepository.currentMessages
        assertEquals(1, persisted.size)
        assertEquals(ChatRole.USER, persisted[0].role)
    }

    @Test
    fun clearConversationDeletesPersistedMessagesAndClearsUi() = runTest(testDispatcher) {
        val initialMessages = listOf(
            ChatMessage(id = "1", text = "Hello", role = ChatRole.USER),
            ChatMessage(id = "2", text = "Hi!", role = ChatRole.ASSISTANT),
        )
        fakeChatHistoryRepository.setMessages(initialMessages)

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearConversation()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.messages.size)
        assertEquals(0, fakeChatHistoryRepository.currentMessages.size)
    }

    @Test
    fun toggleDarkThemeUpdatesPreferenceAndUi() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDarkTheme)

        viewModel.toggleDarkTheme()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDarkTheme)
        assertTrue(fakeUserPreferencesRepository.savedDarkTheme)

        viewModel.toggleDarkTheme()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDarkTheme)
        assertFalse(fakeUserPreferencesRepository.savedDarkTheme)
    }

    @Test
    fun voiceInputOnPromptChangeDoesNotSendImmediately() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        // Simulating voice recognition result populating the prompt
        viewModel.onPromptChange("Voice recognized text")

        val state = viewModel.uiState.value
        assertEquals("Voice recognized text", state.prompt)
        assertEquals(0, state.messages.size)
        assertFalse(state.isLoading)
        assertEquals(0, fakeChatHistoryRepository.currentMessages.size)
    }

    private class FakeGeminiRepository : GeminiRepository {
        var responseResult: Result<String> = Result.success("Default response")

        override suspend fun generateText(prompt: String): Result<String> {
            return responseResult
        }
    }

    private class FakeChatHistoryRepository : ChatHistoryRepository {
        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        val currentMessages: List<ChatMessage> get() = _messages.value

        fun setMessages(messages: List<ChatMessage>) {
            _messages.value = messages
        }

        override fun getMessages(): Flow<List<ChatMessage>> = _messages.asStateFlow()

        override suspend fun saveMessage(message: ChatMessage) {
            val existing = _messages.value.toMutableList()
            val index = existing.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                existing[index] = message
            } else {
                existing.add(message)
            }
            _messages.value = existing
        }

        override suspend fun clearHistory() {
            _messages.value = emptyList()
        }
    }

    private class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isDarkTheme = MutableStateFlow(false)
        var savedDarkTheme: Boolean = false

        override val isDarkTheme: Flow<Boolean> = _isDarkTheme.asStateFlow()

        override suspend fun setDarkTheme(isDark: Boolean) {
            savedDarkTheme = isDark
            _isDarkTheme.value = isDark
        }
    }
}
