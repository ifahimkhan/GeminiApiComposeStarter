package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.InMemoryChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelVoiceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: InMemoryChatHistoryRepository

    private val dummyRepository = object : GeminiRepository {
        override suspend fun generateText(prompt: String): Result<String> =
            Result.success("Dummy response")

        override fun sendMessageStream(prompt: String): Flow<String> =
            flowOf("Dummy stream")

        override fun resetChatSession() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = InMemoryChatHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onVoiceInputResult_whenPromptIsEmpty_populatesPromptWithoutAutoSending() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onVoiceInputResult("What is the capital of France?")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("What is the capital of France?", state.prompt)
        assertNull(state.promptError)
        // Verify voice input does NOT automatically trigger send
        assertFalse(state.isLoading)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onVoiceInputResult_whenPromptHasExistingText_appendsRecognizedSpeech() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini,")
        viewModel.onVoiceInputResult("tell me a joke")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Hello Gemini, tell me a joke", state.prompt)
        assertNull(state.promptError)
        assertFalse(state.isLoading)
    }

    @Test
    fun onVoiceInputResult_whenRecognizedSpeechIsBlank_ignoresWithoutAlteringPrompt() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Existing text")
        viewModel.onVoiceInputResult("   ")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Existing text", state.prompt)
    }

    @Test
    fun onVoiceInputResult_clearsPromptErrorIfPresent() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        // Trigger empty prompt error
        viewModel.onPromptChange("  ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        // Receive voice input
        viewModel.onVoiceInputResult("Now I am speaking")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Now I am speaking", state.prompt)
        assertNull(state.promptError)
    }

    @Test
    fun onVoiceInputError_updatesErrorMessageInUiState() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onVoiceInputError("Speech recognition is not available on this device.")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Speech recognition is not available on this device.", state.errorMessage)
    }

    @Test
    fun onVoiceInputResult_allowsSubsequentKeyboardEditing() = runTest {
        val viewModel = ChatViewModel(
            repository = dummyRepository,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onVoiceInputResult("Voice dictated text")
        advanceUntilIdle()
        assertEquals("Voice dictated text", viewModel.uiState.value.prompt)

        // User edits via keyboard
        viewModel.onPromptChange("Voice dictated text edited by keyboard")
        advanceUntilIdle()
        assertEquals("Voice dictated text edited by keyboard", viewModel.uiState.value.prompt)
    }
}
