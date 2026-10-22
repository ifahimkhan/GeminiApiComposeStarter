package com.example.geminiapicomposestarter

import com.example.geminiapicomposestarter.data.ChatMessage
import com.example.geminiapicomposestarter.data.GeminiRepository
import com.example.geminiapicomposestarter.data.Participant
import com.example.geminiapicomposestarter.ui.chat.ChatViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    class FakeGeminiRepository : GeminiRepository() {
        var shouldReturnError = false
        var responseToReturn = "Mocked response from Gemini"

        override suspend fun sendMessage(history: List<ChatMessage>, prompt: String): Result<String> {
            return if (shouldReturnError) {
                Result.failure(RuntimeException("Network error connecting to Gemini"))
            } else {
                Result.success(responseToReturn)
            }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeGeminiRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(geminiRepository = fakeGeminiRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatePrompt_updatesStatePrompt() {
        viewModel.updatePrompt("Hello Google")
        assertEquals("Hello Google", viewModel.uiState.value.currentPrompt)
    }

    @Test
    fun toggleDarkMode_updatesThemeState() {
        viewModel.toggleDarkMode(true)
        assertTrue(viewModel.uiState.value.isDarkMode)

        viewModel.toggleDarkMode(false)
        assertFalse(viewModel.uiState.value.isDarkMode)
    }

    @Test
    fun sendMessage_withBlankPrompt_setsError() {
        viewModel.sendMessage("   ")
        assertEquals("Prompt cannot be empty.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun sendMessage_success_addsUserAndModelMessages() = runTest(testDispatcher) {
        viewModel.sendMessage("Hello Gemini")
        // Check immediate state
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Hello Gemini", viewModel.uiState.value.messages[0].text)
        assertEquals(Participant.USER, viewModel.uiState.value.messages[0].participant)

        advanceUntilIdle()

        // Check after completion
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Mocked response from Gemini", viewModel.uiState.value.messages[1].text)
        assertEquals(Participant.MODEL, viewModel.uiState.value.messages[1].participant)
    }

    @Test
    fun sendMessage_failure_setsErrorMessage() = runTest(testDispatcher) {
        fakeGeminiRepository.shouldReturnError = true
        viewModel.sendMessage("Test failure")

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Network error"))
    }
}
