package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.fakes.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.fakes.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.fakes.FakeUserPreferencesRepository
import com.fahim.geminiApiComposeStarter.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var fakeChatHistoryRepository: FakeChatHistoryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeGeminiRepository = FakeGeminiRepository()
        fakeChatHistoryRepository = FakeChatHistoryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()

        viewModel = ChatViewModel(
            repository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun initialState_hasCorrectDefaults() {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.isApiKeyConfigured)
    }

    @Test
    fun successfulSend_addsUserMessageAndGeminiReply() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.success("Gemini answer")

        viewModel.onPromptChange("What is Kotlin?")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("What is Kotlin?", messages[0].text)
        assertTrue(messages[0].isUser)

        assertEquals("Gemini answer", messages[1].text)
        assertFalse(messages[1].isUser)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun send_isLoadingIsTrueWhileWaitingAndFalseAfter() = runTest(testDispatcher) {
        fakeGeminiRepository.responseDelayMs = 1000L
        fakeGeminiRepository.responseResult = Result.success("Delayed response")

        viewModel.onPromptChange("Tell me a story")
        viewModel.onSend()

        // Before coroutines finish execution
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        // Advance past delay
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun error_setsErrorMessageAndClearsItAfterHandled() = runTest(testDispatcher) {
        fakeGeminiRepository.responseResult = Result.failure(RuntimeException("API error occurred"))

        viewModel.onPromptChange("Fail prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("API error occurred", state.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun blankInput_isIgnored() = runTest(testDispatcher) {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PromptError.EMPTY, state.promptError)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun clearChat_clearsAllMessagesFromHistory() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onConfirmClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
