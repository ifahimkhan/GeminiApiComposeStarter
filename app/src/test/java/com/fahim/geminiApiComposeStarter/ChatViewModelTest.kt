package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.CompletableDeferred
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state has empty prompt, empty messages, not loading, and no errors`() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
    }

    @Test
    fun `sending empty prompt sets promptError to EMPTY and does not call repository`() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(fakeRepository.generatedPrompts.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sending prompt with missing API key shows missing key error message`() = runTest {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
        )

        noKeyViewModel.onPromptChange("Hello Gemini")
        noKeyViewModel.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            noKeyViewModel.uiState.value.errorMessage,
        )
        assertTrue(fakeRepository.generatedPrompts.isEmpty())
    }

    @Test
    fun `loading state is true while awaiting response and user message is immediately added`() = runTest {
        val gate = CompletableDeferred<Unit>()
        fakeRepository.responseDeferred = gate

        viewModel.onPromptChange("What is Jetpack Compose?")
        viewModel.onSend()

        // Advance dispatcher until background generation pauses at gate
        testDispatcher.scheduler.runCurrent()

        val inFlightState = viewModel.uiState.value
        assertTrue(inFlightState.isLoading)
        assertEquals(1, inFlightState.messages.size)
        assertEquals("What is Jetpack Compose?", inFlightState.messages[0].text)
        assertEquals(MessageRole.USER, inFlightState.messages[0].role)

        // Release gate and finish
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `successful gemini response appends gemini message and stops loading`() = runTest {
        fakeRepository.cannedResponse = "Jetpack Compose is Android's modern UI toolkit."

        viewModel.onPromptChange("Tell me about Compose")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.messages.size)

        // User message
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("Tell me about Compose", state.messages[0].text)

        // Gemini message
        assertEquals(MessageRole.GEMINI, state.messages[1].role)
        assertEquals("Jetpack Compose is Android's modern UI toolkit.", state.messages[1].text)
    }

    @Test
    fun `api failure updates error message and stops loading`() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Network timeout occurred"

        viewModel.onPromptChange("Generate code")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network timeout occurred", state.errorMessage)

        // Dismiss error
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `conversation state updates voice input into prompt correctly`() {
        viewModel.onPromptChange("Translate")
        viewModel.onVoiceInputReceived("good morning")

        assertEquals("Translate good morning", viewModel.uiState.value.prompt)
    }

    @Test
    fun `clearing history removes all messages`() = runTest {
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
