package com.rannbir.geminiApiComposeStarter.ui.chat

import com.rannbir.geminiApiComposeStarter.data.FakeGeminiRepository
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasEmptyPromptAndNoErrors() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = false)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_success_clearsPromptAndUpdatesMessages() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        assertEquals("", viewModel.uiState.value.prompt)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Tell me a joke", viewModel.uiState.value.messages[0].text)
        assertTrue(viewModel.uiState.value.messages[0].isFromUser)
        assertEquals("This is a response from Fake Gemini", viewModel.uiState.value.messages[1].text)
        assertFalse(viewModel.uiState.value.messages[1].isFromUser)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.shouldFail = true
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Query that fails")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Simulated Gemini API error"))
    }

    @Test
    fun onVoiceInputResult_appendsText() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)

        viewModel.onVoiceInputResult("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)

        viewModel.onVoiceInputResult("world")
        assertEquals("Hello world", viewModel.uiState.value.prompt)
    }

    @Test
    fun onClearChat_clearsMessagesInMemory() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("First question")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}
