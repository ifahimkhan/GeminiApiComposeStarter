package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeGeminiRepository
    private lateinit var dao: FakeChatMessageDao
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = FakeGeminiRepository()
        dao = FakeChatMessageDao()

        viewModel = ChatViewModel(
            repository = repository,
            chatMessageDao = dao,
            hasApiKey = true,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyPrompt_showsValidationError() = runTest {
        viewModel.onPromptChange("   ")

        viewModel.onSend()

        advanceUntilIdle()

        assertEquals(
            PromptError.EMPTY,
            viewModel.uiState.value.promptError
        )

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun sendMessage_addsUserAndAssistantMessages() = runTest {
        repository.response = "Hello from Gemini"

        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages

        assertEquals(2, messages.size)

        assertEquals("Hello", messages[0].text)
        assertTrue(messages[0].isUser)

        assertEquals("Hello from Gemini", messages[1].text)
        assertFalse(messages[1].isUser)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)
    }

    @Test
    fun repositoryFailure_showsErrorMessage() = runTest {
        repository.failure = Exception("Network error")

        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        advanceUntilIdle()

        assertEquals(
            "Network error",
            viewModel.uiState.value.errorMessage
        )

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun missingApiKey_showsApiKeyError() = runTest {
        val noKeyViewModel = ChatViewModel(
            repository = repository,
            chatMessageDao = dao,
            hasApiKey = false,
        )

        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()

        advanceUntilIdle()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            noKeyViewModel.uiState.value.errorMessage
        )
    }
}

private class FakeGeminiRepository : GeminiRepository {

    var response: String = "Fake Gemini response"
    var failure: Throwable? = null

    override suspend fun generateText(prompt: String): Result<String> {
        failure?.let {
            return Result.failure(it)
        }

        return Result.success(response)
    }
}

private class FakeChatMessageDao : ChatMessageDao {

    private val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun observeMessages(): Flow<List<ChatMessageEntity>> {
        return messages
    }

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messages.value = messages.value + message
    }

    override suspend fun insertMessages(
        messages: List<ChatMessageEntity>
    ) {
        this.messages.value += messages
    }

    override suspend fun clearMessages() {
        messages.value = emptyList()
    }
}