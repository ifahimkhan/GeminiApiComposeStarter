package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.fakes.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.fakes.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.fakes.FakeUserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var geminiRepository: FakeGeminiRepository
    private lateinit var historyRepository: FakeChatHistoryRepository
    private lateinit var preferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        geminiRepository = FakeGeminiRepository()
        historyRepository = FakeChatHistoryRepository()
        preferencesRepository = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(hasApiKey: Boolean = true) = ChatViewModel(
        repository = geminiRepository,
        historyRepository = historyRepository,
        userPreferencesRepository = preferencesRepository,
        hasApiKey = hasApiKey,
    )

    @Test
    fun `sending an empty prompt surfaces a validation error and does not call the repository`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertNull(geminiRepository.lastPrompt)
    }

    @Test
    fun `sending without an api key surfaces the missing key message`() = runTest(dispatcher) {
        val viewModel = viewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(geminiRepository.lastPrompt)
    }

    @Test
    fun `successful send appends the user prompt and the model reply to history`() = runTest(dispatcher) {
        geminiRepository.response = Result.success("**Hi there**")
        val viewModel = viewModel()

        viewModel.onPromptChange("hello gemini")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[0].isFromUser)
        assertEquals("hello gemini", state.messages[0].text)
        assertEquals("**Hi there**", state.messages[1].text)
        assertEquals(false, state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("", state.prompt)
    }

    @Test
    fun `failed send surfaces the error message and keeps only the user prompt in history`() = runTest(dispatcher) {
        geminiRepository.response = Result.failure(IllegalStateException("network down"))
        val viewModel = viewModel()

        viewModel.onPromptChange("hello gemini")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("network down", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `onVoiceResult populates the prompt field`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onVoiceResult("what is jetpack compose")

        assertEquals("what is jetpack compose", viewModel.uiState.value.prompt)
    }
}
