package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.MainDispatcherRule
import com.fahim.geminiApiComposeStarter.data.FakeChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.FakeGeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.MessageSender
import com.fahim.geminiApiComposeStarter.data.model.MessageStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var fakeChatHistoryRepository: FakeChatHistoryRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeGeminiRepository = FakeGeminiRepository()
        fakeChatHistoryRepository = FakeChatHistoryRepository()
        viewModel = ChatViewModel(
            geminiRepository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            hasApiKey = true,
        )
    }

    @Test
    fun initialUiState_isDefaultAndEmpty() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
        assertFalse(state.canRetry)
        assertNull(state.lastFailedPrompt)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.hasApiKey)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)

        viewModel.onPromptChange("")
        assertEquals("", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, fakeGeminiRepository.callCount)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() {
        val noKeyViewModel = ChatViewModel(
            geminiRepository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            hasApiKey = false,
        )

        noKeyViewModel.onPromptChange("Test prompt")
        noKeyViewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
        assertEquals(0, fakeGeminiRepository.callCount)
    }

    @Test
    fun onSend_successfulApiCall_addsMessagesAndClearsInput() = runTest {
        fakeGeminiRepository.customResponse = "Hello! I am Gemini."

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
        assertNull(state.errorMessage)
        assertFalse(state.canRetry)
        assertEquals(1, fakeGeminiRepository.callCount)
        assertEquals("Hi there", fakeGeminiRepository.lastPromptReceived)

        // Check messages: 1 USER message and 1 GEMINI message
        assertEquals(2, state.messages.size)
        val userMsg = state.messages[0]
        assertEquals("Hi there", userMsg.text)
        assertEquals(MessageSender.USER, userMsg.sender)
        assertEquals(MessageStatus.SUCCESS, userMsg.status)

        val botMsg = state.messages[1]
        assertEquals("Hello! I am Gemini.", botMsg.text)
        assertEquals(MessageSender.GEMINI, botMsg.sender)
        assertEquals(MessageStatus.SUCCESS, botMsg.status)
    }

    @Test
    fun onSend_apiFailure_setsErrorStateAndEnablesRetry() = runTest {
        fakeGeminiRepository.shouldFail = true
        fakeGeminiRepository.failureException = RuntimeException("Network timeout error")

        viewModel.onPromptChange("Will fail")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network timeout error", state.errorMessage)
        assertTrue(state.canRetry)
        assertEquals("Will fail", state.lastFailedPrompt)

        // Bot message should have ERROR status
        assertEquals(2, state.messages.size)
        val botMsg = state.messages[1]
        assertEquals(MessageSender.GEMINI, botMsg.sender)
        assertEquals(MessageStatus.ERROR, botMsg.status)
    }

    @Test
    fun onRetry_resendsLastFailedPrompt() = runTest {
        fakeGeminiRepository.shouldFail = true
        viewModel.onPromptChange("Retry me")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canRetry)
        assertEquals(1, fakeGeminiRepository.callCount)

        // Now fix the failure and retry
        fakeGeminiRepository.shouldFail = false
        fakeGeminiRepository.customResponse = "Recovered response"
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.canRetry)
        assertNull(state.lastFailedPrompt)
        assertNull(state.errorMessage)
        assertEquals(2, fakeGeminiRepository.callCount)
        assertEquals("Retry me", fakeGeminiRepository.lastPromptReceived)
    }

    @Test
    fun onVoiceInputResult_appendsToExistingPrompt() {
        viewModel.onPromptChange("Explain")
        viewModel.onVoiceInputResult("quantum computing")

        assertEquals("Explain quantum computing", viewModel.uiState.value.prompt)
    }

    @Test
    fun onVoiceInputResult_setsPromptWhenInitiallyEmpty() {
        viewModel.onVoiceInputResult("What is Android?")
        assertEquals("What is Android?", viewModel.uiState.value.prompt)
    }

    @Test
    fun onVoiceInputResult_ignoresBlankInput() {
        viewModel.onPromptChange("Initial")
        viewModel.onVoiceInputResult("   ")
        assertEquals("Initial", viewModel.uiState.value.prompt)
    }

    @Test
    fun onDismissError_clearsErrorMessage() {
        fakeGeminiRepository.shouldFail = true
        viewModel.onPromptChange("Trigger error")
        viewModel.onSend()

        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onShowClearConfirmDialog_togglesDialog() {
        assertFalse(viewModel.uiState.value.showClearConfirmDialog)
        viewModel.onShowClearConfirmDialog(true)
        assertTrue(viewModel.uiState.value.showClearConfirmDialog)
        viewModel.onShowClearConfirmDialog(false)
        assertFalse(viewModel.uiState.value.showClearConfirmDialog)
    }

    @Test
    fun onShowStudentInfoDialog_togglesDialog() {
        assertFalse(viewModel.uiState.value.showStudentInfoDialog)
        viewModel.onShowStudentInfoDialog(true)
        assertTrue(viewModel.uiState.value.showStudentInfoDialog)
        viewModel.onShowStudentInfoDialog(false)
        assertFalse(viewModel.uiState.value.showStudentInfoDialog)
    }

    @Test
    fun onClearChatHistory_clearsMessagesAndResetsDialog() = runTest {
        viewModel.onPromptChange("First message")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        viewModel.onShowClearConfirmDialog(true)

        viewModel.onClearChatHistory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.showClearConfirmDialog)
        assertNull(state.errorMessage)
        assertFalse(state.canRetry)
    }

    @Test
    fun onThemeChange_updatesThemeMode() {
        viewModel.onThemeChange("DARK")
        assertEquals("DARK", viewModel.uiState.value.themeMode)

        viewModel.onThemeChange("LIGHT")
        assertEquals("LIGHT", viewModel.uiState.value.themeMode)

        viewModel.onThemeChange("SYSTEM")
        assertEquals("SYSTEM", viewModel.uiState.value.themeMode)
    }

    @Test
    fun duplicateSend_isIgnoredWhileLoading() = runTest {
        // With simulated delay, keep request in-flight
        fakeGeminiRepository.simulatedDelayMs = 5000L

        viewModel.onPromptChange("First send")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, fakeGeminiRepository.callCount)

        // Try to send again while loading
        viewModel.onPromptChange("Second send")
        viewModel.onSend()

        // Call count should still be 1 (ignored)
        assertEquals(1, fakeGeminiRepository.callCount)
    }
}
