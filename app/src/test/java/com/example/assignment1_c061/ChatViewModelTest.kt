package com.example.assignment1_c061

import com.example.assignment1_c061.data.GeminiRepository
import com.example.assignment1_c061.ui.chat.ChatViewModel
import com.example.assignment1_c061.ui.chat.MessageSender
import com.example.assignment1_c061.ui.chat.PromptError
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

class FakeGeminiRepository : GeminiRepository {
    var shouldReturnError = false
    var responseText = "Hello from Fake Gemini"

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldReturnError) {
            Result.failure(Exception("API Error"))
        } else {
            Result.success(responseText)
        }
    }
}

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
    fun `initial state is empty`() {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        val state = viewModel.uiState.value

        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPromptChange updates prompt in state`() {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello World")

        assertEquals("Hello World", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets promptError`() {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend without API key sets error message`() {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend success appends user and Gemini messages`() = runTest {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("What is Compose?")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals(MessageSender.USER, state.messages[0].sender)
        assertEquals("What is Compose?", state.messages[0].text)
        assertEquals(MessageSender.GEMINI, state.messages[1].sender)
        assertEquals("Hello from Fake Gemini", state.messages[1].text)
    }

    @Test
    fun `onSend failure sets error message`() = runTest {
        fakeRepository.shouldReturnError = true
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Error Test")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API Error", state.errorMessage)
    }
}
