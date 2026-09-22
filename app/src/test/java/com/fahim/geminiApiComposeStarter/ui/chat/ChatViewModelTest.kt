package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.MainDispatcherRule
import com.fahim.geminiApiComposeStarter.data.Author
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.fake.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.fake.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.fake.FakeUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotState
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeGeminiRepository()
    private val history = FakeChatHistoryRepository()
    private val preferences = FakeUserPreferencesRepository()

    private fun viewModel(hasApiKey: Boolean = true) = ChatViewModel(
        repository = repository,
        history = history,
        preferences = preferences,
        hasApiKey = hasApiKey,
    )

    /**
     * `uiState` is a `WhileSubscribed` StateFlow, so it only emits while something collects it.
     * Every test needs a collector alive for its duration.
     */
    private fun TestScope.subscribe(viewModel: ChatViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @Test
    fun `onPromptChange updates the prompt and clears the empty-field error`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onSend()
        runCurrent()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        viewModel.onPromptChange("Hello")
        runCurrent()
        assertEquals("Hello", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun `blank prompt is rejected without calling the repository`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onPromptChange("   ")
        viewModel.onSend()
        runCurrent()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, repository.callCount)
        assertTrue(history.current.isEmpty())
    }

    @Test
    fun `a successful send stores the user turn and the model reply`() = runTest {
        repository.result = Result.success("Compose is a declarative UI toolkit.")
        // Hold the call open on the virtual clock so the in-flight state can be asserted
        // without racing the completion.
        repository.delayMillis = 1_000
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onPromptChange("What is Compose?")
        viewModel.onSend()
        runCurrent()

        // While the call is in flight: field cleared, spinner on, mascot thinking.
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(MascotState.THINKING, viewModel.uiState.value.mascotState)

        advanceTimeBy(1_001)
        runCurrent()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals(Author.USER, messages[0].author)
        assertEquals("What is Compose?", messages[0].text)
        assertEquals(Author.GEMINI, messages[1].author)
        assertEquals("Compose is a declarative UI toolkit.", messages[1].text)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("What is Compose?", repository.lastPrompt)
    }

    @Test
    fun `history is replayed to the repository but the new prompt is not duplicated in it`() =
        runTest {
            val viewModel = viewModel()
            subscribe(viewModel)

            viewModel.onPromptChange("First")
            viewModel.onSend()
            runCurrent()

            viewModel.onPromptChange("Second")
            viewModel.onSend()
            runCurrent()

            // The second call replays exactly the two turns that preceded it.
            assertEquals(2, repository.lastHistory.size)
            assertEquals("First", repository.lastHistory[0].text)
            assertEquals(Author.GEMINI, repository.lastHistory[1].author)
            assertEquals("Second", repository.lastPrompt)
        }

    @Test
    fun `a failure surfaces the error message and leaves loading off`() = runTest {
        repository.result = Result.failure(IllegalStateException("Network unreachable"))
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        runCurrent()

        assertEquals("Network unreachable", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(MascotState.CONFUSED, viewModel.uiState.value.mascotState)
        // The user's own turn is still kept, so the transcript is not silently lost.
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `onErrorShown clears the message so the snackbar is not repeated`() = runTest {
        repository.result = Result.failure(IllegalStateException("boom"))
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        runCurrent()
        viewModel.onErrorShown()
        runCurrent()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `a missing API key is reported without calling the repository`() = runTest {
        val viewModel = viewModel(hasApiKey = false)
        subscribe(viewModel)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        runCurrent()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            viewModel.uiState.value.errorMessage,
        )
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `clearing the history empties the transcript`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        runCurrent()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        runCurrent()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `preference changes are persisted and reflected in the state`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.onThemeModeChange(ThemeMode.DARK)
        viewModel.onDynamicColorChange(true)
        runCurrent()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertTrue(viewModel.uiState.value.dynamicColor)
        assertEquals(ThemeMode.DARK, preferences.current.themeMode)
    }

    @Test
    fun `an existing conversation is restored into the state`() = runTest {
        val seeded = FakeChatHistoryRepository(
            listOf(
                ChatMessage(id = 1, text = "Earlier question", author = Author.USER),
                ChatMessage(id = 2, text = "Earlier answer", author = Author.GEMINI),
            ),
        )
        val viewModel = ChatViewModel(repository, seeded, preferences, hasApiKey = true)
        subscribe(viewModel)
        runCurrent()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Earlier question", viewModel.uiState.value.messages[0].text)
    }

    @Test
    fun `setMascot drives the character directly`() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel)

        viewModel.setMascot(MascotState.SURPRISED)
        runCurrent()

        assertEquals(MascotState.SURPRISED, viewModel.uiState.value.mascotState)
    }
}
