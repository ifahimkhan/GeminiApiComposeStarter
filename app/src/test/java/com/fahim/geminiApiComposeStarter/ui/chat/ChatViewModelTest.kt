package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.MessageSender
import com.fahim.geminiApiComposeStarter.data.preferences.AiTone
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var fakeChatHistoryRepository: FakeChatHistoryRepository
    private lateinit var fakeUserPreferencesRepository: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        fakeGeminiRepository = FakeGeminiRepository()
        fakeChatHistoryRepository = FakeChatHistoryRepository()
        fakeUserPreferencesRepository = FakeUserPreferencesRepository()
    }

    private fun createViewModel(hasApiKey: Boolean = true): ChatViewModel {
        return ChatViewModel(
            geminiRepository = fakeGeminiRepository,
            chatHistoryRepository = fakeChatHistoryRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun initialState_loadsPersistedMessagesAndDefaultTone() = runTest {
        val initialMessage = ChatMessage(sender = MessageSender.USER, text = "Earlier query")
        fakeChatHistoryRepository.saveMessage(initialMessage)

        val viewModel = createViewModel()

        assertEquals(listOf(initialMessage), viewModel.uiState.value.messages)
        assertEquals(AiTone.BALANCED, viewModel.uiState.value.selectedTone)
        assertEquals("", viewModel.uiState.value.prompt)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.promptError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest {
        val viewModel = createViewModel()
        viewModel.onSend() // triggers prompt error EMPTY
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onVoiceInput_appendsTranscriptToPrompt() = runTest {
        val viewModel = createViewModel()

        viewModel.onVoiceInput("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)

        viewModel.onVoiceInput("world")
        assertEquals("Hello world", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() = runTest {
        val viewModel = createViewModel()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() = runTest {
        val viewModel = createViewModel(hasApiKey = false)

        viewModel.onPromptChange("Valid prompt")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_success_persistsUserAndGeminiMessages() = runTest {
        fakeGeminiRepository.resultToReturn = Result.success("Gemini answer")
        val viewModel = createViewModel(hasApiKey = true)

        viewModel.onPromptChange("Explain Kotlin")
        viewModel.onSend()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Explain Kotlin", messages[0].text)
        assertEquals(MessageSender.USER, messages[0].sender)
        assertEquals("Gemini answer", messages[1].text)
        assertEquals(MessageSender.GEMINI, messages[1].sender)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_failure_setsErrorMessageAndClearsLoading() = runTest {
        fakeGeminiRepository.resultToReturn = Result.failure(RuntimeException("Network timeout"))
        val viewModel = createViewModel(hasApiKey = true)

        viewModel.onPromptChange("Will fail")
        viewModel.onSend()

        assertEquals("Network timeout", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        // User message is preserved in history, prompt is cleared
        assertEquals(1, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onClearHistory_clearsMessages() = runTest {
        fakeChatHistoryRepository.saveMessage(ChatMessage(sender = MessageSender.USER, text = "Query"))
        val viewModel = createViewModel()
        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSelectTone_updatesSelectedTone() = runTest {
        val viewModel = createViewModel()

        viewModel.onSelectTone(AiTone.CREATIVE)

        assertEquals(AiTone.CREATIVE, viewModel.uiState.value.selectedTone)
    }
}

// Fake implementations for testing
private class FakeGeminiRepository : GeminiRepository {
    var resultToReturn: Result<String> = Result.success("Default response")
    var lastPrompt: String? = null
    var lastSystemInstruction: String? = null

    override suspend fun generateText(prompt: String, systemInstruction: String?): Result<String> {
        lastPrompt = prompt
        lastSystemInstruction = systemInstruction
        return resultToReturn
    }
}

private class FakeChatHistoryRepository : ChatHistoryRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val messages: Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun saveMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _tone = MutableStateFlow(AiTone.BALANCED)
    override val selectedToneFlow: Flow<AiTone> = _tone.asStateFlow()

    override suspend fun setAiTone(tone: AiTone) {
        _tone.value = tone
    }
}
