package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ConversationRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val gemini = FakeGeminiRepository()
    private val history = FakeConversations()
    private val preferences = FakePreferences()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun model(hasKey: Boolean = true, initialize: suspend () -> Unit = {}) =
        ChatViewModel(gemini, history, preferences, hasKey, initialize)

    @Test fun emptyPromptIsRejectedWithoutNetworkOrDatabase() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("   ")
        model.onSend()
        assertEquals(PromptError.EMPTY, model.uiState.value.promptError)
        assertEquals(0, gemini.calls)
        assertTrue(history.messages.value.isEmpty())
        model.onPromptChange("Hello")
        assertNull(model.uiState.value.promptError)
    }

    @Test fun successSavesBothMessagesAndClearsInput() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("  Hello  ")
        model.onSend()
        assertTrue(model.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals("", model.uiState.value.prompt)
        assertEquals(listOf("Hello", "Hello from Gemini"), model.uiState.value.messages.map { it.text })
        assertEquals(listOf(true, false), model.uiState.value.messages.map { it.isUser })
    }

    @Test fun duplicateSendIsIgnoredWhileRequestIsPending() = runTest(dispatcher) {
        gemini.pending = CompletableDeferred()
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        runCurrent()
        model.onSend()
        assertEquals(1, gemini.calls)
        assertTrue(model.uiState.value.isLoading)
        gemini.pending!!.complete(Result.success("Done"))
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun apiFailureRestoresPromptAndNeverExposesExceptionDetails() = runTest(dispatcher) {
        gemini.result = Result.failure(IllegalStateException("sensitive upstream details"))
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals("Hello", model.uiState.value.prompt)
        assertNotNull(model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.errorMessage!!.contains("sensitive"))
        assertEquals(1, model.uiState.value.messages.size)
        model.dismissError()
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun thrownNetworkFailureDoesNotCrashOrLeaveLoadingActive() = runTest(dispatcher) {
        gemini.throws = true
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertNotNull(model.uiState.value.errorMessage)
    }

    @Test fun missingKeyDoesNotSendOrPersistPrompt() = runTest(dispatcher) {
        val model = model(hasKey = false)
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, model.uiState.value.errorMessage)
        assertEquals(0, gemini.calls)
        assertTrue(history.messages.value.isEmpty())
    }

    @Test fun keyInitializationFailureIsHandled() = runTest(dispatcher) {
        val model = model(initialize = { error("Keystore failure") })
        advanceUntilIdle()
        assertFalse(model.uiState.value.isInitializing)
        model.onPromptChange("Hello")
        model.onSend()
        assertEquals(0, gemini.calls)
        assertNotNull(model.uiState.value.errorMessage)
    }

    @Test fun persistedHistoryIsLoadedByNewViewModelAndCanBeCleared() = runTest(dispatcher) {
        history.add(ChatMessage(text = "Saved", isUser = true))
        val model = model()
        advanceUntilIdle()
        assertEquals("Saved", model.uiState.value.messages.single().text)
        model.requestClearHistory()
        assertTrue(model.uiState.value.showClearConfirmation)
        model.clearHistory()
        advanceUntilIdle()
        assertTrue(model.uiState.value.messages.isEmpty())
        assertFalse(model.uiState.value.showClearConfirmation)
        assertFalse(model.uiState.value.isClearing)
    }

    @Test fun databaseWriteFailurePreventsNetworkCall() = runTest(dispatcher) {
        history.failWrites = true
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        advanceUntilIdle()
        assertEquals(0, gemini.calls)
        assertEquals("Hello", model.uiState.value.prompt)
        assertNotNull(model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun clearFailureKeepsHistoryAndReportsError() = runTest(dispatcher) {
        history.add(ChatMessage(text = "Saved", isUser = true))
        history.failWrites = true
        val model = model()
        advanceUntilIdle()
        model.clearHistory()
        advanceUntilIdle()
        assertEquals(1, model.uiState.value.messages.size)
        assertNotNull(model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.isClearing)
    }

    @Test fun themePreferenceIsSavedAndObserved() = runTest(dispatcher) {
        val model = model()
        advanceUntilIdle()
        model.setTheme(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, preferences.theme.value)
        assertEquals(ThemeMode.DARK, model.uiState.value.theme)
    }

    @Test fun clearIsBlockedDuringGeneration() = runTest(dispatcher) {
        gemini.pending = CompletableDeferred()
        val model = model()
        advanceUntilIdle()
        model.onPromptChange("Hello")
        model.onSend()
        runCurrent()
        model.clearHistory()
        assertEquals(1, history.messages.value.size)
        gemini.pending!!.complete(Result.success("Done"))
        advanceUntilIdle()
    }
}

private class FakeGeminiRepository : GeminiRepository {
    var calls = 0
    var throws = false
    var result = Result.success("Hello from Gemini")
    var pending: CompletableDeferred<Result<String>>? = null
    override suspend fun generateText(prompt: String): Result<String> {
        calls++
        if (throws) error("Network unavailable")
        return pending?.await() ?: result
    }
}

private class FakeConversations : ConversationRepository {
    override val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    var failWrites = false
    override suspend fun add(message: ChatMessage) {
        if (failWrites) error("Disk unavailable")
        messages.value = messages.value + message.copy(id = messages.value.size.toLong() + 1)
    }
    override suspend fun clear() {
        if (failWrites) error("Disk unavailable")
        messages.value = emptyList()
    }
}

private class FakePreferences : UserPreferences {
    override val theme = MutableStateFlow(ThemeMode.SYSTEM)
    override suspend fun setTheme(mode: ThemeMode) { theme.value = mode }
}
