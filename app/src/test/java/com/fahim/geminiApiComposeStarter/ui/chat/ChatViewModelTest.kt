package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ChatSender
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
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

class FakeGeminiRepository : GeminiRepository {
    val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    var shouldFail = false
    var failureErrorMessage = "API Error"

    override fun getMessages(): Flow<List<ChatMessage>> = messagesFlow

    override fun getThemeMode(): Flow<ThemeMode> = themeModeFlow

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        themeModeFlow.value = themeMode
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        if (shouldFail) return Result.failure(RuntimeException(failureErrorMessage))
        val newId = (messagesFlow.value.size + 1).toLong()
        val userMsg = ChatMessage(id = newId, sender = ChatSender.USER, content = prompt)
        val assistantMsg = ChatMessage(id = newId + 1, sender = ChatSender.ASSISTANT, content = "Echo: $prompt")
        messagesFlow.value = messagesFlow.value + userMsg + assistantMsg
        return Result.success("Echo: $prompt")
    }

    override suspend fun clearHistory() {
        messagesFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialUiState_isEmpty() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPromptState() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSpeechResult_appendsSpeechText() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Initial")
        viewModel.onSpeechResult("spoken prompt")

        assertEquals("Initial spoken prompt", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_noApiKey_setsErrorMessage() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_success_clearsPromptAndUpdatesMessages() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals(2, state.messages.size)
        assertEquals("Hello", state.messages[0].content)
        assertEquals("Echo: Hello", state.messages[1].content)
        assertFalse(state.isLoading)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureErrorMessage = "Network connection timeout"

        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Test")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Network connection timeout", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun onClearHistory_clearsAllMessages() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onCycleThemeMode_cyclesThroughThemeModes() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

        viewModel.onCycleThemeMode()
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.onCycleThemeMode()
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        viewModel.onCycleThemeMode()
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }
}
