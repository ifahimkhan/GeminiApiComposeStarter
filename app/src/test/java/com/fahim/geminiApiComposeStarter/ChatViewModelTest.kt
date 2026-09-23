package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            userPreferencesRepository = null,
            hasApiKey = true,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isDefaultAndEmpty() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest {
        viewModel.onPromptChange("What is Jetpack Compose?")
        assertEquals("What is Jetpack Compose?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest {
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeRepository.messagesList.isEmpty())
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            hasApiKey = false,
        )
        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            noKeyViewModel.uiState.value.errorMessage,
        )
        assertFalse(noKeyViewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_validPrompt_success_updatesMessagesAndClearsLoading() = runTest {
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        // Check that prompt field was cleared and loading started
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        // After completion, loading is false and messages are populated
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Hello Gemini", viewModel.uiState.value.messages[0].text)
        assertEquals(Participant.USER, viewModel.uiState.value.messages[0].participant)
        assertEquals("Mock response from Gemini", viewModel.uiState.value.messages[1].text)
        assertEquals(Participant.GEMINI, viewModel.uiState.value.messages[1].participant)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Will fail")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network timeout error", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onVoiceInput_appendsSpokenText() = runTest {
        viewModel.onPromptChange("Hello")
        viewModel.onVoiceInput("world")

        assertEquals("Hello world", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onClearChat_clearsMessages() = runTest {
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Fail test")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("Network timeout error", viewModel.uiState.value.errorMessage)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

/**
 * Fake in-memory GeminiRepository implementation for unit testing.
 */
class FakeGeminiRepository(
    var shouldFail: Boolean = false,
    var mockResponse: String = "Mock response from Gemini",
) : GeminiRepository {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messagesStream: Flow<List<ChatMessage>> = _messages.asStateFlow()

    val messagesList: List<ChatMessage> get() = _messages.value

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(RuntimeException("Network timeout error"))
        } else {
            Result.success(mockResponse)
        }
    }

    override suspend fun sendMessage(prompt: String): Result<String> {
        val userMsg = ChatMessage(text = prompt, participant = Participant.USER)
        _messages.update { it + userMsg }

        val result = generateText(prompt)
        result.fold(
            onSuccess = { res ->
                val geminiMsg = ChatMessage(text = res, participant = Participant.GEMINI)
                _messages.update { it + geminiMsg }
            },
            onFailure = { err ->
                val errMsg = ChatMessage(
                    text = err.message ?: "Error",
                    participant = Participant.GEMINI,
                    isError = true,
                )
                _messages.update { it + errMsg }
            },
        )
        return result
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
