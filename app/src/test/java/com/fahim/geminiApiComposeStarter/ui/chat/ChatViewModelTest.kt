package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGeminiRepository(
        var resultToReturn: Result<String> = Result.success("Fake response"),
    ) : GeminiRepository {
        var callCount = 0
        var lastPromptReceived: String? = null

        override suspend fun generateText(prompt: String): Result<String> {
            callCount++
            lastPromptReceived = prompt
            return resultToReturn
        }
    }

    @Test
    fun initialState_isEmpty() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals("", state.response)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPrompt() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyError() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun onSend_success_updatesResponseAndClearsPrompt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("Generated text"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Generated text", viewModel.uiState.value.response)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, repo.callCount)
        assertEquals("Tell me a joke", repo.lastPromptReceived)

        // Verify conversational messages
        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Tell me a joke", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertTrue(messages[0].id.isNotBlank())
        assertEquals("Generated text", messages[1].text)
        assertEquals(Participant.ASSISTANT, messages[1].participant)
        assertTrue(messages[1].id.isNotBlank())
    }

    @Test
    fun onSend_failure_updatesErrorMessage() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("Network failure")))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network failure", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun retry_reSendsLastPrompt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("First error")))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, repo.callCount)
        assertEquals("First error", viewModel.uiState.value.errorMessage)

        // Now repository will succeed on retry
        repo.resultToReturn = Result.success("Success on retry")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repo.callCount)
        assertEquals("First question", repo.lastPromptReceived)
        assertEquals("Success on retry", viewModel.uiState.value.response)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onVoiceResult_whenPromptEmpty_setsPromptAndDoesNotSend() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onVoiceResult("Hello from speech recognition")

        assertEquals("Hello from speech recognition", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
        assertEquals(0, repo.callCount)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onVoiceResult_whenPromptNotEmpty_appendsToExistingPrompt() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Write a poem about")
        viewModel.onVoiceResult("the night sky")

        assertEquals("Write a poem about the night sky", viewModel.uiState.value.prompt)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun onVoiceError_setsErrorMessage() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onVoiceError("Speech recognition isn't available on this device.")

        assertEquals(
            "Speech recognition isn't available on this device.",
            viewModel.uiState.value.errorMessage
        )
    }
}

