package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.MISSING_API_KEY_MESSAGE
import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.fake.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.fake.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.fake.FakeUserPreferencesRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistoryRepository
    private lateinit var preferences: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gemini = FakeGeminiRepository()
        history = FakeChatHistoryRepository()
        preferences = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatViewModel(gemini, history, preferences)

    @Test
    fun `blank prompt is rejected without calling the api`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(gemini.prompts.isEmpty())
        assertTrue(history.current.isEmpty())
    }

    @Test
    fun `successful send records both turns and clears the prompt`() = runTest(dispatcher) {
        gemini.nextResult = Result.success("42")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("What is six times seven?")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(2, state.messages.size)
        assertEquals(Author.USER, state.messages[0].author)
        assertEquals("What is six times seven?", state.messages[0].text)
        assertEquals(Author.MODEL, state.messages[1].author)
        assertEquals("42", state.messages[1].text)
    }

    @Test
    fun `history sent to the api excludes the turn being sent`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("first")
        viewModel.onSend()
        advanceUntilIdle()
        viewModel.onPromptChange("second")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(gemini.historiesSeen[0].isEmpty())
        assertEquals(2, gemini.historiesSeen[1].size)
        assertEquals(listOf("first", "second"), gemini.prompts)
    }

    @Test
    fun `a failure keeps the user turn and offers a retryable error`() = runTest(dispatcher) {
        gemini.nextResult = Result.failure(IllegalStateException("no network"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.messages.size)
        assertEquals(Author.USER, state.messages[0].author)
        assertNotNull(state.error)
        assertEquals("no network", state.error?.message)
        assertTrue(state.error?.retryable == true)
    }

    @Test
    fun `retry resends the failed prompt without duplicating the user turn`() = runTest(dispatcher) {
        gemini.nextResult = Result.failure(IllegalStateException("timeout"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("ping")
        viewModel.onSend()
        advanceUntilIdle()

        gemini.nextResult = Result.success("pong")
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("ping", "ping"), gemini.prompts)
        assertEquals(2, state.messages.size)
        assertEquals("pong", state.messages[1].text)
        assertNull(state.error)
    }

    @Test
    fun `a missing api key is reported but not retryable`() = runTest(dispatcher) {
        gemini.nextResult = Result.failure(MissingApiKeyException())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("hello")
        viewModel.onSend()
        advanceUntilIdle()

        val error = viewModel.uiState.value.error
        assertEquals(MISSING_API_KEY_MESSAGE, error?.message)
        assertFalse(error?.retryable == true)
    }

    @Test
    fun `a speech result is appended to whatever is already typed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Summarise")
        viewModel.onVoiceResult("this article")
        advanceUntilIdle()

        assertEquals("Summarise this article", viewModel.uiState.value.prompt)
    }

    @Test
    fun `clearing the conversation empties the stored history`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("hi")
        viewModel.onSend()
        advanceUntilIdle()
        viewModel.onClearConversation()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `theme choice is written through to the preferences store`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onThemeModeChange(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, preferences.current.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `an error surfaced twice produces two distinct events`() = runTest(dispatcher) {
        gemini.nextResult = Result.failure(IllegalStateException("same failure"))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("one")
        viewModel.onSend()
        advanceUntilIdle()
        val first = viewModel.uiState.value.error
        viewModel.onErrorShown()

        viewModel.onPromptChange("two")
        viewModel.onSend()
        advanceUntilIdle()
        val second = viewModel.uiState.value.error

        assertNotNull(first)
        assertNotNull(second)
        assertFalse(first == second)
    }
}
