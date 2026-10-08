package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeMessageRepository
    private lateinit var settings: FakePreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        gemini = FakeGeminiRepository()
        history = FakeMessageRepository()
        settings = FakePreferencesRepository()
        viewModel = ChatViewModel(gemini, history, settings)
    }

    @Test
    fun sendMessage_addsUserMessageAndGeminiReply() = runTest {
        viewModel.onInputChange("Hi Gemini")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hi Gemini", state.messages[0].text)
        assertTrue(state.messages[0].isFromUser)
        assertEquals("Hello from fake Gemini", state.messages[1].text)
        assertFalse(state.messages[1].isFromUser)
        assertEquals("", state.input)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun sendMessage_blankInput_doesNothing() = runTest {
        viewModel.onInputChange("   ")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(gemini.prompts.isEmpty())
    }

    @Test
    fun sendMessage_showsLoadingWhileWaiting() = runTest {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate

        viewModel.onInputChange("Slow question")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse("Send disabled while loading", viewModel.uiState.value.canSend)

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun sendMessage_whileLoading_isIgnored() = runTest {
        gemini.gate = CompletableDeferred()
        viewModel.onInputChange("first")
        viewModel.sendMessage()
        viewModel.onInputChange("second")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals(listOf("first"), gemini.prompts)
    }

    @Test
    fun sendMessage_passesPreviousMessagesAsHistory() = runTest {
        viewModel.onInputChange("one")
        viewModel.sendMessage()
        advanceUntilIdle()
        viewModel.onInputChange("two")
        viewModel.sendMessage()
        advanceUntilIdle()

        assertTrue(gemini.histories[0].isEmpty())
        assertEquals(listOf("one", "Hello from fake Gemini"), gemini.histories[1].map { it.text })
    }

    @Test
    fun failure_setsErrorAndRetryRecovers() = runTest {
        gemini.nextResult = Result.failure(IOException("timeout"))
        viewModel.onInputChange("Will fail")
        viewModel.sendMessage()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("Will fail", state.failedPrompt)
        assertEquals(1, state.messages.size) // user message kept

        gemini.nextResult = Result.success("Recovered")
        viewModel.retry()
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertEquals(listOf("Will fail", "Recovered"), state.messages.map { it.text })
        // Retry must not include the failed prompt twice.
        assertTrue(gemini.histories.last().isEmpty())
    }

    @Test
    fun dismissError_clearsError() = runTest {
        gemini.nextResult = Result.failure(RuntimeException("boom"))
        viewModel.onInputChange("x")
        viewModel.sendMessage()
        advanceUntilIdle()

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.failedPrompt)
    }

    @Test
    fun voiceResult_fillsOrAppendsToInput() {
        viewModel.onVoiceResult("hello")
        assertEquals("hello", viewModel.uiState.value.input)
        viewModel.onVoiceResult("world")
        assertEquals("hello world", viewModel.uiState.value.input)
    }

    @Test
    fun suggestion_fillsAndSendsImmediately() = runTest {
        viewModel.useSuggestion("Write a short poem about Android")
        advanceUntilIdle()

        assertEquals(listOf("Write a short poem about Android"), gemini.prompts)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun clearHistory_removesAllMessages() = runTest {
        viewModel.onInputChange("Hi")
        viewModel.sendMessage()
        advanceUntilIdle()

        viewModel.clearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun preferences_arePersistedAndReflectedInState() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setDynamicColor(false)
        advanceUntilIdle()

        val s = viewModel.uiState.value.settings
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertFalse(s.dynamicColor)
    }
}
