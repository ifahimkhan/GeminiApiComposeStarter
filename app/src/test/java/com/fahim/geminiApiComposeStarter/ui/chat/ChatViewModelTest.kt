package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }
    private class Store : ChatStore {
        override val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        override suspend fun insert(message: ChatMessageEntity) {
            messages.value += message.copy(id = (messages.value.size + 1).toLong())
        }
        override suspend fun clear() { messages.value = emptyList() }
    }
    private class Preferences : UserPreferences {
        override val compactMessages = MutableStateFlow(false)
        override suspend fun setCompactMessages(value: Boolean) { compactMessages.value = value }
    }
    private class Repository : GeminiRepository {
        var calls = 0
        val response = CompletableDeferred<Result<String>>()
        override suspend fun generateText(prompt: String): Result<String> { calls++; return response.await() }
    }
    @Test fun successPersistsMessagesAndTracksLoading() = runTest(dispatcher) {
        val repository = Repository(); val store = Store()
        val vm = ChatViewModel(repository, store, Preferences())
        runCurrent(); vm.updateInput(" Hello "); vm.sendMessage(); runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals("Hello", store.messages.value.single().text)
        vm.updateInput("second"); vm.sendMessage(); runCurrent()
        assertEquals(1, repository.calls)
        repository.response.complete(Result.success("Hi!")); runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("Hello", "Hi!"), vm.uiState.value.messages.map { it.text })
        assertEquals("second", vm.uiState.value.input)
    }
    @Test fun failureRestoresPromptWithoutLeakingException() = runTest(dispatcher) {
        val repo = Repository(); val vm = ChatViewModel(repo, Store(), Preferences())
        runCurrent(); vm.updateInput("Hello"); vm.sendMessage(); runCurrent()
        repo.response.complete(Result.failure(Exception("sensitive server payload"))); runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Hello", vm.uiState.value.input)
        assertNotNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.errorMessage!!.contains("sensitive"))
        vm.clearError(); assertNull(vm.uiState.value.errorMessage)
    }
    @Test fun blankInputDoesNotCallRepository() = runTest(dispatcher) {
        val repo = Repository(); val vm = ChatViewModel(repo, Store(), Preferences())
        runCurrent(); vm.updateInput("   "); vm.sendMessage(); runCurrent()
        assertEquals(0, repo.calls)
    }
    @Test fun restoresHistoryAndPreferencesAndClearsHistory() = runTest(dispatcher) {
        val store = Store(); store.insert(ChatMessageEntity(text = "Saved", isFromUser = true))
        val prefs = Preferences(); prefs.setCompactMessages(true)
        val vm = ChatViewModel(Repository(), store, prefs); runCurrent()
        assertEquals("Saved", vm.uiState.value.messages.single().text)
        assertTrue(vm.uiState.value.compactMessages)
        vm.setCompactMessages(false); runCurrent(); assertFalse(prefs.compactMessages.value)
        vm.clearHistory(); runCurrent(); assertTrue(vm.uiState.value.messages.isEmpty())
    }
    @Test fun cancellationIsNotDisplayedAsNetworkFailure() = runTest(dispatcher) {
        val repo = Repository(); val vm = ChatViewModel(repo, Store(), Preferences())
        runCurrent(); vm.updateInput("Hello"); vm.sendMessage(); runCurrent()
        repo.response.cancel(); runCurrent()
        assertFalse(vm.uiState.value.isLoading); assertNull(vm.uiState.value.errorMessage)
    }
}
