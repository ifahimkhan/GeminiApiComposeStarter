package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.Sender
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @get:Rule val instantRule = InstantTaskExecutorRule()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `initial state is empty`() = runTest {
        val viewModel = ChatViewModel(FakeGemini(Result.success("ok")), FakeHistory())
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test fun `send persists prompt and successful response`() = runTest {
        val history = FakeHistory()
        val viewModel = ChatViewModel(FakeGemini(Result.success("reply")), history)
        viewModel.onPromptChange("hello")
        viewModel.onSend()
        assertTrue(viewModel.uiState.value.isLoading)
        advanceUntilIdle()
        assertEquals(listOf("hello", "reply"), history.messages.value.map { it.text })
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test fun `failure exposes safe error`() = runTest {
        val viewModel = ChatViewModel(FakeGemini(Result.failure(Exception("network"))), FakeHistory())
        viewModel.onPromptChange("hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(ChatViewModel.RESPONSE_ERROR_MESSAGE, viewModel.uiState.value.errorMessage)
    }

    private class FakeGemini(private val result: Result<String>) : GeminiRepository {
        override suspend fun generateText(prompt: String) = result
    }
    private class FakeHistory : ChatHistoryRepository {
        val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        override fun observeMessages(): Flow<List<ChatMessage>> = messages
        override suspend fun add(text: String, sender: Sender) {
            messages.value = messages.value + ChatMessage(messages.value.size.toLong() + 1, text, sender, messages.value.size.toLong())
        }
    }
}
