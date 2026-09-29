package com.fahim.geminiApiComposeStarter

import android.app.Application
import android.content.Context
import com.fahim.geminiApiComposeStarter.data.ChatAttachment
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSession
import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/**
 * Fake implementation of GeminiRepository for deterministic unit testing.
 */
class FakeGeminiRepository(
    var shouldFail: Boolean = false,
    var mockResponse: String = "This is a fake Gemini response.",
    var failureMessage: String = "Network connection failed",
) : GeminiRepository {

    var lastPrompt: String? = null
    var lastModelName: String? = null

    override suspend fun generateChatResponse(
        context: Context,
        history: List<ChatMessage>,
        prompt: String,
        attachments: List<ChatAttachment>,
        modelName: String,
    ): Result<String> {
        lastPrompt = prompt
        lastModelName = modelName
        return if (shouldFail) {
            Result.failure(RuntimeException(failureMessage))
        } else {
            Result.success(mockResponse)
        }
    }
}

/**
 * In-memory ChatStorage for unit tests.
 */
class InMemoryChatStorage : ChatStorage(null) {
    private var sessions: List<ChatSession> = emptyList()
    private var activeId: String? = null
    private var selectedModel: String = "gemini-3.8-flash"

    override fun loadSessions(): List<ChatSession> = sessions
    override fun saveSessions(sessions: List<ChatSession>) {
        this.sessions = sessions
    }

    override fun loadActiveSessionId(): String? = activeId
    override fun saveActiveSessionId(id: String) {
        this.activeId = id
    }

    override fun loadSelectedModel(default: String): String = selectedModel
    override fun saveSelectedModel(model: String) {
        this.selectedModel = model
    }
}

/**
 * Mock Application class to safely satisfy AndroidViewModel in local JVM unit tests.
 */
class FakeApplication : Application() {
    override fun getApplicationContext(): Context = this
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeStorage: InMemoryChatStorage
    private lateinit var fakeApplication: FakeApplication

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeStorage = InMemoryChatStorage()
        fakeApplication = FakeApplication()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            application = fakeApplication,
            repository = fakeRepository,
            storage = fakeStorage,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun initialState_hasDefaultSessionAndModel() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertNotNull(state.currentSessionId)
        assertTrue(state.messages.isEmpty())
        assertEquals("gemini-3.8-flash", state.selectedModel)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        val viewModel = createViewModel()
        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() {
        val viewModel = createViewModel()
        viewModel.onPromptChange("")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() {
        val viewModel = createViewModel(hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_success_appendsUserAndModelMessages() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = true)
        fakeRepository.mockResponse = "Hello, I am Gemini!"

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        // User message added immediately
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Hi there", viewModel.uiState.value.messages[0].text)
        assertTrue(viewModel.uiState.value.messages[0].isUser)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // After coroutine completes
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Hello, I am Gemini!", viewModel.uiState.value.messages[1].text)
        assertFalse(viewModel.uiState.value.messages[1].isUser)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = true)
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Quota exceeded"

        viewModel.onPromptChange("Test query")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Quota exceeded", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onNewChat_createsNewSession() {
        val viewModel = createViewModel()
        val initialId = viewModel.uiState.value.currentSessionId

        viewModel.onNewChat()
        val newId = viewModel.uiState.value.currentSessionId

        assertTrue(initialId != newId)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSelectModel_updatesSelectedModel() {
        val viewModel = createViewModel()
        viewModel.onSelectModel("gemini-3.1-pro")

        assertEquals("gemini-3.1-pro", viewModel.uiState.value.selectedModel)
    }
}
