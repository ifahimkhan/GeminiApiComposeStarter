package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private val fakeRepository = object : GeminiRepository {
        var resultToReturn: Result<String> = Result.success("Fake response")
        override suspend fun generateText(prompt: String): Result<String> = resultToReturn
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
    fun `onSend with empty prompt sets promptError`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `onSend without API key sets errorMessage`() {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onSend success updates response text`() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Fake response", viewModel.uiState.value.response)
    }
}
