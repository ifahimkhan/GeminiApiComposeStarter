package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeGeminiRepository(var result: Result<String> = Result.success("Hi there")) : GeminiRepository {
    val prompts = mutableListOf<String>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun generateText(prompt: String): Result<String> {
        prompts += prompt
        gate?.await()
        return result
    }
}

private class FakeHistory : ChatHistoryRepository {
    private val store = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L
    override val messages: Flow<List<ChatMessage>> = store
    override suspend fun add(message: ChatMessage) { store.value += message.copy(id = nextId++) }
    override suspend fun clear() { store.value = emptyList() }
}

private class FakeSettings : SettingsRepository {
    private val mode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = mode
    override suspend fun setThemeMode(mode: ThemeMode) { this.mode.value = mode }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeHistory
    private lateinit var settings: FakeSettings

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gemini = FakeGeminiRepository()
        history = FakeHistory()
        settings = FakeSettings()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(hasApiKey: Boolean = true) =
        ChatViewModel(gemini, history, settings, hasApiKey, clock = { 1_000L })

    @Test
    fun emptyPrompt_setsPromptError_andDoesNotCallGemini() {
        val vm = viewModel()
        vm.onPromptChange("   ")
        vm.onSend()

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun typing_clearsPromptError() {
        val vm = viewModel()
        vm.onSend()
        vm.onPromptChange("h")

        assertNull(vm.uiState.value.promptError)
    }

    @Test
    fun send_success_addsUserAndGeminiBubbles_andClearsPrompt() {
        val vm = viewModel()
        vm.onPromptChange("  hello  ")
        vm.onSend()

        val state = vm.uiState.value
        assertEquals(listOf("hello", "Hi there"), state.messages.map { it.text })
        assertEquals(listOf(true, false), state.messages.map { it.isUser })
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun send_showsLoading_whileRequestIsInFlight() {
        gemini.gate = CompletableDeferred()
        val vm = viewModel()
        vm.onPromptChange("hello")
        vm.onSend()

        assertTrue(vm.uiState.value.isLoading)

        gemini.gate!!.complete(Unit)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun secondSend_isIgnored_whileLoading() {
        gemini.gate = CompletableDeferred()
        val vm = viewModel()
        vm.onPromptChange("one")
        vm.onSend()
        vm.onPromptChange("two")
        vm.onSend()

        assertEquals(listOf("one"), gemini.prompts)
    }

    @Test
    fun send_failure_setsFriendlyError_andKeepsUserBubble() {
        gemini.result = Result.failure(IllegalStateException("503 This model is currently experiencing high demand"))
        val vm = viewModel()
        vm.onPromptChange("hello")
        vm.onSend()

        val state = vm.uiState.value
        assertEquals(ChatViewModel.BUSY_MESSAGE, state.errorMessage)
        assertEquals(listOf("hello"), state.messages.map { it.text })
        assertFalse(state.isLoading)
    }

    @Test
    fun retry_resendsLastPrompt_withoutDuplicatingUserBubble() {
        gemini.result = Result.failure(RuntimeException("boom"))
        val vm = viewModel()
        vm.onPromptChange("hello")
        vm.onSend()

        gemini.result = Result.success("Recovered")
        vm.onRetry()

        assertEquals(listOf("hello", "hello"), gemini.prompts)
        assertEquals(listOf("hello", "Recovered"), vm.uiState.value.messages.map { it.text })
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun missingApiKey_showsMessage_andDoesNotCallGemini() {
        val vm = viewModel(hasApiKey = false)
        vm.onPromptChange("hello")
        vm.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun errorShown_clearsErrorMessage() {
        val vm = viewModel(hasApiKey = false)
        vm.onPromptChange("hello")
        vm.onSend()
        vm.onErrorShown()

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun clearChat_removesAllMessages() {
        val vm = viewModel()
        vm.onPromptChange("hello")
        vm.onSend()
        vm.onClearChat()

        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun voiceResult_isAppendedToExistingPrompt() {
        val vm = viewModel()
        vm.onPromptChange("Tell me")
        vm.onVoiceResult(" a joke ")

        assertEquals("Tell me a joke", vm.uiState.value.prompt)
    }

    @Test
    fun suggestionClick_sendsThatText() {
        val vm = viewModel()
        vm.onSuggestionClick("Write a poem")

        assertEquals(listOf("Write a poem"), gemini.prompts)
    }

    @Test
    fun themeChange_isPersistedAndReflectedInState() {
        val vm = viewModel()
        vm.onThemeModeChange(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
    }

    @Test
    fun friendlyError_mapsTimeouts() {
        val error = RuntimeException("Something unexpected", java.net.SocketTimeoutException("Socket timeout has expired"))

        assertEquals(ChatViewModel.TIMEOUT_MESSAGE, ChatViewModel.friendlyError(error))
    }
}
