package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

// ---------- Fakes (no network, no database) ----------

/** Returns whatever [result] is set to, and remembers the prompts it received. */
class FakeGeminiRepository(var result: Result<String> = Result.success("Hello from fake Gemini")) :
    GeminiRepository {
    val prompts = mutableListOf<String>()
    override suspend fun generateText(prompt: String): Result<String> {
        prompts.add(prompt)
        return result
    }
}

/** Keeps messages in memory instead of Room. */
class FakeChatHistoryRepository : ChatHistoryRepository {
    private val list = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 0L
    override val messages: Flow<List<ChatMessage>> = list
    override suspend fun addMessage(text: String, isUser: Boolean) {
        list.update { it + ChatMessage(id = nextId++, text = text, isUser = isUser) }
    }
    override suspend fun clear() {
        list.value = emptyList()
    }
}

/** Keeps the dark mode setting in memory instead of DataStore. */
class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val dark = MutableStateFlow<Boolean?>(null)
    override val darkMode: Flow<Boolean?> = dark
    override suspend fun setDarkMode(enabled: Boolean) {
        dark.value = enabled
    }
}

// ---------- Tests ----------

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistoryRepository
    private lateinit var preferences: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        // viewModelScope uses Dispatchers.Main, so replace it with a test dispatcher
        Dispatchers.setMain(testDispatcher)
        gemini = FakeGeminiRepository()
        history = FakeChatHistoryRepository()
        preferences = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true) =
        ChatViewModel(gemini, history, preferences, hasApiKey)

    @Test
    fun emptyPrompt_showsPromptError_andDoesNotCallGemini() = runTest {
        val viewModel = createViewModel()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun missingApiKey_showsErrorMessage() = runTest {
        val viewModel = createViewModel(hasApiKey = false)

        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun successfulSend_addsUserAndGeminiMessages_andClearsInput() = runTest {
        gemini.result = Result.success("I am Gemini")
        val viewModel = createViewModel()

        viewModel.onPromptChange("Who are you?")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Who are you?", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("I am Gemini", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
    }

    @Test
    fun failedSend_showsError_andKeepsOnlyUserMessage() = runTest {
        gemini.result = Result.failure(RuntimeException("Network error"))
        val viewModel = createViewModel()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Network error", state.errorMessage)
        assertEquals(1, state.messages.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun onErrorShown_clearsErrorMessage() = runTest {
        val viewModel = createViewModel(hasApiKey = false)
        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        viewModel.onErrorShown()

        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearChat_removesAllMessages() = runTest {
        val viewModel = createViewModel()
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun darkModeChange_isSavedAndShownInState() = runTest {
        val viewModel = createViewModel()

        viewModel.onDarkModeChange(true)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.darkMode)
        assertTrue(viewModel.uiState.value.darkMode!!)
    }
}