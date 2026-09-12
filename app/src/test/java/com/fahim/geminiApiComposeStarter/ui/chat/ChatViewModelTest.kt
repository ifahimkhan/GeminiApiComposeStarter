package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessage
import com.fahim.geminiApiComposeStarter.data.local.MessageAuthor
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun emptyPrompt_setsValidationError() = runTest {
        val viewModel = createViewModel()

        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun successfulSend_persistsUserAndGeminiMessages() = runTest {
        val history = FakeHistoryRepository()
        val viewModel = createViewModel(history = history)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals(MessageAuthor.USER, messages[0].author)
        assertEquals("Hello", messages[0].text)
        assertEquals(MessageAuthor.GEMINI, messages[1].author)
        assertEquals("Fake Gemini response", messages[1].text)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun missingKey_doesNotCallGemini() = runTest {
        val gemini = FakeGeminiRepository()
        val viewModel = createViewModel(
            gemini = gemini,
            hasApiKey = false,
        )

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(gemini.wasCalled)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("GEMINI_API_KEY") == true)
    }

    private fun createViewModel(
        gemini: FakeGeminiRepository = FakeGeminiRepository(),
        history: FakeHistoryRepository = FakeHistoryRepository(),
        preferences: FakePreferencesRepository = FakePreferencesRepository(),
        hasApiKey: Boolean = true,
    ) = ChatViewModel(
        geminiRepository = gemini,
        historyRepository = history,
        preferencesRepository = preferences,
        hasApiKey = hasApiKey,
    )
}

private class FakeGeminiRepository : GeminiRepository {
    var wasCalled = false

    override suspend fun generateText(prompt: String): Result<String> {
        wasCalled = true
        return Result.success("Fake Gemini response")
    }
}

private class FakeHistoryRepository : ChatHistoryRepository {
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override fun observeMessages(): Flow<List<ChatMessage>> = messages

    override suspend fun addMessage(author: MessageAuthor, text: String) {
        messages.value = messages.value + ChatMessage(
            id = nextId++,
            author = author,
            text = text,
            timestamp = 1_700_000_000_000,
        )
    }

    override suspend fun clearHistory() {
        messages.value = emptyList()
    }
}

private class FakePreferencesRepository : UserPreferencesRepository {
    private val name = MutableStateFlow("")
    override val displayName: Flow<String> = name

    override suspend fun setDisplayName(name: String) {
        this.name.value = name
    }
}
