package com.fahim.geminiApiComposeStarter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.fahim.geminiApiComposeStarter.data.*
import com.fahim.geminiApiComposeStarter.ui.chat.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: MemoryChatStore
    private lateinit var repository: FakeGeminiRepository
    private lateinit var preferences: MemoryPreferences
    private lateinit var vm: ChatViewModel
    private val owner = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        store = MemoryChatStore(); repository = FakeGeminiRepository(); preferences = MemoryPreferences()
        vm = ChatViewModel(repository, store, preferences, SavedStateHandle()); owner.put("chat", vm)
    }
    @After fun cleanup() { owner.clear(); Dispatchers.resetMain() }
    @Test fun blankInputDoesNotCallGemini() = runTest {
        advanceUntilIdle(); vm.onPromptChange("  "); vm.onSend(); advanceUntilIdle()
        assertEquals(0, repository.calls.size); assertNotNull(vm.uiState.value.promptError)
    }
    @Test fun sendsExactlyOnceAndPersistsBothMessages() = runTest {
        advanceUntilIdle(); vm.newChat(); vm.onPromptChange("Hello"); vm.onSend(); vm.onSend()
        assertTrue(vm.uiState.value.isLoading); advanceUntilIdle()
        assertEquals(1, repository.calls.size); assertEquals(listOf(Role.USER, Role.ASSISTANT), vm.uiState.value.messages.map { it.role })
        assertFalse(vm.uiState.value.isLoading); assertEquals("", vm.uiState.value.prompt)
    }
    @Test fun failureRetryDoesNotDuplicateUserTurn() = runTest {
        advanceUntilIdle(); repository.response = Result.failure(Exception("Offline"))
        vm.newChat(); vm.onPromptChange("Question"); vm.onSend(); advanceUntilIdle()
        val failed = vm.uiState.value.messages.single()
        assertEquals(Delivery.FAILED, failed.delivery)
        repository.response = Result.success("Recovered"); vm.retry(failed); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.messages.size); assertEquals(failed.id, vm.uiState.value.messages.first().id)
        assertEquals("Recovered", vm.uiState.value.messages.last().text)
    }
    @Test fun followUpContainsPreviousSuccessfulTurn() = runTest {
        advanceUntilIdle(); vm.newChat(); vm.onPromptChange("First question"); vm.onSend(); advanceUntilIdle()
        vm.onPromptChange("Explain more"); vm.onSend(); advanceUntilIdle()
        assertTrue(repository.calls.last().contains("USER: First question"))
        assertTrue(repository.calls.last().contains("ASSISTANT: Test answer"))
        assertEquals(4, vm.uiState.value.messages.size)
    }
    @Test fun switchingConversationDuringRequestDoesNotMoveItsAnswer() = runTest {
        advanceUntilIdle(); repository.pending = CompletableDeferred()
        vm.newChat(); vm.onPromptChange("Original"); vm.onSend(); runCurrent()
        val originalId = vm.uiState.value.conversationId!!
        vm.newChat(Category.RESEARCH); vm.onPromptChange("Keep this draft")
        repository.pending!!.complete(Result.success("Original answer")); advanceUntilIdle()
        assertNull(vm.uiState.value.conversationId); assertEquals("Keep this draft", vm.uiState.value.prompt)
        assertTrue(vm.uiState.value.messages.isEmpty()); assertEquals(2, store.messages(originalId).first().size)
    }
    @Test fun voiceCancellationAndUnavailableNeverSend() = runTest {
        advanceUntilIdle(); vm.navigate(Destination.VOICE); vm.onVoiceStarted(); vm.onVoiceResult(null)
        assertEquals(VoiceStatus.CANCELLED, vm.uiState.value.voiceStatus)
        vm.onVoiceUnavailable(); assertEquals(VoiceStatus.UNAVAILABLE, vm.uiState.value.voiceStatus)
        assertTrue(repository.calls.isEmpty())
    }
    @Test fun voiceTranscriptRequiresExplicitSend() = runTest {
        advanceUntilIdle(); vm.onVoiceResult("Recognized question"); vm.useTranscript(); advanceUntilIdle()
        assertEquals("Recognized question", vm.uiState.value.prompt)
        assertEquals(Destination.CHAT, vm.uiState.value.destination); assertTrue(repository.calls.isEmpty())
    }
    @Test fun regenerateReplacesAnswerWithoutDuplicatingTurn() = runTest {
        advanceUntilIdle(); vm.newChat(); vm.onPromptChange("Question"); vm.onSend(); advanceUntilIdle()
        repository.response = Result.success("Better answer"); vm.regenerate(); advanceUntilIdle()
        assertEquals(2, vm.uiState.value.messages.size); assertEquals("Better answer", vm.uiState.value.messages.last().text)
        assertFalse(repository.calls.last().contains("Test answer"))
    }
    @Test fun newConversationDoesNotLeakPreviousContext() = runTest {
        advanceUntilIdle(); vm.newChat(); vm.onPromptChange("Private first topic"); vm.onSend(); advanceUntilIdle()
        vm.newChat(); vm.onPromptChange("Separate topic"); vm.onSend(); advanceUntilIdle()
        assertFalse(repository.calls.last().contains("Private first topic")); assertEquals(2, vm.uiState.value.conversations.size)
    }
    @Test fun preferencesAreCollectedIntoUiState() = runTest {
        advanceUntilIdle(); vm.savePreferences(UserPreferences("Hemish", Category.RESEARCH, true)); advanceUntilIdle()
        assertEquals("Hemish", vm.uiState.value.preferences.displayName); assertTrue(vm.uiState.value.preferences.reducedMotion)
    }
    @Test fun rapidPreferenceEditsUpdateImmediatelyAndSaveTheLatestValue() = runTest {
        advanceUntilIdle()
        vm.savePreferences(UserPreferences("H")); vm.savePreferences(UserPreferences("Hemish"))
        assertEquals("Hemish", vm.uiState.value.preferences.displayName)
        advanceUntilIdle(); assertEquals("Hemish", preferences.preferences.value.displayName)
    }
    @Test fun selectedModelIsCapturedForPendingRequestAndNextTurnUsesNewSelection() = runTest {
        advanceUntilIdle()
        val first = GeminiModels.options[0].id; val second = GeminiModels.options[1].id
        vm.selectModel(first); repository.pending = CompletableDeferred()
        vm.newChat(); vm.onPromptChange("First"); vm.onSend()
        // Change before the request coroutine starts: the existing request must retain its model.
        vm.selectModel(second); runCurrent()
        assertEquals(listOf(first), repository.models)
        repository.pending!!.complete(Result.success("Answer")); advanceUntilIdle()
        repository.pending = null
        vm.onPromptChange("Next"); vm.onSend(); advanceUntilIdle()
        assertEquals(listOf(first, second), repository.models)
        assertEquals(second, preferences.preferences.value.modelId)
    }
    @Test fun retryUsesSelectedModelWithoutLosingConversationOrDraft() = runTest {
        advanceUntilIdle(); repository.response = Result.failure(Exception("Unavailable"))
        vm.newChat(); vm.onPromptChange("Keep my question"); vm.onSend(); advanceUntilIdle()
        val id = vm.uiState.value.conversationId
        vm.onPromptChange("A draft for later")
        val selected = GeminiModels.options.last().id
        vm.selectModel(selected); advanceUntilIdle()
        assertEquals(id, vm.uiState.value.conversationId)
        assertEquals("A draft for later", vm.uiState.value.prompt)
        repository.response = Result.success("Recovered"); vm.retry(vm.uiState.value.messages.single()); advanceUntilIdle()
        assertEquals(selected, repository.models.last()); assertEquals(2, vm.uiState.value.messages.size)
    }
    @Test fun unknownSavedModelFallsBackToConfiguredModel() = runTest {
        advanceUntilIdle(); vm.selectModel("removed-model"); advanceUntilIdle()
        assertEquals(GeminiModels.options.first().id, vm.uiState.value.preferences.modelId)
    }
    @Test fun contextBudgetKeepsRecentCompletePairsAndSkipsFailed() {
        val history = (0..10).flatMap { n -> listOf(
            ChatMessage("u$n", "c", "$n", n * 2L, Role.USER, "Q$n".repeat(1000)),
            ChatMessage("a$n", "c", "$n", n * 2L + 1, Role.ASSISTANT, "A$n".repeat(1000))) }
        val prompt = ConversationPromptBuilder.build(history, ChatMessage("current", "c", "now", 30, Role.USER, "Now"), Category.GENERAL)
        assertTrue(prompt.length < 24500); assertTrue(prompt.contains("Q10")); assertFalse(prompt.contains("Q0")); assertTrue(prompt.endsWith("USER: Now\nASSISTANT:"))
    }
}
class FakeGeminiRepository : GeminiRepository {
    val calls = mutableListOf<String>()
    val models = mutableListOf<String>()
    var response = Result.success("Test answer")
    var pending: CompletableDeferred<Result<String>>? = null
    override suspend fun generateText(prompt: String): Result<String> { calls.add(prompt); return pending?.await() ?: response }
    override suspend fun generateText(prompt: String, modelId: String): Result<String> { models.add(modelId); return generateText(prompt) }
}
class MemoryPreferences : PreferenceStore {
    override val preferences = MutableStateFlow(UserPreferences())
    override suspend fun save(value: UserPreferences) { preferences.value = value }
}
class MemoryChatStore : ChatStore {
    private val chats = MutableStateFlow<List<Conversation>>(emptyList())
    private val rows = MutableStateFlow<List<ChatMessage>>(emptyList())
    override fun conversations(query: String) = chats.map { list -> list.filter { query.isBlank() || it.title.contains(query, true) } }
    override fun messages(conversationId: String) = rows.map { list -> list.filter { it.conversationId == conversationId }.sortedBy { it.sequence } }
    override suspend fun create(conversation: Conversation) { if (chats.value.none { it.id == conversation.id }) chats.value += conversation }
    override suspend fun appendUser(message: ChatMessage) { rows.value += message }
    override suspend fun setDelivery(messageId: String, delivery: Delivery) { rows.value = rows.value.map { if (it.id == messageId) it.copy(delivery = delivery) else it } }
    override suspend fun complete(user: ChatMessage, answer: String) {
        setDelivery(user.id, Delivery.SENT)
        rows.value = rows.value.filterNot { it.id == user.turnId + "-answer" } + ChatMessage(user.turnId + "-answer", user.conversationId, user.turnId, user.sequence + 1, Role.ASSISTANT, answer)
    }
    override suspend fun feedback(messageId: String, value: Int) { rows.value = rows.value.map { if (it.id == messageId) it.copy(feedback = value) else it } }
    override suspend fun recoverInterrupted() { rows.value = rows.value.map { if (it.delivery == Delivery.PENDING) it.copy(delivery = Delivery.INTERRUPTED) else it } }
}
