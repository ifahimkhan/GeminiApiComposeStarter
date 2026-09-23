package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant
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
        viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isEmpty() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals("", state.response)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPrompt() {
        viewModel.onPromptChange("What is Compose?")
        assertEquals("What is Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_triggersEmptyError() {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(fakeRepository.receivedPrompts.isEmpty())
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() {
        val noKeyViewModel = ChatViewModel(repository = fakeRepository, hasApiKey = false)
        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
        assertTrue(fakeRepository.receivedPrompts.isEmpty())
    }

    @Test
    fun onSend_success_addsMessagesAndClearsPrompt() = runTest(testDispatcher) {
        fakeRepository.simulatedResult = Result.success("Jetpack Compose is Android's modern toolkit.")
        viewModel.onPromptChange("Tell me about Compose")
        viewModel.onSend()

        // Prompt field is immediately cleared
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Jetpack Compose is Android's modern toolkit.", viewModel.uiState.value.response)
        assertEquals(2, viewModel.uiState.value.messages.size)

        val userMessage = viewModel.uiState.value.messages[0]
        assertEquals("Tell me about Compose", userMessage.text)
        assertEquals(MessageParticipant.USER, userMessage.participant)

        val modelMessage = viewModel.uiState.value.messages[1]
        assertEquals("Jetpack Compose is Android's modern toolkit.", modelMessage.text)
        assertEquals(MessageParticipant.MODEL, modelMessage.participant)
    }

    @Test
    fun onSend_failure_setsErrorMessageAndClearsLoading() = runTest(testDispatcher) {
        fakeRepository.simulatedResult = Result.failure(RuntimeException("Network timeout"))
        viewModel.onPromptChange("Hi Gemini")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network timeout", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearChat_clearsMessagesAndResponse() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals("", viewModel.uiState.value.response)
    }

    @Test
    fun dismissError_clearsErrorMessage() {
        val noKeyViewModel = ChatViewModel(repository = fakeRepository, hasApiKey = false)
        noKeyViewModel.onPromptChange("test")
        noKeyViewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)

        noKeyViewModel.dismissError()
        assertNull(noKeyViewModel.uiState.value.errorMessage)
    }
}
