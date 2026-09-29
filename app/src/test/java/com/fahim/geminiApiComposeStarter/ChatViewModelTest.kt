package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
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
    fun emptyPromptIsRejected() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
        assertEquals(0, state.messages.size)
        assertFalse(state.isLoading)
        assertEquals(0, fakeChatHistoryRepository.currentMessages.size)
    }

    @Test
    fun blankOrWhitespacePromptIsRejected() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("   \n\t  ")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
        assertEquals(0, state.messages.size)
        assertFalse(state.isLoading)
        assertEquals(0, fakeChatHistoryRepository.currentMessages.size)
    }

    @Test
    fun validPromptAddsUserMessageAndClearsPrompt() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.success("Response")

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hello assistant")
        viewModel.onSend()

        // Check immediate UI state (prompt cleared, user message present)
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertNull(state.promptError)
        assertTrue(state.messages.any { it.role == ChatRole.USER && it.text == "Hello assistant" })

        advanceUntilIdle()
    }

    @Test
    fun loadingStateBecomesTrueWhileRequestIsRunningAndFalseAfterSuccess() = runTest(testDispatcher) {
        val deferredResponse = CompletableDeferred<String>()
        val asyncRepository = object : GeminiRepository {
            override suspend fun generateText(prompt: String): Result<String> {
                return Result.success(deferredResponse.await())
            }
        }

        val viewModel = ChatViewModel(
            repository = asyncRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Are you there?")
        viewModel.onSend()

        // State while request is running
        assertTrue("Loading state should be true while request is running", viewModel.uiState.value.isLoading)

        // Complete the async response
        deferredResponse.complete("Yes, I am here!")
        advanceUntilIdle()

        // State after response arrives
        assertFalse("Loading state should be false after success", viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Yes, I am here!", viewModel.uiState.value.messages[1].text)
        assertEquals(ChatRole.ASSISTANT, viewModel.uiState.value.messages[1].role)
    }

    @Test
    fun successfulGeminiResponseAddsAssistantMessage() = runTest(testDispatcher) {
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
    fun repositoryFailureProducesErrorMessageAndResetsLoading() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.failure(RuntimeException("Network request failed"))

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
        assertFalse("Loading state should be false after failure", state.isLoading)
        assertEquals("Network request failed", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals("Test query", state.messages[0].text)

        // Only user message is persisted; failed assistant response is NOT persisted
        val persisted = fakeChatHistoryRepository.currentMessages
        assertEquals(1, persisted.size)
        assertEquals(ChatRole.USER, persisted[0].role)
    }

    @Test
    fun errorDismissalClearsErrorMessage() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.failure(RuntimeException("Temporary error"))

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Query")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("Temporary error", viewModel.uiState.value.errorMessage)

        viewModel.onErrorDismissed()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun multipleConversationTurnsPreserveMessageOrder() = runTest(testDispatcher) {
        var turnCount = 0
        fakeGeminiRepository.customGenerator = { prompt ->
            turnCount++
            "Response $turnCount to $prompt"
        }

        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true,
        )
        advanceUntilIdle()

        // Turn 1
        viewModel.onPromptChange("Turn 1 prompt")
        viewModel.onSend()
        advanceUntilIdle()

        // Turn 2
        viewModel.onPromptChange("Turn 2 prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("Turn 1 prompt", state.messages[0].text)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("Response 1 to Turn 1 prompt", state.messages[1].text)
        assertEquals(ChatRole.ASSISTANT, state.messages[1].role)

        assertEquals("Turn 2 prompt", state.messages[2].text)
        assertEquals(ChatRole.USER, state.messages[2].role)
        assertEquals("Response 2 to Turn 2 prompt", state.messages[3].text)
        assertEquals(ChatRole.ASSISTANT, state.messages[3].role)

        // Verify order in persistent repository
        val persisted = fakeChatHistoryRepository.currentMessages
        assertEquals(4, persisted.size)
        assertEquals("Turn 1 prompt", persisted[0].text)
        assertEquals("Response 1 to Turn 1 prompt", persisted[1].text)
        assertEquals("Turn 2 prompt", persisted[2].text)
        assertEquals("Response 2 to Turn 2 prompt", persisted[3].text)
    }

    @Test
    fun apiKeyUnavailableBehaviorRemainsCorrect() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = false,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hello without key")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, state.errorMessage)
        assertEquals(0, state.messages.size)
        assertFalse(state.isLoading)
        assertEquals(0, fakeChatHistoryRepository.currentMessages.size)
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
        var customGenerator: ((String) -> String)? = null

        override suspend fun generateText(prompt: String): Result<String> {
            val generator = customGenerator
            return if (generator != null) {
                Result.success(generator(prompt))
            } else {
                responseResult
            }
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
