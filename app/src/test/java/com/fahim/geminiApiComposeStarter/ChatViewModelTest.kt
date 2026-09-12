package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.AppTheme
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.MessageSender
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeGeminiRepository : GeminiRepository {
        var shouldFail = false
        var mockResponse = "Hello from Fake Gemini"
        val savedMessages = mutableListOf<ChatMessage>()
        val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())

        override suspend fun generateText(prompt: String): Result<String> {
            return if (shouldFail) {
                Result.failure(RuntimeException("API Error"))
            } else {
                Result.success(mockResponse)
            }
        }

        override fun getMessagesFlow(): Flow<List<ChatMessage>> = messagesFlow

        override suspend fun saveMessage(message: ChatMessage) {
            savedMessages.add(message)
            messagesFlow.value = savedMessages.toList()
        }
    }

    private class FakeThemeRepository : ThemeRepository(DummyContext()) {
        val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
        override val selectedThemeFlow: Flow<AppTheme> = themeFlow
        override suspend fun setTheme(theme: AppTheme) {
            themeFlow.value = theme
        }
    }

    // A minimal dummy context that doesn't actually do anything
    private class DummyContext : android.content.ContextWrapper(null) {
        override fun getApplicationContext(): android.content.Context = this
    }

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeThemeRepository: FakeThemeRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeThemeRepository = FakeThemeRepository()
        
        viewModel = ChatViewModel(
            repository = fakeRepository,
            themeRepository = fakeThemeRepository,
            hasApiKey = { true }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertEquals(AppTheme.SYSTEM, state.selectedTheme)
    }

    @Test
    fun `test successful message flow adds user and gemini messages`() = runTest {
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hello Gemini", state.messages[0].text)
        assertEquals(MessageSender.USER, state.messages[0].sender)
        assertEquals("Hello from Fake Gemini", state.messages[1].text)
        assertEquals(MessageSender.GEMINI, state.messages[1].sender)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `test gemini api failure updates error message state`() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Fail prompt")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API Error", state.errorMessage)
    }

    @Test
    fun `test empty input does not trigger api request`() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
        assertTrue(fakeRepository.savedMessages.isEmpty())
    }

    @Test
    fun `test multiple messages maintain chronological order`() = runTest {
        viewModel.onPromptChange("First Prompt")
        viewModel.onSend()
        viewModel.onPromptChange("Second Prompt")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(4, state.messages.size)
        assertEquals("First Prompt", state.messages[0].text)
        assertEquals("Second Prompt", state.messages[2].text)
    }

    @Test
    fun `test retry functionality triggers request correctly after error`() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Retry test")
        viewModel.onSend()

        assertEquals("API Error", viewModel.uiState.value.errorMessage)

        fakeRepository.shouldFail = false
        viewModel.retryLastMessage()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Hello from Fake Gemini", state.messages.last().text)
    }
}
