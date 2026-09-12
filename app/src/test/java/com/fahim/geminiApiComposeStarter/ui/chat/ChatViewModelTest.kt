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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onSend_savesUserAndGeminiMessages() = runTest {

        val fakeDao = FakeChatMessageDao()

        val historyRepository =
            ChatHistoryRepository(fakeDao)

        val geminiRepository =
            FakeGeminiRepository(
                response = "Hello from Gemini"
            )

        val viewModel = ChatViewModel(
            repository = geminiRepository,
            chatHistoryRepository = historyRepository,
            hasApiKey = true
        )

        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages

        assertEquals(2, messages.size)

        assertEquals(
            "Hello",
            messages[0].text
        )

        assertEquals(
            true,
            messages[0].isUser
        )

        assertEquals(
            "Hello from Gemini",
            messages[1].text
        )

        assertEquals(
            false,
            messages[1].isUser
        )
    }
}

private class FakeGeminiRepository(
    private val response: String
) : GeminiRepository {

    override suspend fun generateText(
        prompt: String
    ): Result<String> {
        return Result.success(response)
    }
}

private class FakeChatMessageDao : ChatMessageDao {

    private val messages =
        MutableStateFlow<List<ChatMessageEntity>>(
            emptyList()
        )

    private var nextId = 1L

    override fun getAllMessages():
            Flow<List<ChatMessageEntity>> {
        return messages
    }

    override suspend fun insertMessage(
        message: ChatMessageEntity
    ) {
        val storedMessage =
            message.copy(
                id = nextId++
            )

        messages.update {
            it + storedMessage
        }
    }

    override suspend fun clearMessages() {
        messages.value = emptyList()
    }
}