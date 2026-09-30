package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

// ---------- Fakes ----------
class FakeGeminiRepository(var result: Result<String> = Result.success("Hi from Gemini")) : GeminiRepository {
    var calls = 0
    override suspend fun generateText(prompt: String): Result<String> { calls++; return result }
}

class FakeChatHistoryRepository : ChatHistoryRepository {
    private val store = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = store
    override suspend fun add(text: String, isUser: Boolean) {
        store.update { it + ChatMessage(id = it.size + 1L, text = text, isUser = isUser) }
    }
    override suspend fun clear() { store.value = emptyList() }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val mode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = mode
    override suspend fun setThemeMode(mode: ThemeMode) { this.mode.value = mode }
}

// ---------- Tests ----------
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistoryRepository

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        gemini = FakeGeminiRepository()
        history = FakeChatHistoryRepository()
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(hasKey: Boolean = true) =
        ChatViewModel(gemini, history, FakeUserPreferencesRepository(), hasApiKey = hasKey)

    @Test fun emptyPrompt_setsPromptError_andDoesNotCallApi() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSend()
        advanceUntilIdle()
        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertEquals(0, gemini.calls)
    }

    @Test fun missingApiKey_showsError() = runTest(dispatcher) {
        val vm = viewModel(hasKey = false)
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertEquals(0, gemini.calls)
    }

    @Test fun success_addsUserAndGeminiMessages_andClearsPrompt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
        assertEquals(listOf("Hello", "Hi from Gemini"), state.messages.map { it.text })
        assertEquals(listOf(true, false), state.messages.map { it.isUser })
    }

    @Test fun failure_showsError_andRestoresPrompt() = runTest(dispatcher) {
        gemini.result = Result.failure(RuntimeException("Network down"))
        val vm = viewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network down", state.errorMessage)
        assertEquals("Hello", state.prompt)
    }

    @Test fun errorShown_clearsErrorMessage() = runTest(dispatcher) {
        val vm = viewModel(hasKey = false)
        vm.onPromptChange("Hello")
        vm.onSend()
        vm.onErrorShown()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test fun voiceResult_fillsPrompt() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onVoiceResult("spoken text")
        assertEquals("spoken text", vm.uiState.value.prompt)
    }

    @Test fun toggleTheme_cyclesAndPersists() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()
        vm.onToggleTheme()
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
    }

    @Test fun clearChat_removesHistory() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        vm.onClearChat()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.messages.isEmpty())
    }
}
