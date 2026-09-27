package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.MissingApiKeyException
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.ResponseStyle
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistoryRepository
    private lateinit var prefs: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gemini = FakeGeminiRepository()
        history = FakeChatHistoryRepository()
        prefs = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ChatViewModel(gemini, history, prefs)

    @Test
    fun emptyPrompt_setsPromptError_andDoesNotCallGemini() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onPromptChange("   ")
        vm.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertTrue(gemini.calls.isEmpty())
    }

    @Test
    fun typingAfterError_clearsPromptError() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onSend()
        vm.onPromptChange("h")
        assertNull(vm.uiState.value.promptError)
    }

    @Test
    fun sendSuccess_storesUserAndModelMessages_andClearsPrompt() = runTest(dispatcher) {
        gemini.enqueue(Result.success("Hi there!"))
        val vm = createViewModel()

        vm.onPromptChange("  Hello  ")
        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(Role.USER, Role.MODEL), state.messages.map { it.role })
        assertEquals(listOf("Hello", "Hi there!"), state.messages.map { it.text })
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun isLoading_isTrueWhileWaitingForGemini() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate
        val vm = createViewModel()

        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isLoading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun sendIgnored_whileAlreadyLoading() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        gemini.gate = gate
        val vm = createViewModel()

        vm.onPromptChange("first")
        vm.onSend()
        advanceUntilIdle()
        vm.onPromptChange("second")
        vm.onSend()
        advanceUntilIdle()

        assertEquals(1, gemini.calls.size)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun previousMessages_areSentAsHistory_withSelectedStyle() = runTest(dispatcher) {
        history = FakeChatHistoryRepository(
            listOf(
                ChatMessage(1, Role.USER, "Who wrote Hamlet?", 1),
                ChatMessage(2, Role.MODEL, "Shakespeare.", 2),
            )
        )
        val vm = createViewModel()
        vm.onResponseStyleChange(ResponseStyle.CONCISE)
        advanceUntilIdle()

        vm.onPromptChange("When?")
        vm.onSend()
        advanceUntilIdle()

        val call = gemini.calls.single()
        assertEquals("When?", call.prompt)
        assertEquals(listOf("Who wrote Hamlet?", "Shakespeare."), call.history.map { it.text })
        assertEquals(ResponseStyle.CONCISE, call.style)
    }

    @Test
    fun failure_setsRequestError_andOffersRetry() = runTest(dispatcher) {
        gemini.enqueue(Result.failure(RuntimeException("Network down")))
        val vm = createViewModel()

        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ChatError.Request("Network down"), state.error)
        assertEquals("Hello", state.retryPrompt)
        assertFalse(state.isLoading)
        // The user's message is kept in history even though the call failed.
        assertEquals(listOf("Hello"), state.messages.map { it.text })
    }

    @Test
    fun retry_resendsPrompt_withoutDuplicatingUserMessage() = runTest(dispatcher) {
        gemini.enqueue(Result.failure(RuntimeException("timeout")), Result.success("Recovered"))
        val vm = createViewModel()

        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        vm.onRetry()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("Hello", "Recovered"), state.messages.map { it.text })
        assertTrue(gemini.calls[1].history.isEmpty())
        assertEquals("Hello", gemini.calls[1].prompt)
        assertNull(state.error)
    }

    @Test
    fun missingApiKey_showsKeyError_withoutRetry() = runTest(dispatcher) {
        gemini.enqueue(Result.failure(MissingApiKeyException()))
        val vm = createViewModel()

        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()

        assertEquals(ChatError.MissingApiKey, vm.uiState.value.error)
        assertNull(vm.uiState.value.retryPrompt)
    }

    @Test
    fun errorShown_clearsError() = runTest(dispatcher) {
        gemini.enqueue(Result.failure(RuntimeException("x")))
        val vm = createViewModel()
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()

        vm.onErrorShown()

        assertNull(vm.uiState.value.error)
        assertNull(vm.uiState.value.retryPrompt)
    }

    @Test
    fun voiceResult_fillsEmptyPrompt_orAppendsToExistingText() = runTest(dispatcher) {
        val vm = createViewModel()

        vm.onVoiceResult("what is kotlin")
        assertEquals("what is kotlin", vm.uiState.value.prompt)

        vm.onVoiceResult("  and compose ")
        assertEquals("what is kotlin and compose", vm.uiState.value.prompt)
    }

    @Test
    fun voiceUnavailable_setsError() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onVoiceUnavailable()
        assertEquals(ChatError.VoiceUnavailable, vm.uiState.value.error)
    }

    @Test
    fun clearHistory_removesAllMessages() = runTest(dispatcher) {
        history = FakeChatHistoryRepository(listOf(ChatMessage(1, Role.USER, "old", 1)))
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.messages.size)

        vm.onClearHistory()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun preferences_areLoadedAndUpdated() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onThemeModeChange(ThemeMode.DARK)
        vm.onDynamicColorChange(false)
        advanceUntilIdle()

        val p = vm.uiState.value.preferences
        assertEquals(ThemeMode.DARK, p.themeMode)
        assertFalse(p.dynamicColor)
    }

    @Test
    fun settingsDialog_opensAndCloses() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onOpenSettings()
        assertTrue(vm.uiState.value.showSettings)
        vm.onDismissSettings()
        assertFalse(vm.uiState.value.showSettings)
    }
}
