package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

private class FakeGeminiRepository(
    var result: Result<String> = Result.success("Hi from fake"),
) : GeminiRepository {
    var lastPrompt: String? = null
    var callCount = 0

    override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
        callCount++
        lastPrompt = prompt
        return result
    }
}

private class FakeChatHistory : ChatHistoryRepository {
    private val store = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var nextId = 1L

    override val messages = store

    override suspend fun add(text: String, isUser: Boolean) {
        store.value = store.value + ChatMessage(nextId++, text, isUser)
    }

    override suspend fun clear() {
        store.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var gemini: FakeGeminiRepository
    private lateinit var history: FakeChatHistory

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        gemini = FakeGeminiRepository()
        history = FakeChatHistory()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(hasApiKey: Boolean = true) =
        ChatViewModel(gemini, history, hasApiKey)

    @Test
    fun onPromptChange_updatesPrompt_andClearsError() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onSend() // empty prompt -> error
        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)

        vm.onPromptChange("hello")

        assertEquals("hello", vm.uiState.value.prompt)
        assertNull(vm.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsError_andDoesNotCallRepository() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onPromptChange("   ")

        vm.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertEquals(0, gemini.callCount)
    }

    @Test
    fun onSend_missingApiKey_showsErrorMessage() = runTest(dispatcher) {
        val vm = viewModel(hasApiKey = false)
        vm.onPromptChange("hello")

        vm.onSend()
        advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertEquals(0, gemini.callCount)
    }

    @Test
    fun onSend_success_addsUserAndAssistantMessages() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onPromptChange("Hello Gemini")

        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Hello Gemini", gemini.lastPrompt)
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[0].isUser)
        assertEquals("Hello Gemini", state.messages[0].text)
        assertFalse(state.messages[1].isUser)
        assertEquals("Hi from fake", state.messages[1].text)
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
    }

    @Test
    fun onSend_failure_showsError_andStopsLoading() = runTest(dispatcher) {
        gemini.result = Result.failure(RuntimeException("Network down"))
        val vm = viewModel()
        vm.onPromptChange("Hello")

        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Network down", state.errorMessage)
        assertFalse(state.isLoading)
        // Only the user's message is stored; there is no assistant reply.
        assertEquals(1, state.messages.size)
        assertTrue(state.messages[0].isUser)
    }

    @Test
    fun onSend_setsLoadingWhileWaiting() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onPromptChange("Hello")

        vm.onSend()

        // Coroutine hasn't run yet, so we are still in the loading state.
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun history_isLoadedIntoUiState_atStartup() = runTest(dispatcher) {
        history.add("Earlier question", isUser = true)
        history.add("Earlier answer", isUser = false)

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.messages.size)
        assertEquals("Earlier question", vm.uiState.value.messages.first().text)
    }
}