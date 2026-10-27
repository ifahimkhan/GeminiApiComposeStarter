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

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeGeminiRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeGeminiRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updating prompt clears existing promptError`() {
        val viewModel = ChatViewModel(repository, hasApiKey = true)
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        viewModel.onPromptChange("What is Jetpack Compose?")

        assertEquals("What is Jetpack Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun `voice input appends recognized phrase to existing prompt`() {
        val viewModel = ChatViewModel(repository, hasApiKey = true)

        viewModel.onPromptChange("Explain")
        viewModel.onVoiceInputReceived("Kotlin coroutines")

        assertEquals("Explain Kotlin coroutines", viewModel.uiState.value.prompt)
    }

    @Test
    fun `sending blank prompt triggers EMPTY validation error`() {
        val viewModel = ChatViewModel(repository, hasApiKey = true)

        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `sending when API key is missing surfaces configuration error`() {
        val viewModel = ChatViewModel(repository, hasApiKey = false)
        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `successful send appends user and assistant messages`() = runTest {
        val viewModel = ChatViewModel(repository, hasApiKey = true)
        viewModel.onPromptChange("Summarize Android Keystore")

        viewModel.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Summarize Android Keystore", viewModel.uiState.value.messages[0].content)
        assertEquals("Mock Gemini reply", viewModel.uiState.value.messages[1].content)
    }

    @Test
    fun `failed send records errorMessage and allows retry`() = runTest {
        repository.simulateError = true
        repository.errorMessageText = "Network timeout"
        val viewModel = ChatViewModel(repository, hasApiKey = true)
        viewModel.onPromptChange("Retry me")

        viewModel.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network timeout", viewModel.uiState.value.errorMessage)

        repository.simulateError = false
        viewModel.onRetryLastPrompt()
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.messages.any { !it.fromUser })
    }

    @Test
    fun `clearing conversation removes all persisted messages`() = runTest {
        val viewModel = ChatViewModel(repository, hasApiKey = true)
        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isNotEmpty())

        viewModel.onClearConversation()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
