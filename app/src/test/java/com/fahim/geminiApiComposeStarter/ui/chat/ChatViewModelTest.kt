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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {

        Dispatchers.setMain(testDispatcher)

        repository = FakeGeminiRepository()

        viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
            historyRepository = null
        )
    }

    @After
    fun tearDown() {

        Dispatchers.resetMain()
    }

    @Test
    fun emptyPromptShowsError() = runTest {

        viewModel.onPromptChange("")

        viewModel.onSend()

        val state = viewModel.uiState.value

        assertEquals(
            PromptError.EMPTY,
            state.promptError
        )

        assertFalse(state.isLoading)
    }

    @Test
    fun validPromptGeneratesResponse() = runTest {

        repository.response =
            "Hello! I am Gemini."

        viewModel.onPromptChange(
            "Hello Gemini"
        )

        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(
            2,
            state.messages.size
        )

        assertEquals(
            "Hello Gemini",
            state.messages[0].text
        )

        assertTrue(
            state.messages[0].isUser
        )

        assertEquals(
            "Hello! I am Gemini.",
            state.messages[1].text
        )

        assertFalse(
            state.messages[1].isUser
        )

        assertFalse(
            state.isLoading
        )
    }

    @Test
    fun promptIsClearedAfterSending() = runTest {

        viewModel.onPromptChange(
            "Tell me about AI"
        )

        viewModel.onSend()

        advanceUntilIdle()

        assertEquals(
            "",
            viewModel.uiState.value.prompt
        )
    }

    @Test
    fun missingApiKeyShowsError() = runTest {

        viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = false,
            historyRepository = null
        )

        viewModel.onPromptChange(
            "Hello"
        )

        viewModel.onSend()

        val state = viewModel.uiState.value

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            state.errorMessage
        )

        assertFalse(
            state.isLoading
        )
    }

    @Test
    fun repositoryFailureShowsError() = runTest {

        repository.shouldFail = true

        viewModel.onPromptChange(
            "Hello"
        )

        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(
            "Something went wrong. Please try again.",
            state.errorMessage
        )

        assertFalse(
            state.isLoading
        )
    }

    @Test
    fun loadingBecomesFalseAfterRequest() = runTest {

        viewModel.onPromptChange(
            "What is Kotlin?"
        )

        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(
            viewModel.uiState.value.isLoading
        )
    }

    @Test
    fun clearErrorRemovesErrors() = runTest {

        viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = false,
            historyRepository = null
        )

        viewModel.onPromptChange(
            "Hello"
        )

        viewModel.onSend()

        assertTrue(
            viewModel.uiState.value.errorMessage != null
        )

        viewModel.clearError()

        assertEquals(
            null,
            viewModel.uiState.value.errorMessage
        )

        assertEquals(
            null,
            viewModel.uiState.value.promptError
        )
    }

    /*
     * Fake repository.
     * It does NOT call the real Gemini API.
     */
    private class FakeGeminiRepository : GeminiRepository {

        var response =
            "This is a test Gemini response."

        var shouldFail = false

        override suspend fun generateText(
            prompt: String
        ): Result<String> {

            return if (shouldFail) {

                Result.failure(
                    RuntimeException("Test error")
                )

            } else {

                Result.success(response)
            }
        }
    }
}