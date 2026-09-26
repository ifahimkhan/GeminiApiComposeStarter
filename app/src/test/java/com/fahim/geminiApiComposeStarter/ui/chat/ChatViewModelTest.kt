package com.fahim.geminiApiComposeStarter.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var geminiRepository: FakeGeminiRepository
    private lateinit var historyRepository: FakeChatHistoryRepository
    private lateinit var preferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        geminiRepository = FakeGeminiRepository()
        historyRepository = FakeChatHistoryRepository()
        preferencesRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(hasApiKey: Boolean = true) = ChatViewModel(
        repository = geminiRepository,
        historyRepository = historyRepository,
        preferencesRepository = preferencesRepository,
        hasApiKey = hasApiKey,
    )

    @Test
    fun `sending an empty prompt sets a prompt error and does not call the repository`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertNull(geminiRepository.lastPrompt)
    }

    @Test
    fun `sending without an api key surfaces the missing key message`() = runTest {
        val viewModel = createViewModel(hasApiKey = false)
        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(geminiRepository.lastPrompt)
    }

    @Test
    fun `successful send appends the user prompt and the reply to history`() = runTest {
        geminiRepository.setNextResult(Result.success("Hi there"))
        val viewModel = createViewModel()
        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
        assertTrue(state.messages[0].isFromUser)
        assertEquals("Hi there", state.messages[1].text)
        assertTrue(!state.messages[1].isFromUser)
        assertTrue(!state.isLoading)
        assertEquals("", state.prompt)
    }

    @Test
    fun `failed send surfaces the error message and keeps the user prompt in history`() = runTest {
        geminiRepository.setNextResult(Result.failure(IllegalStateException("network down")))
        val viewModel = createViewModel()
        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("network down", state.errorMessage)
        assertTrue(!state.isLoading)
    }

    @Test
    fun `voice input unavailable surfaces an error message`() = runTest {
        val viewModel = createViewModel()

        viewModel.onVoiceInputUnavailable()

        assertEquals(ChatViewModel.VOICE_INPUT_UNAVAILABLE_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `toggling dark mode flips the current value`() = runTest {
        val viewModel = createViewModel()

        viewModel.toggleDarkMode(currentlyDark = false)

        assertEquals(true, viewModel.darkModeOverride.value)
    }
}
