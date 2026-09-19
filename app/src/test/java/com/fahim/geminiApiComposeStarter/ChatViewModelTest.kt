package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeGeminiRepository(
        var shouldReturnSuccess: Boolean = true,
        var responseText: String = "Test response"
    ) : GeminiRepository {
        override suspend fun generateText(prompt: String): Result<String> {
            return if (shouldReturnSuccess) {
                Result.success(responseText)
            } else {
                Result.failure(RuntimeException("API Error"))
            }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSendEmptyPromptSetsError() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository, null, hasApiKey = true)

        viewModel.onPromptChange("")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun testSendSuccessUpdatesMessages() = runTest {
        val repository = FakeGeminiRepository(shouldReturnSuccess = true, responseText = "Hello from AI")
        val viewModel = ChatViewModel(repository, null, hasApiKey = true)

        viewModel.onPromptChange("Hello AI")
        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun testSendFailureSetsErrorMessage() = runTest {
        val repository = FakeGeminiRepository(shouldReturnSuccess = false)
        val viewModel = ChatViewModel(repository, null, hasApiKey = true)

        viewModel.onPromptChange("Hello AI")
        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("API Error", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun testMissingApiKeySetsErrorMessage() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository, null, hasApiKey = false)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testOnErrorShownClearsError() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository, null, hasApiKey = false)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)

        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
