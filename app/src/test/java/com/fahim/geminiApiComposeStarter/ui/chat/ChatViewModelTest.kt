package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.ConversationMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.StoredChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initializesWithNewChatWhenStorageIsEmpty() = runTest(dispatcher) {
        val viewModel = ChatViewModel(
            repo = FakeGeminiRepository("unused"),
            storage = FakeChatStorage(),
            hasApiKey = true,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.activeChatId.isNotBlank())
        assertEquals(listOf(ChatSummary(state.activeChatId, "New chat")), state.chatSummaries)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun emptyPromptShowsValidationError() = runTest(dispatcher) {
        val viewModel = ChatViewModel(
            repo = FakeGeminiRepository("unused"),
            storage = FakeChatStorage(),
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun sendStreamsGeminiResponseAndPersistsChat() = runTest(dispatcher) {
        val storage = FakeChatStorage()
        val viewModel = ChatViewModel(
            repo = FakeGeminiRepository("Hi there"),
            storage = storage,
            hasApiKey = true,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Hello", state.messages[0].text)
        assertEquals(ChatAuthor.USER, state.messages[0].author)
        assertEquals("Hi there", state.messages[1].text)
        assertEquals(ChatAuthor.GEMINI, state.messages[1].author)
        assertEquals("Hello", state.chatSummaries.first().title)
        assertEquals(2, storage.savedChats.first().messages.size)
    }
}

private class FakeGeminiRepository(
    private val response: String,
) : GeminiRepository {
    override fun generateTextStream(
        prompt: String,
        history: List<ConversationMessage>,
    ): Flow<Result<String>> = flow {
        emit(Result.success(response))
    }
}

private class FakeChatStorage : ChatStorage {
    var savedChats: List<StoredChat> = emptyList()
        private set
    private var activeChatId: String? = null

    override suspend fun loadChats(): List<StoredChat> = savedChats

    override suspend fun saveChats(chats: List<StoredChat>, activeChatId: String) {
        savedChats = chats
        this.activeChatId = activeChatId
    }

    override suspend fun loadActiveChatId(): String? = activeChatId
}
