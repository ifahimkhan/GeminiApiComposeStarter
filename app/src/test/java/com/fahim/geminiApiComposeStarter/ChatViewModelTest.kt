package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemePreference
import com.fahim.geminiApiComposeStarter.data.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import androidx.lifecycle.viewModelScope
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<ChatViewModel>()

    private lateinit var gemini: FakeGeminiRepository
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var history: ChatHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        gemini = FakeGeminiRepository()
        preferences = mockk()
        history = mockk()

        every {
            preferences.themePreference
        } returns flowOf(ThemePreference.SYSTEM)

        coEvery {
            preferences.saveTheme(any())
        } returns Unit

        coEvery {
            history.loadMessages()
        } returns emptyList()

        coEvery {
            history.saveExchange(any(), any())
        } returns Unit
    }

    @After
    fun tearDown() {
        createdViewModels.forEach {
            it.viewModelScope.cancel()
        }
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        hasApiKey: Boolean = true
    ): ChatViewModel {
        return ChatViewModel(
            repository = gemini,
            hasApiKey = hasApiKey,
            preferencesRepository = preferences,
            historyRepository = history
        ).also {
            createdViewModels.add(it)
        }
    }

    @Test
    fun startupLoadsSavedHistory() = runTest {
        val savedMessages = listOf(
            ChatMessage(
                id = "saved-message",
                text = "Previous conversation",
                isUser = true
            )
        )

        coEvery {
            history.loadMessages()
        } returns savedMessages

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(savedMessages, viewModel.uiState.value.messages)
    }

    @Test
    fun emptyPromptShowsValidationError() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(
            PromptError.EMPTY,
            viewModel.uiState.value.promptError
        )
        assertTrue(gemini.prompts.isEmpty())
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun missingApiKeyPreventsRequest() = runTest {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            viewModel.uiState.value.errorMessage
        )
        assertTrue(gemini.prompts.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun successfulReplyAddsMessagesAndSavesExchange() = runTest {
        gemini.response = Result.success("Hello from Gemini")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("  Hello  ")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(listOf("Hello"), gemini.prompts)
        assertEquals(2, state.messages.size)

        assertEquals("Hello", state.messages[0].text)
        assertTrue(state.messages[0].isUser)

        assertEquals("Hello from Gemini", state.messages[1].text)
        assertFalse(state.messages[1].isUser)

        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        coVerify(exactly = 1) {
            history.saveExchange(
                state.messages[0],
                state.messages[1]
            )
        }
    }

    @Test
    fun pendingReplyShowsLoadingAndBlocksDuplicateSend() = runTest {
        val pendingReply = CompletableDeferred<Result<String>>()
        gemini.pendingReply = pendingReply

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        runCurrent()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onSend()
        runCurrent()

        assertEquals(1, gemini.prompts.size)

        pendingReply.complete(Result.success("Reply"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.messages.size)
    }

    @Test
    fun failedRequestRestoresPromptAndRemovesFailedMessage() = runTest {
        gemini.response = Result.failure(
            IllegalStateException("Test connection error")
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals("Hello", state.prompt)
        assertEquals("Test connection error", state.errorMessage)
        assertFalse(state.isLoading)
        assertTrue(state.messages.isEmpty())

        coVerify(exactly = 0) {
            history.saveExchange(any(), any())
        }
    }

    @Test
    fun storageFailureKeepsReplyAndShowsWarning() = runTest {
        gemini.response = Result.success("Reply received")

        coEvery {
            history.saveExchange(any(), any())
        } throws IllegalStateException("Storage unavailable")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(2, state.messages.size)
        assertEquals("Reply received", state.messages.last().text)
        assertFalse(state.isLoading)
        assertEquals(
            "Reply received, but this exchange could not be saved.",
            state.errorMessage
        )
    }

    @Test
    fun historyLoadFailurePreventsSending() = runTest {
        coEvery {
            history.loadMessages()
        } throws IllegalStateException("Storage unavailable")

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(
            "Could not load chat history. Restart the app.",
            viewModel.uiState.value.errorMessage
        )

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertTrue(gemini.prompts.isEmpty())
        assertEquals(
            "Chat history is unavailable. Restart the app.",
            viewModel.uiState.value.errorMessage
        )
    }

    private class FakeGeminiRepository : GeminiRepository {
        val prompts = mutableListOf<String>()

        var response: Result<String> = Result.success("Test reply")

        var pendingReply: CompletableDeferred<Result<String>>? = null

        override suspend fun generateText(prompt: String): Result<String> {
            prompts.add(prompt)
            return pendingReply?.await() ?: response
        }
    }
}