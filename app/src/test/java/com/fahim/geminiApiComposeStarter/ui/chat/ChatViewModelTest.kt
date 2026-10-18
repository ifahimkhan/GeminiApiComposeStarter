package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.data.repository.ChatHistoryRepository
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
    private lateinit var fakeHistoryRepository: FakeChatHistoryRepository
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGeminiRepository = FakeGeminiRepository()
        fakeHistoryRepository = FakeChatHistoryRepository()
        fakePreferencesRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeHistoryRepository,
            userPreferencesRepository = fakePreferencesRepository,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun initialState_loadsMessagesAndPreferences() = runTest(testDispatcher) {
        fakeHistoryRepository.saveMessage("Existing greeting", isFromUser = true)
        fakePreferencesRepository.setDarkMode(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertEquals(1, state.messages.size)
        assertEquals("Existing greeting", state.messages.first().text)
        assertEquals(true, state.isDarkMode)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSend() // Empty prompt triggers error
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptErrorEmpty_doesNotCallGemini() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, fakeGeminiRepository.callCount)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage_doesNotCallGemini() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, fakeGeminiRepository.callCount)
    }

    @Test
    fun onSend_success_persistsUserAndGeminiMessages_clearsPromptAndStopsLoading() =
        runTest(testDispatcher) {
            fakeGeminiRepository.stubResponse = Result.success("I am Gemini, an AI assistant.")

            val viewModel = createViewModel(hasApiKey = true)
            advanceUntilIdle()

            viewModel.onPromptChange("Tell me who you are")
            viewModel.onSend()

            // Input is cleared immediately when sending
            assertEquals("", viewModel.uiState.value.prompt)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.errorMessage)
            assertEquals(1, fakeGeminiRepository.callCount)
            assertEquals("Tell me who you are", fakeGeminiRepository.lastPrompt)

            // Conversation history should have 2 messages: user prompt and model response
            assertEquals(2, state.messages.size)
            assertEquals("Tell me who you are", state.messages[0].text)
            assertTrue(state.messages[0].isFromUser)
            assertEquals("I am Gemini, an AI assistant.", state.messages[1].text)
            assertFalse(state.messages[1].isFromUser)
        }

    @Test
    fun onSend_failure_setsErrorMessage_stopsLoading() = runTest(testDispatcher) {
        fakeGeminiRepository.stubResponse = Result.failure(RuntimeException("Network request failed"))

        val viewModel = createViewModel(hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Generate error")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network request failed", state.errorMessage)
        assertEquals(1, fakeGeminiRepository.callCount)
        // User message was saved before the error
        assertEquals(1, state.messages.size)
        assertEquals("Generate error", state.messages[0].text)
    }

    @Test
    fun onVoiceInput_insertsOrAppendsSpokenText() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onVoiceInput("First sentence.")
        assertEquals("First sentence.", viewModel.uiState.value.prompt)

        viewModel.onVoiceInput("Second sentence.")
        assertEquals("First sentence. Second sentence.", viewModel.uiState.value.prompt)
    }

    @Test
    fun onToggleTheme_updatesPreferences() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onToggleTheme()
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isDarkMode)

        viewModel.onToggleTheme()
        advanceUntilIdle()
        assertEquals(false, viewModel.uiState.value.isDarkMode)
    }

    @Test
    fun onClearHistory_clearsSavedMessages() = runTest(testDispatcher) {
        fakeHistoryRepository.saveMessage("Message 1", true)
        fakeHistoryRepository.saveMessage("Message 2", false)

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeGeminiRepository.stubResponse = Result.failure(Exception("Error message"))
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Test")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals("Error message", viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // --- Fake implementations for isolated unit testing ---

    class FakeGeminiRepository : GeminiRepository {
        var stubResponse: Result<String> = Result.success("Default stub response")
        var callCount = 0
        var lastPrompt: String? = null

        override suspend fun generateText(prompt: String): Result<String> {
            callCount++
            lastPrompt = prompt
            return stubResponse
        }
    }

    class FakeChatHistoryRepository : ChatHistoryRepository {
        private val messageList = mutableListOf<ChatMessage>()
        private val _flow = MutableStateFlow<List<ChatMessage>>(emptyList())
        private var nextId = 1L

        override fun getMessages(): Flow<List<ChatMessage>> = _flow.asStateFlow()

        override suspend fun saveMessage(text: String, isFromUser: Boolean): Long {
            val id = nextId++
            messageList.add(ChatMessage(id = id, text = text, isFromUser = isFromUser))
            _flow.value = messageList.toList()
            return id
        }

        override suspend fun clearHistory() {
            messageList.clear()
            _flow.value = emptyList()
        }
    }

    class FakeUserPreferencesRepository : UserPreferencesRepository {
        private val _isDarkMode = MutableStateFlow<Boolean?>(null)
        override val isDarkMode: Flow<Boolean?> = _isDarkMode.asStateFlow()

        override suspend fun setDarkMode(isDark: Boolean?) {
            _isDarkMode.value = isDark
        }

        override suspend fun toggleDarkMode() {
            val current = _isDarkMode.value
            _isDarkMode.value = when (current) {
                true -> false
                false -> true
                null -> true
            }
        }
    }
}
