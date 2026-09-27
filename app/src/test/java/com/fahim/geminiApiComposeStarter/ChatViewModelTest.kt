package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
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
            chatDao = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefaultAndEmpty() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertFalse(state.isListeningVoice)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest {
        viewModel.onPromptChange("Transmit coordinates")
        assertEquals("Transmit coordinates", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
            chatDao = null
        )
        noKeyViewModel.onPromptChange("Hello Orbit")
        noKeyViewModel.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            noKeyViewModel.uiState.value.errorMessage,
        )
        assertFalse(noKeyViewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_validPrompt_success_updatesMessagesAndClearsLoading() = runTest {
        viewModel.onPromptChange("Hello Orbit AI")
        viewModel.onSend()

        // Check that prompt field was cleared and loading started
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Hello Orbit AI", viewModel.uiState.value.messages[0].text)
        assertTrue(viewModel.uiState.value.messages[0].isUser)

        advanceUntilIdle()

        // After completion, loading is false and both user + model messages are populated
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Orbit Core Transmission: Signal verified.", viewModel.uiState.value.messages[1].text)
        assertFalse(viewModel.uiState.value.messages[1].isUser)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Will fail")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Orbit connection lost", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun setVoiceListening_updatesState() = runTest {
        viewModel.setVoiceListening(true)
        assertTrue(viewModel.uiState.value.isListeningVoice)

        viewModel.setVoiceListening(false)
        assertFalse(viewModel.uiState.value.isListeningVoice)
    }

    @Test
    fun onVoiceResult_setsPromptAndSends() = runTest {
        viewModel.onVoiceResult("Voice prompt test")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Voice prompt test", viewModel.uiState.value.messages[0].text)
    }

    @Test
    fun onClearChat_clearsMessages() = runTest {
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Fail test")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("Orbit connection lost", viewModel.uiState.value.errorMessage)
        viewModel.clearErrorMessage()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

/**
 * Fake in-memory GeminiRepository implementation for unit testing.
 */
class FakeGeminiRepository(
    var shouldFail: Boolean = false,
    var mockResponse: String = "Orbit Core Transmission: Signal verified.",
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(RuntimeException("Orbit connection lost"))
        } else {
            Result.success(mockResponse)
        }
    }
}
