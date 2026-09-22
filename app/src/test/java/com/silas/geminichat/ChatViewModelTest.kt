package com.silas.geminichat

import com.silas.geminichat.data.ChatHistoryRepository
import com.silas.geminichat.data.GeminiRepository
import com.silas.geminichat.data.prefs.UserPreferencesRepository
import com.silas.geminichat.model.ChatMessage
import com.silas.geminichat.ui.chat.ChatViewModel
import com.silas.geminichat.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Unit tests for [ChatViewModel] using kotlinx-coroutines-test and fake
 * repositories (no Android framework or network).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private class FakeGeminiRepository(private val result: Result<String>) : GeminiRepository {
        val prompts = mutableListOf<String>()
        override suspend fun generateText(prompt: String): Result<String> {
            prompts.add(prompt)
            return result
        }
    }

    private class FakeHistoryRepository : ChatHistoryRepository {
        private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        private var nextId = 1L

        override fun observeMessages(): Flow<List<ChatMessage>> = messages

        override suspend fun addMessage(message: ChatMessage) {
            messages.value = messages.value + message.copy(id = nextId++)
        }

        override suspend fun clearHistory() {
            messages.value = emptyList()
        }
    }

    private class FakePrefs : UserPreferencesRepository {
        override val userName = MutableStateFlow("")
        override suspend fun setUserName(name: String) = Unit
    }

    private fun createViewModel(
        geminiResult: Result<String> = Result.success("hi"),
        hasApiKey: Boolean = true,
    ): ChatViewModel = ChatViewModel(
        geminiRepository = FakeGeminiRepository(geminiResult),
        chatHistoryRepository = FakeHistoryRepository(),
        userPreferencesRepository = FakePrefs(),
        hasApiKey = hasApiKey,
    )

    @Test
    fun sendWithEmptyPromptShowsError() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = createViewModel()
            vm.onSend()
            advanceUntilIdle()
            assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sendAppendsUserAndAssistantMessagesOnSuccess() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = createViewModel(geminiResult = Result.success("Hello back"))
            vm.onPromptChange("Hello")
            vm.onSend()
            advanceUntilIdle()
            val messages = vm.uiState.value.messages
            assertEquals(2, messages.size)
            assertEquals("Hello", messages[0].content)
            assertEquals("Hello back", messages[1].content)
            assertFalse(vm.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sendSurfacesErrorMessageOnFailure() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = createViewModel(geminiResult = Result.failure(RuntimeException("boom")))
            vm.onPromptChange("Hi")
            vm.onSend()
            advanceUntilIdle()
            assertEquals("boom", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sendWithoutApiKeyShowsMissingKeyMessage() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val vm = createViewModel(hasApiKey = false)
            vm.onPromptChange("Hi")
            vm.onSend()
            advanceUntilIdle()
            assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
