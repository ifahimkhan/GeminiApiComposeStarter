package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageDeliveryStatus
import com.fahim.geminiApiComposeStarter.data.local.room.SenderRole
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValues() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertTrue(state.hasApiKey)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsErrors() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")

        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, fakeRepository.sendCallCount)
    }

    @Test
    fun onSend_whenApiKeyMissing_setsMissingApiKeyError() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = false)
        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, fakeRepository.sendCallCount)
    }

    @Test
    fun onSend_withValidPrompt_triggersLoadingAndSucceeds() = runTest {
        fakeRepository.returnText = "Hello from Fake Gemini"
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        // Prompt input is cleared upon send
        assertEquals("", viewModel.uiState.value.prompt)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
        assertNull(state.lastFailedPrompt)
        assertEquals(2, state.messages.size)
        assertEquals("Hi there", state.messages[0].content)
        assertEquals(SenderRole.USER, state.messages[0].senderRole)
        assertEquals("Hello from Fake Gemini", state.messages[1].content)
        assertEquals(SenderRole.ASSISTANT, state.messages[1].senderRole)
    }

    @Test
    fun onSend_whenRepositoryFails_setsErrorMessageAndPreservesFailedPromptForRetry() = runTest {
        fakeRepository.shouldFail = true
        fakeRepository.failureMessage = "Rate limit exceeded"
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Tell me a story")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Rate limit exceeded", state.errorMessage)
        assertEquals("Tell me a story", state.lastFailedPrompt)
    }

    @Test
    fun onRetry_resendsFailedPrompt() = runTest {
        fakeRepository.shouldFail = true
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Calculate 2+2")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("Calculate 2+2", viewModel.uiState.value.lastFailedPrompt)

        // Fix failure condition and retry
        fakeRepository.shouldFail = false
        fakeRepository.returnText = "4"
        viewModel.onRetry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.lastFailedPrompt)
        assertEquals("4", state.messages.last().content)
    }

    @Test
    fun onSend_preventsDuplicateSendWhileLoading() = runTest {
        fakeRepository.delayMs = 500
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Prompt 1")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second send while first is in-flight
        viewModel.onPromptChange("Prompt 2")
        viewModel.onSend()

        advanceUntilIdle()

        assertEquals(1, fakeRepository.sendCallCount)
    }

    @Test
    fun onVoiceInputResult_updatesPromptCorrectly() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        viewModel.onVoiceInputResult("What is Jetpack Compose?")

        assertEquals("What is Jetpack Compose?", viewModel.uiState.value.prompt)

        // Appending additional speech
        viewModel.onVoiceInputResult("Explain state.")
        assertEquals("What is Jetpack Compose? Explain state.", viewModel.uiState.value.prompt)
    }

    @Test
    fun onClearChat_removesAllMessages() = runTest {
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest {
        fakeRepository.shouldFail = true
        val viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
        advanceUntilIdle()

        viewModel.onPromptChange("Fail")
        viewModel.onSend()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // Fake repository implementation for unit tests
    private class FakeChatRepository : ChatRepository {
        val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        var shouldFail: Boolean = false
        var failureMessage: String = "Simulated network failure"
        var returnText: String = "Default Gemini response"
        var sendCallCount: Int = 0
        var delayMs: Long = 0

        override fun getMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

        override suspend fun sendUserMessage(prompt: String): Result<String> {
            sendCallCount++
            if (delayMs > 0) delay(delayMs)

            val userEntity = ChatMessageEntity(
                id = (messagesFlow.value.size + 1).toLong(),
                senderRole = SenderRole.USER,
                content = prompt,
                status = MessageDeliveryStatus.SENT
            )
            messagesFlow.value = messagesFlow.value + userEntity

            return if (shouldFail) {
                val errorEntity = ChatMessageEntity(
                    id = (messagesFlow.value.size + 1).toLong(),
                    senderRole = SenderRole.ASSISTANT,
                    content = "Error: $failureMessage",
                    status = MessageDeliveryStatus.ERROR
                )
                messagesFlow.value = messagesFlow.value + errorEntity
                Result.failure(IllegalStateException(failureMessage))
            } else {
                val assistantEntity = ChatMessageEntity(
                    id = (messagesFlow.value.size + 1).toLong(),
                    senderRole = SenderRole.ASSISTANT,
                    content = returnText,
                    status = MessageDeliveryStatus.DELIVERED
                )
                messagesFlow.value = messagesFlow.value + assistantEntity
                Result.success(returnText)
            }
        }

        override suspend fun clearChat() {
            messagesFlow.value = emptyList()
        }

        override suspend fun deleteMessage(id: Long) {
            messagesFlow.value = messagesFlow.value.filterNot { it.id == id }
        }
    }
}
