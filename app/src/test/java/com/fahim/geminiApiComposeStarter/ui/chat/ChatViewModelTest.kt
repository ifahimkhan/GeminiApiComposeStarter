package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeGeminiRepository : GeminiRepository {
        private val messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
        var shouldReturnError = false
        var errorMessage = "API Error"

        override suspend fun generateText(prompt: String): Result<String> {
            return if (shouldReturnError) {
                Result.failure(Exception(errorMessage))
            } else {
                Result.success("Echo: $prompt")
            }
        }

        override fun getMessages(): Flow<List<ChatMessage>> = messagesFlow

        override suspend fun saveMessage(message: ChatMessage) {
            messagesFlow.value = messagesFlow.value + message
        }

        override suspend fun clearMessages() {
            messagesFlow.value = emptyList()
        }
    }

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

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
    fun initial_state_is_empty() = runTest {
        viewModel = ChatViewModel(fakeRepository, null, hasApiKey = true)
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertTrue(!state.isLoading)
    }

    @Test
    fun onPromptChange_updates_prompt() = runTest {
        viewModel = ChatViewModel(fakeRepository, null, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_empty_prompt_sets_error() = runTest {
        viewModel = ChatViewModel(fakeRepository, null, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_missing_api_key_sets_error_message() = runTest {
        viewModel = ChatViewModel(fakeRepository, null, hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_success_generates_and_saves_messages() = runTest {
        viewModel = ChatViewModel(fakeRepository, null, hasApiKey = true)
        viewModel.onPromptChange("Test prompt")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Test prompt", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Echo: Test prompt", state.messages[1].text)
        assertTrue(!state.messages[1].isUser)
        assertTrue(!state.isLoading)
    }
}
