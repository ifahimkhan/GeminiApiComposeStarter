package com.fahim.geminiApiComposeStarter.ui.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = true,
            chatDao = null,
        )
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, fakeRepository.callCount)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() {
        val vmWithoutKey = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
            chatDao = null,
        )
        vmWithoutKey.onPromptChange("Tell me a joke")
        vmWithoutKey.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            vmWithoutKey.uiState.value.errorMessage,
        )
        assertEquals(0, fakeRepository.callCount)
    }

    @Test
    fun onSend_success_addsMessagesAndUpdatesResponse() = runTest {
        fakeRepository.resultToReturn = Result.success("Why did the chicken cross the road?")
        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Why did the chicken cross the road?", state.response)
        assertEquals(2, state.messages.size)

        // Verify User message
        assertEquals(MessageSender.USER, state.messages[0].sender)
        assertEquals("Tell me a joke", state.messages[0].text)

        // Verify Gemini message
        assertEquals(MessageSender.GEMINI, state.messages[1].sender)
        assertEquals("Why did the chicken cross the road?", state.messages[1].text)
        assertFalse(state.messages[1].isError)

        assertEquals("Tell me a joke", fakeRepository.lastPromptReceived)
    }

    @Test
    fun onSend_failure_setsErrorMessageAndAddsErrorMessage() = runTest {
        val expectedError = "Quota exceeded"
        fakeRepository.resultToReturn = Result.failure(RuntimeException(expectedError))
        viewModel.onPromptChange("Generate something")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(expectedError, state.errorMessage)
        assertEquals(2, state.messages.size)

        // Verify Error message
        val geminiErrorMsg = state.messages[1]
        assertEquals(MessageSender.GEMINI, geminiErrorMsg.sender)
        assertEquals(expectedError, geminiErrorMsg.text)
        assertTrue(geminiErrorMsg.isError)
    }

    @Test
    fun onVoiceInput_populatesAndAppendsPrompt() {
        viewModel.onVoiceInput("First part")
        assertEquals("First part", viewModel.uiState.value.prompt)

        viewModel.onVoiceInput("second part")
        assertEquals("First part second part", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onVoiceInput_blankString_isIgnored() {
        viewModel.onPromptChange("Existing text")
        viewModel.onVoiceInput("   ")
        assertEquals("Existing text", viewModel.uiState.value.prompt)
    }

    @Test
    fun onRetry_resendsPromptSuccessfully() = runTest {
        fakeRepository.resultToReturn = Result.success("Retried answer")
        viewModel.onRetry("Retry this query")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Retried answer", state.response)
        assertEquals("Retry this query", fakeRepository.lastPromptReceived)
    }

    @Test
    fun onClearHistory_clearsMessagesAndResponse() = runTest {
        fakeRepository.resultToReturn = Result.success("Answer")
        viewModel.onPromptChange("Question")
        viewModel.onSend()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.response)
        assertNull(state.errorMessage)
    }

    @Test
    fun onToggleTheme_togglesLightAndDarkModeCorrectly() {
        assertNull(viewModel.uiState.value.isDarkTheme)

        // If system is currently dark, toggling switches to light mode (false)
        viewModel.onToggleTheme(isCurrentSystemDark = true)
        assertEquals(false, viewModel.uiState.value.isDarkTheme)

        // Toggling again switches to dark mode (true)
        viewModel.onToggleTheme(isCurrentSystemDark = false)
        assertEquals(true, viewModel.uiState.value.isDarkTheme)

        // Toggling again switches back to light mode (false)
        viewModel.onToggleTheme(isCurrentSystemDark = true)
        assertEquals(false, viewModel.uiState.value.isDarkTheme)
    }
}

