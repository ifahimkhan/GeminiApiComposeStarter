package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.prefs.AppSettings
import com.fahim.geminiApiComposeStarter.data.prefs.SettingsRepository
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import kotlinx.coroutines.CompletableDeferred
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

/** Fake that returns whatever [result] is set to, optionally suspending until [gate] completes. */
private class FakeGeminiRepository : GeminiRepository {
    var result: Result<String> = Result.success("fake reply")
    var gate: CompletableDeferred<Unit>? = null
    val prompts = mutableListOf<String>()

    override suspend fun generateText(prompt: String): Result<String> {
        prompts += prompt
        gate?.await()
        return result
    }
}

private class FakeChatHistoryRepository : ChatHistoryRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override fun observeMessages(): Flow<List<ChatMessage>> = messages

    override suspend fun addMessage(text: String, isUser: Boolean) {
        messages.value = messages.value + ChatMessage(nextId++, text, isUser)
    }

    override suspend fun clear() {
        messages.value = emptyList()
    }
}

private class FakeSettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        state.value = state.value.copy(dynamicColor = enabled)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        // Unconfined: viewModelScope coroutines run eagerly, so state is updated synchronously.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        gemini = FakeGeminiRepository()
        viewModel = ChatViewModel(gemini, FakeChatHistoryRepository(), FakeSettingsRepository())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `sending a blank prompt shows an error and does not call Gemini`() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, state.promptError)
        assertTrue(gemini.prompts.isEmpty())
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `editing the prompt clears the prompt error`() = runTest {
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, state.promptError)

        viewModel.onPromptChange("hello")

        assertNull(state.promptError)
    }

    @Test
    fun `successful send stores user and model messages and clears the input`() = runTest {
        viewModel.onPromptChange("  What is Kotlin?  ")
        viewModel.onSend()

        assertEquals(listOf("What is Kotlin?"), gemini.prompts) // trimmed
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[0].isUser)
        assertEquals("What is Kotlin?", state.messages[0].text)
        assertFalse(state.messages[1].isUser)
        assertEquals("fake reply", state.messages[1].text)
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loading is true while waiting and a second send is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate

        viewModel.onPromptChange("first")
        viewModel.onSend()
        assertTrue(state.isLoading)

        viewModel.onPromptChange("second")
        viewModel.onSend() // ignored while loading
        assertEquals(listOf("first"), gemini.prompts)

        gate.complete(Unit)
        assertFalse(state.isLoading)
    }

    @Test
    fun `failed send shows the error and gives the prompt back`() = runTest {
        gemini.result = Result.failure(IllegalStateException("Network down"))

        viewModel.onPromptChange("hello")
        viewModel.onSend()

        assertEquals("Network down", state.errorMessage)
        assertEquals("hello", state.prompt)
        assertFalse(state.isLoading)
        // The user's own message is still part of the history; only the reply is missing.
        assertEquals(1, state.messages.size)
    }

    @Test
    fun `onErrorShown clears the error message`() = runTest {
        gemini.result = Result.failure(IllegalStateException("boom"))
        viewModel.onPromptChange("hello")
        viewModel.onSend()

        viewModel.onErrorShown()

        assertNull(state.errorMessage)
    }

    @Test
    fun `voice result fills an empty prompt and appends to an existing one`() = runTest {
        viewModel.onVoiceResult("hello there")
        assertEquals("hello there", state.prompt)

        viewModel.onVoiceResult("general kenobi")
        assertEquals("hello there general kenobi", state.prompt)
    }

    @Test
    fun `blank voice result is ignored`() = runTest {
        viewModel.onVoiceResult("  ")
        assertEquals("", state.prompt)
    }

    @Test
    fun `voice unavailable surfaces an error message`() = runTest {
        viewModel.onVoiceUnavailable()
        assertEquals(ChatViewModel.VOICE_UNAVAILABLE_MESSAGE, state.errorMessage)
    }

    @Test
    fun `clear chat removes all messages`() = runTest {
        viewModel.onPromptChange("hi")
        viewModel.onSend()
        assertEquals(2, state.messages.size)

        viewModel.onClearChat()

        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `theme and dynamic colour changes flow through to the UI state`() = runTest {
        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(false)

        assertEquals(ThemeMode.DARK, state.themeMode)
        assertFalse(state.dynamicColor)
    }
}
