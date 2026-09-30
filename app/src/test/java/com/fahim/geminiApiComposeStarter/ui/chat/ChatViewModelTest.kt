package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
    fun `onPromptChange updates prompt state`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)

        viewModel.onPromptChange("Hello World")

        assertEquals("Hello World", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun `onVoiceResult appends spoken text to prompt`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onVoiceResult("Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets promptError`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)

        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend without API key sets errorMessage`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Hi")

        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend success sends message and updates chat history`() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Tell me a joke")

        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Tell me a joke", viewModel.uiState.value.messages[0].text)
        assertEquals("Fake Gemini Response", viewModel.uiState.value.messages[1].text)
    }

    @Test
    fun `onSend failure sets errorMessage`() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Quota Exceeded"
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello")

        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Quota Exceeded", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onClearHistory clears stored messages`() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isNotEmpty())

        viewModel.onClearHistory()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
