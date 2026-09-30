package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.lifecycle.ViewModel
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sendPersistsUserAndGeminiMessagesAndTracksLoading() = runTest {
        val history = InMemoryChatHistoryRepository()
        val response = CompletableDeferred<Result<String>>()
        val gemini = FakeGeminiRepository { response.await() }
        val viewModel = ChatViewModel(gemini, history, hasApiKey = true)
        runCurrent()

        viewModel.onPromptChange("  hello  ")
        viewModel.onSend()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(listOf("hello"), gemini.prompts)
        assertEquals(listOf("user"), history.messages.value.map { it.role })
        assertNull(viewModel.uiState.value.errorMessage)

        response.complete(Result.success("Hello back"))
        advanceUntilIdle()

        assertEquals(listOf("user", "gemini"), history.messages.value.map { it.role })
        assertEquals("hello", history.messages.value[0].content)
        assertEquals("Hello back", history.messages.value[1].content)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun failedRequestKeepsUserMessageAndExposesTransientError() = runTest {
        val history = InMemoryChatHistoryRepository()
        val gemini = FakeGeminiRepository {
            Result.failure<String>(IllegalStateException("offline"))
        }
        val viewModel = ChatViewModel(gemini, history, hasApiKey = true)
        runCurrent()

        viewModel.onPromptChange("hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(listOf("user"), history.messages.value.map { it.role })
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("offline", viewModel.uiState.value.errorMessage)
    }
}

private class InMemoryChatHistoryRepository : ChatHistoryRepository {
    private val storedMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages = storedMessages.asStateFlow()

    override fun observeMessages(): Flow<List<ChatMessageEntity>> = messages

    override suspend fun insertMessage(message: ChatMessageEntity) {
        storedMessages.update { current ->
            current + message.copy(id = current.size + 1L)
        }
    }
}

private class FakeGeminiRepository(
    private val result: suspend (String) -> Result<String>,
) : GeminiRepository {
    val prompts = mutableListOf<String>()

    override suspend fun generateText(prompt: String): Result<String> {
        prompts += prompt
        return result(prompt)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}