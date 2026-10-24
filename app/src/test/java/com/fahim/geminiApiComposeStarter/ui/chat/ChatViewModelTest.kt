package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
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
    fun initialState_isEmpty() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyPromptError() = runTest(testDispatcher) {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
        )
        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_success_savesMessagesAndUpdatesResponse() = runTest(testDispatcher) {
        fakeRepository.responseText = "Gemini response text"
        viewModel.onPromptChange("Tell me a fact")
        viewModel.onSend()

        // Before coroutines finish
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Gemini response text", viewModel.uiState.value.response)
        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Tell me a fact", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("Gemini response text", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.shouldFail = true
        fakeRepository.failureException = RuntimeException("Custom API Error")
        viewModel.onPromptChange("Hi")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Custom API Error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onClearChat_clearsMessagesAndResponse() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("", viewModel.uiState.value.response)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
