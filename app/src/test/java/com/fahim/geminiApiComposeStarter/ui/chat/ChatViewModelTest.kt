package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.MainDispatcherRule
import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.data.history.FakeChatHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        geminiRepository: FakeGeminiRepository = FakeGeminiRepository(),
        historyRepository: FakeChatHistoryRepository = FakeChatHistoryRepository(),
        hasApiKey: Boolean = true,
    ) = ChatViewModel(
        geminiRepository = geminiRepository,
        historyRepository = historyRepository,
        hasApiKey = hasApiKey,
    )

    @Test
    fun `sending a message stores the user message and the Gemini reply`() = runTest {
        val viewModel = buildViewModel(geminiRepository = FakeGeminiRepository(response = "Hi there!"))

        viewModel.onInputChange("Hello Gemini")
        viewModel.onSend()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals("Hello Gemini", messages[0].text)
        assertFalse(messages[1].isFromUser)
        assertEquals("Hi there!", messages[1].text)
    }

    @Test
    fun `input field is cleared and loading finishes after a successful send`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onInputChange("What is Kotlin?")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `a failed Gemini call surfaces an error message and allows retry`() = runTest {
        val failingRepository = FakeGeminiRepository(shouldFail = true, failureMessage = "network down")
        val viewModel = buildViewModel(geminiRepository = failingRepository)

        viewModel.onInputChange("Will this work?")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("network down", state.errorMessage)
        assertTrue(state.canRetry)
        // The failed prompt is not persisted as an assistant message.
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `retry resends the last failed prompt without duplicating the user message`() = runTest {
        val failingRepository = FakeGeminiRepository(shouldFail = true)
        val viewModel = buildViewModel(geminiRepository = failingRepository)

        viewModel.onInputChange("Retry me")
        viewModel.onSend()
        assertEquals(1, failingRepository.callCount)
        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onRetry()

        assertEquals(2, failingRepository.callCount)
        assertEquals("Retry me", failingRepository.lastPrompt)
        // Still only one user message — retry doesn't re-add it to history.
        assertEquals(1, viewModel.uiState.value.messages.count { it.isFromUser })
    }

    @Test
    fun `sending an empty message shows a validation error and never calls Gemini`() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = buildViewModel(geminiRepository = repository)

        viewModel.onInputChange("   ")
        viewModel.onSend()

        assertEquals(0, repository.callCount)
        assertNotNull(viewModel.uiState.value.inputError)
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `missing API key blocks sending with a clear message`() = runTest {
        val repository = FakeGeminiRepository()
        val viewModel = buildViewModel(geminiRepository = repository, hasApiKey = false)

        viewModel.onInputChange("Hello")
        viewModel.onSend()

        assertEquals(0, repository.callCount)
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `selecting a suggestion sends it as a prompt`() = runTest {
        val repository = FakeGeminiRepository(response = "Sure, here's an explanation.")
        val viewModel = buildViewModel(geminiRepository = repository)

        viewModel.onSuggestionSelected("Explain quantum computing simply")

        assertEquals("Explain quantum computing simply", repository.lastPrompt)
        assertTrue(viewModel.uiState.value.messages.any { it.isFromUser && it.text == "Explain quantum computing simply" })
    }
}
