package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

    private val testDispatcher: TestDispatcher =
        StandardTestDispatcher()

    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var historyRepository: ChatHistoryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeGeminiRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        historyRepository = ChatHistoryRepository(fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyPrompt_setsEmptyPromptError() =
        runTest(testDispatcher) {

            val viewModel = createViewModel()

            advanceUntilIdle()

            viewModel.onPromptChange("")
            viewModel.onSend()

            advanceUntilIdle()

            assertEquals(
                PromptError.EMPTY,
                viewModel.uiState.value.promptError
            )
        }

    @Test
    fun validPrompt_savesUserAndGeminiMessages() =
        runTest(testDispatcher) {

            fakeGeminiRepository.result =
                Result.success("Hello from Gemini")

            val viewModel = createViewModel()

            advanceUntilIdle()

            viewModel.onPromptChange("Hello")
            viewModel.onSend()

            advanceUntilIdle()

            val state = viewModel.uiState.value

            assertEquals(2, state.messages.size)

            assertEquals(
                "Hello",
                state.messages[0].text
            )

            assertEquals(
                MessageSender.USER,
                state.messages[0].sender
            )

            assertEquals(
                "Hello from Gemini",
                state.messages[1].text
            )

            assertEquals(
                MessageSender.GEMINI,
                state.messages[1].sender
            )

            assertFalse(state.isLoading)
        }

    @Test
    fun successfulRequest_clearsPrompt() =
        runTest(testDispatcher) {

            fakeGeminiRepository.result =
                Result.success("Response")

            val viewModel = createViewModel()

            advanceUntilIdle()

            viewModel.onPromptChange("Test prompt")
            viewModel.onSend()

            advanceUntilIdle()

            assertEquals(
                "",
                viewModel.uiState.value.prompt
            )
        }

    @Test
    fun repositoryFailure_setsErrorMessage() =
        runTest(testDispatcher) {

            fakeGeminiRepository.result =
                Result.failure(
                    IllegalStateException("Network error")
                )

            val viewModel = createViewModel()

            advanceUntilIdle()

            viewModel.onPromptChange("Hello")
            viewModel.onSend()

            advanceUntilIdle()

            val state = viewModel.uiState.value

            assertEquals(
                "Network error",
                state.errorMessage
            )

            assertFalse(state.isLoading)
        }

    @Test
    fun missingApiKey_setsMissingKeyError() =
        runTest(testDispatcher) {

            val viewModel =
                createViewModel(
                    hasApiKey = false
                )

            advanceUntilIdle()

            viewModel.onPromptChange("Hello")
            viewModel.onSend()

            advanceUntilIdle()

            assertEquals(
                ChatViewModel.MISSING_API_KEY_MESSAGE,
                viewModel.uiState.value.errorMessage
            )
        }

    @Test
    fun clearHistory_removesStoredMessages() =
        runTest(testDispatcher) {

            fakeDao.insertMessage(
                ChatMessageEntity(
                    id = 1L,
                    text = "Old message",
                    sender = MessageSender.USER.name
                )
            )

            val viewModel = createViewModel()

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.messages.isNotEmpty()
            )

            viewModel.clearHistory()

            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.messages.isEmpty()
            )
        }

    private fun createViewModel(
        hasApiKey: Boolean = true
    ): ChatViewModel {
        return ChatViewModel(
            repository = fakeGeminiRepository,
            historyRepository = historyRepository,
            hasApiKey = hasApiKey
        )
    }
}

private class FakeGeminiRepository :
    GeminiRepository {

    var result: Result<String> =
        Result.success("Fake Gemini response")

    override suspend fun generateText(
        prompt: String
    ): Result<String> {
        return result
    }
}

private class FakeChatMessageDao :
    ChatMessageDao {

    private val messages =
        MutableStateFlow<List<ChatMessageEntity>>(
            emptyList()
        )

    override fun getAllMessages():
            Flow<List<ChatMessageEntity>> {
        return messages
    }

    override suspend fun insertMessage(
        message: ChatMessageEntity
    ) {
        messages.update {
            it + message
        }
    }

    override suspend fun clearMessages() {
        messages.value = emptyList()
    }
}