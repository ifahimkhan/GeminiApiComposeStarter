package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.Participant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
        viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyMessagesAndDefaultValues() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.hasApiKey)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_whenNoApiKey_setsMissingApiKeyError() = runTest {
        val noKeyViewModel = ChatViewModel(repository = fakeRepository, hasApiKey = false)
        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
        assertFalse(noKeyViewModel.uiState.value.isLoading)
        assertTrue(noKeyViewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_successfulResponse_addsMessagesAndClearsLoading() = runTest {
        fakeRepository.responseResult = Result.success("Hello, human!")

        viewModel.onPromptChange("Hello!")
        viewModel.onSend()

        // Before coroutines finish running
        testDispatcher.scheduler.runCurrent()

        // Advance coroutines
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
        assertEquals(2, state.messages.size)
        assertEquals("Hello!", state.messages[0].text)
        assertEquals(Participant.USER, state.messages[0].participant)
        assertEquals("Hello, human!", state.messages[1].text)
        assertEquals(Participant.GEMINI, state.messages[1].participant)
        assertEquals("Hello, human!", state.response)
        assertNull(state.errorMessage)
    }

    @Test
    fun onSend_failureResponse_setsErrorMessageAndClearsLoading() = runTest {
        fakeRepository.responseResult = Result.failure(Exception("Network error"))

        viewModel.onPromptChange("Will fail")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.messages.size) // User message was added
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun onSend_loadingState_preventsDuplicateCalls() = runTest {
        fakeRepository.delayMs = 1000L
        fakeRepository.responseResult = Result.success("Delayed response")

        viewModel.onPromptChange("First prompt")
        viewModel.onSend()

        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt sending second prompt while first is processing
        viewModel.onPromptChange("Second prompt")
        viewModel.onSend()

        advanceUntilIdle()

        // Only the first prompt should have been processed
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("First prompt", viewModel.uiState.value.messages[0].text)
    }

    @Test
    fun onClearChat_removesAllMessages() = runTest {
        fakeRepository.responseResult = Result.success("Response")
        viewModel.onPromptChange("Message")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        fakeRepository.responseResult = Result.failure(Exception("Error occurred"))
        viewModel.onPromptChange("Test")
        viewModel.onSend()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

/**
 * Fake GeminiRepository for reliable, fast ViewModel unit testing.
 */
class FakeGeminiRepository(
    var responseResult: Result<String> = Result.success("Default fake response"),
    var delayMs: Long = 0L
) : GeminiRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        if (delayMs > 0) {
            delay(delayMs)
        }
        return responseResult
    }
}
