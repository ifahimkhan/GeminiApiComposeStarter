package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.Participant
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeGeminiRepository(
    var result: Result<String> = Result.success("Hello from fake Gemini!"),
) : GeminiRepository {
    var lastPrompt: String? = null

    override suspend fun generateText(prompt: String): Result<String> {
        lastPrompt = prompt
        return result
    }
}

class FakeChatMessageDao : ChatMessageDao {
    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

    override fun insertMessage(message: ChatMessageEntity) {
        messagesFlow.value = messagesFlow.value + message
    }

    override fun insertMessages(messages: List<ChatMessageEntity>) {
        messagesFlow.value = messagesFlow.value + messages
    }

    override fun deleteMessage(id: String) {
        messagesFlow.value = messagesFlow.value.filterNot { it.id == id }
    }

    override fun clearAll() {
        messagesFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialState_isEmpty() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals("", state.response)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("How is the weather?")
        assertEquals("How is the weather?", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onVoiceInputResult_updatesPrompt() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onVoiceInputResult("Voice prompt test")
        assertEquals("Voice prompt test", viewModel.uiState.value.prompt)

        viewModel.onVoiceInputResult("additional words")
        assertEquals("Voice prompt test additional words", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyError() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(fakeRepo.lastPrompt)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = false,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("Hello AI")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_success_insertsMessagesAndUpdatesState() = runTest {
        val fakeRepo = FakeGeminiRepository(result = Result.success("Generated response"))
        val fakeDao = FakeChatMessageDao()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatDao = fakeDao,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)
        assertEquals("Generated response", viewModel.uiState.value.response)
        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Tell me a joke", viewModel.uiState.value.messages[0].text)
        assertEquals(Participant.USER, viewModel.uiState.value.messages[0].participant)
        assertEquals("Generated response", viewModel.uiState.value.messages[1].text)
        assertEquals(Participant.GEMINI, viewModel.uiState.value.messages[1].participant)
    }

    @Test
    fun onSend_failure_setsErrorMessageAndStopsLoading() = runTest {
        val fakeRepo = FakeGeminiRepository(result = Result.failure(RuntimeException("Network timeout")))
        val fakeDao = FakeChatMessageDao()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatDao = fakeDao,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("Will it fail?")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network timeout", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun onClearHistory_clearsMessages() = runTest {
        val fakeRepo = FakeGeminiRepository()
        val fakeDao = FakeChatMessageDao()
        fakeDao.insertMessage(
            ChatMessageEntity(text = "Previous message", participant = Participant.USER)
        )
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatDao = fakeDao,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        viewModel.onClearHistory()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onDismissError_clearsErrorMessage() {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = false,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )

        viewModel.onPromptChange("Query")
        viewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
