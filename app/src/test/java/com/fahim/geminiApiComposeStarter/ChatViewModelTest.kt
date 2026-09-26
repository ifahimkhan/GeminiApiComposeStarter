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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository(var shouldReturnError: Boolean = false) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldReturnError) {
            Result.failure(RuntimeException("Network error"))
        } else {
            Result.success("Generated answer for: $prompt")
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
    fun promptChange_updatesStatePrompt() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun send_emptyPrompt_setsEmptyPromptError() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun send_missingApiKey_setsErrorMessage() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun send_successfulGeneration_updatesMessageList() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Explain Jetpack Compose")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("Explain Jetpack Compose", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Generated answer for: Explain Jetpack Compose", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
    }

    @Test
    fun send_failedGeneration_setsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Test query")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
    }
}
