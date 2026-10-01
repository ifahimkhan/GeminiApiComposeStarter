package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fake implementation of GeminiRepository for testing. */
class FakeGeminiRepository : GeminiRepository {
    var shouldFail: Boolean = false
    var failureException: Exception = RuntimeException("Network error")
    var responseText: String = "Hello from Fake Gemini"
    val promptsReceived = mutableListOf<Pair<String, String?>>()

    override suspend fun generateText(prompt: String, modelName: String?): Result<String> {
        promptsReceived.add(Pair(prompt, modelName))
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            Result.success(responseText)
        }
    }
}

/** Fake in-memory implementation of ChatHistoryRepository for testing. */
class FakeChatHistoryRepository(
    initialMessages: List<ChatMessage> = emptyList(),
) : ChatHistoryRepository {
    private val _messages = MutableStateFlow(initialMessages)
    private var nextId = 1L

    override fun getMessages(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun saveUserMessage(text: String): Long {
        val id = nextId++
        val newMsg = ChatMessage(id = id, text = text, isUser = true)
        _messages.value = _messages.value + newMsg
        return id
    }

    override suspend fun saveGeminiMessage(text: String): Long {
        val id = nextId++
        val newMsg = ChatMessage(id = id, text = text, isUser = false)
        _messages.value = _messages.value + newMsg
        return id
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}

/** Fake implementation of UserPreferencesRepository for testing. */
class FakeUserPreferencesRepository(
    initialModel: String = UserPreferencesRepository.DEFAULT_MODEL,
) : UserPreferencesRepository {
    private val _modelFlow = MutableStateFlow(initialModel)
    override val selectedModelFlow: Flow<String> = _modelFlow.asStateFlow()

    override suspend fun setSelectedModel(modelName: String) {
        _modelFlow.value = modelName
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGeminiRepo: FakeGeminiRepository
    private lateinit var fakeHistoryRepo: FakeChatHistoryRepository
    private lateinit var fakePrefsRepo: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGeminiRepo = FakeGeminiRepository()
        fakeHistoryRepo = FakeChatHistoryRepository()
        fakePrefsRepo = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            repository = fakeGeminiRepo,
            historyRepository = fakeHistoryRepo,
            preferencesRepository = fakePrefsRepo,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun initialState_loadsHistoryAndModelPreferences() = runTest(testDispatcher) {
        fakeHistoryRepo.saveUserMessage("Existing message")
        fakePrefsRepo.setSelectedModel("gemini-3.8-flash")

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Existing message", state.messages.first().text)
        assertEquals("gemini-3.8-flash", state.selectedModel)
        assertTrue(state.hasApiKey)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPromptChange("Hello World")

        assertEquals("Hello World", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeGeminiRepo.promptsReceived.isEmpty())
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeGeminiRepo.promptsReceived.isEmpty())
    }

    @Test
    fun onSend_successfulGeneration_savesMessagesAndClearsInput() = runTest(testDispatcher) {
        fakeGeminiRepo.responseText = "Gemini response text"
        val viewModel = createViewModel(hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("User query")
        viewModel.onSend()

        // Prompt input is immediately cleared
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        // Verify both user message and gemini message were persisted in history
        assertEquals(2, state.messages.size)
        assertEquals("User query", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Gemini response text", state.messages[1].text)
        assertFalse(state.messages[1].isUser)

        assertEquals(1, fakeGeminiRepo.promptsReceived.size)
        assertEquals("User query", fakeGeminiRepo.promptsReceived[0].first)
    }

    @Test
    fun onSend_failureGeneration_setsErrorMessageAndDismissesLoading() = runTest(testDispatcher) {
        fakeGeminiRepo.shouldFail = true
        fakeGeminiRepo.failureException = RuntimeException("API quota exceeded")

        val viewModel = createViewModel(hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("User query")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API quota exceeded", state.errorMessage)
        // User message should still be saved even if Gemini fails
        assertEquals(1, state.messages.size)
        assertEquals("User query", state.messages[0].text)
    }

    @Test
    fun onVoiceInputResult_populatesPromptForEditing() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.onVoiceInputResult("Voice input test")
        assertEquals("Voice input test", viewModel.uiState.value.prompt)

        // Calling voice input again appends to the prompt
        viewModel.onVoiceInputResult("more text")
        assertEquals("Voice input test more text", viewModel.uiState.value.prompt)
    }

    @Test
    fun onModelSelected_updatesPreference() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onModelSelected("gemini-3.8-flash")
        advanceUntilIdle()

        assertEquals("gemini-3.8-flash", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun onClearHistory_clearsMessages() = runTest(testDispatcher) {
        fakeHistoryRepo.saveUserMessage("Message 1")
        fakeHistoryRepo.saveGeminiMessage("Message 2")

        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeGeminiRepo.shouldFail = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Test")
        viewModel.onSend()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
