package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.PreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository : GeminiRepository {
    var responseResult: Result<String> = Result.success("Fake response")
    
    override suspend fun generateText(prompt: String): Result<String> {
        return responseResult
    }
}

class FakeChatHistoryRepository : ChatHistoryRepository {
    private val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messages

    override suspend fun addMessage(text: String, isUser: Boolean) {
        val current = messages.value.toMutableList()
        current.add(ChatMessageEntity(id = nextId++, text = text, isUser = isUser, timestamp = System.currentTimeMillis()))
        messages.value = current
    }

    override suspend fun clearHistory() {
        messages.value = emptyList()
    }
}

class FakePreferencesRepository : PreferencesRepository {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = _themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit val geminiRepository: FakeGeminiRepository
    private lateinit val chatHistoryRepository: FakeChatHistoryRepository
    private lateinit val preferencesRepository: FakePreferencesRepository
    private lateinit val viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        geminiRepository = FakeGeminiRepository()
        chatHistoryRepository = FakeChatHistoryRepository()
        preferencesRepository = FakePreferencesRepository()
        viewModel = ChatViewModel(geminiRepository, chatHistoryRepository, preferencesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
        assertEquals(null, state.promptError)
    }

    @Test
    fun `send empty message shows error`() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
    }

    @Test
    fun `successful Gemini response adds messages`() = runTest {
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.errorMessage)
        assertEquals(2, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Fake response", state.messages[1].text)
        assertTrue(!state.messages[1].isUser)
    }

    @Test
    fun `Gemini failure sets error message`() = runTest {
        geminiRepository.responseResult = Result.failure(Exception("Network error"))
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Network error", state.errorMessage)
        assertEquals(1, state.messages.size) // User message is still added
    }
    
    @Test
    fun `clear history works`() = runTest {
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)
        
        viewModel.clearHistory()
        advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `theme preference updates`() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, preferencesRepository.themeMode.first())
    }
}
