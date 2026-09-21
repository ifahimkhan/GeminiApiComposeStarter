package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
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

    private val testDispatcher =
        StandardTestDispatcher()

    private lateinit var repository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = FakeGeminiRepository()

        viewModel = ChatViewModel(
            repository = repository,
            hasApiKey = true,
            chatMessageDao = FakeChatMessageDao()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyPrompt_showsError() = runTest {
        viewModel.onPromptChange("")

        viewModel.onSend()

        advanceUntilIdle()

        assertEquals(
            PromptError.EMPTY,
            viewModel.uiState.value.promptError
        )
    }

    @Test
    fun successfulPrompt_addsMessages() = runTest {
        repository.response = "Hello from Gemini"

        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)

        assertEquals(
            "",
            state.prompt
        )

        assertTrue(
            state.messages.any {
                it.text == "Hello" && it.isUser
            }
        )

        assertTrue(
            state.messages.any {
                it.text == "Hello from Gemini" &&
                        !it.isUser
            }
        )
    }

    @Test
    fun apiError_setsErrorMessage() = runTest {
        repository.error =
            IllegalStateException("API error")

        viewModel.onPromptChange("Hello")

        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)

        assertEquals(
            "API error",
            state.errorMessage
        )
    }

    @Test
    fun missingApiKey_showsError() = runTest {

        val viewModelWithoutKey =
            ChatViewModel(
                repository = repository,
                hasApiKey = false,
                chatMessageDao = FakeChatMessageDao()
            )

        viewModelWithoutKey.onPromptChange("Hello")

        viewModelWithoutKey.onSend()

        advanceUntilIdle()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            viewModelWithoutKey
                .uiState
                .value
                .errorMessage
        )
    }
}


/**
 * Fake Gemini repository.
 * No real Gemini API request is made during testing.
 */
private class FakeGeminiRepository :
    GeminiRepository {

    var response: String = "Test response"

    var error: Throwable? = null

    override suspend fun generateText(
        prompt: String
    ): Result<String> {

        if (error != null) {
            return Result.failure(error!!)
        }

        return Result.success(response)
    }
}


/**
 * Fake Room DAO.
 * Messages are stored only in memory during testing.
 */
private class FakeChatMessageDao :
    ChatMessageDao {

    private val messages =
        MutableStateFlow<List<ChatMessageEntity>>(
            emptyList()
        )

    override suspend fun insertMessage(
        message: ChatMessageEntity
    ) {
        messages.value =
            messages.value +
                    message.copy(
                        id =
                            messages.value.size
                                .toLong() + 1
                    )
    }

    override fun getAllMessages():
            Flow<List<ChatMessageEntity>> {

        return messages
    }

    override suspend fun deleteAllMessages() {
        messages.value = emptyList()
    }
}