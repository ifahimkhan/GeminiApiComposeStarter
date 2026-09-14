package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ChatStorage
import com.fahim.geminiApiComposeStarter.data.Conversation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeRepository : GeminiRepository {
        val prompts = mutableListOf<String>()
        var result: Result<String> = Result.success("Hello from Gemini")
        var pending: CompletableDeferred<Result<String>>? = null
        override suspend fun generateText(prompt: String): Result<String> {
            prompts += prompt
            return pending?.await() ?: result
        }
    }

    @Test fun emptyPromptDoesNotCallApi() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo, true)
        vm.onPromptChange("   ")
        vm.onSend()
        advanceUntilIdle()
        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertTrue(repo.prompts.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun missingKeyShowsErrorWithoutSending() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo, false)
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertTrue(repo.prompts.isEmpty())
        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test fun sendPublishesLoadingThenOrderedMessages() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ChatViewModel(repo, true)
        vm.onPromptChange("  Hello  ")
        vm.onSend()
        assertTrue(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.prompt)
        assertEquals("Hello", vm.uiState.value.messages.single().text)
        advanceUntilIdle()
        assertEquals(listOf("Hello"), repo.prompts)
        assertEquals(listOf(ChatRole.USER, ChatRole.GEMINI), vm.uiState.value.messages.map { it.role })
        assertEquals("Hello from Gemini", vm.uiState.value.messages.last().text)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun failureKeepsUserMessageAndAllowsRetry() = runTest(dispatcher) {
        val repo = FakeRepository().apply { result = Result.failure(IllegalStateException("Offline")) }
        val vm = ChatViewModel(repo, true)
        vm.onPromptChange("Hello")
        vm.onSend()
        advanceUntilIdle()
        assertEquals("Offline", vm.uiState.value.errorMessage)
        assertEquals(ChatRole.USER, vm.uiState.value.messages.single().role)
        assertFalse(vm.uiState.value.isLoading)
        repo.result = Result.success("Recovered")
        vm.onPromptChange("Try again")
        assertNull(vm.uiState.value.errorMessage)
        vm.onSend()
        advanceUntilIdle()
        assertEquals("Recovered", vm.uiState.value.messages.last().text)
    }

    @Test fun duplicateSendIsBlockedAndDraftSurvivesResponse() = runTest(dispatcher) {
        val response = CompletableDeferred<Result<String>>()
        val repo = FakeRepository().apply { pending = response }
        val vm = ChatViewModel(repo, true)
        vm.onPromptChange("First")
        vm.onSend()
        runCurrent()
        vm.onPromptChange("Next draft")
        vm.onSend()
        runCurrent()
        assertEquals(listOf("First"), repo.prompts)
        response.complete(Result.success("Answer"))
        advanceUntilIdle()
        assertEquals("Next draft", vm.uiState.value.prompt)
        assertEquals(2, vm.uiState.value.messages.size)
    }

    @Test fun messageIdsRemainUniqueAcrossRequests() = runTest(dispatcher) {
        val vm = ChatViewModel(FakeRepository(), true)
        repeat(3) {
            vm.onPromptChange("Prompt $it")
            vm.onSend()
            advanceUntilIdle()
        }
        val ids = vm.uiState.value.messages.map { it.id }
        assertEquals(6, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun attachmentsCanBeSentWithoutTypedTextAndAreClearedAfterward() = runTest(dispatcher) {
        val received = mutableListOf<PendingAttachment>()
        val repo = object : GeminiRepository {
            override suspend fun generateText(prompt: String) = Result.success("unused")
            override suspend fun generateConversation(messages: List<ChatMessage>, attachments: List<PendingAttachment>): Result<String> {
                received += attachments
                return Result.success("I can see the attachment.")
            }
        }
        val vm = ChatViewModel(repo, true)
        val attachment = PendingAttachment("content://example/file", "notes.pdf", "application/pdf")
        vm.addAttachments(listOf(attachment))
        vm.onSend()
        advanceUntilIdle()
        assertEquals(listOf(attachment), received)
        assertTrue(vm.uiState.value.pendingAttachments.isEmpty())
        assertEquals("Analyze the attached file.", vm.uiState.value.messages.first().text)
        assertEquals(listOf(attachment), vm.uiState.value.messages.first().attachments)
    }

    private class MemoryStorage : ChatStorage {
        var saved = emptyList<Conversation>()
        override suspend fun load() = saved
        override suspend fun save(conversations: List<Conversation>) { saved = conversations }
    }

    @Test fun unsentDraftIsNeverStoredOrRestored() = runTest(dispatcher) {
        val storage = MemoryStorage()
        val vm = ChatViewModel(FakeRepository(), true, storage)
        advanceUntilIdle()
        vm.onPromptChange("Remember me")
        vm.onSend()
        advanceUntilIdle()
        vm.onPromptChange("Unsent draft")
        advanceUntilIdle()
        val restored = ChatViewModel(FakeRepository(), true, storage)
        advanceUntilIdle()
        assertEquals("", storage.saved.single().draft)
        assertEquals("", restored.uiState.value.prompt)
        val conversationId = storage.saved.single().id
        restored.selectChat(conversationId)
        advanceUntilIdle()
        assertEquals(listOf("Remember me", "Hello from Gemini"), restored.uiState.value.messages.map { it.text })
        assertEquals("", restored.uiState.value.prompt)
    }

    @Test fun typingInANewChatDoesNotCreateAHistoryEntry() = runTest(dispatcher) {
        val storage = MemoryStorage()
        val vm = ChatViewModel(FakeRepository(), true, storage)
        advanceUntilIdle()
        vm.onPromptChange("Do not save me")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.conversations.isEmpty())
        assertTrue(storage.saved.isEmpty())
    }

    @Test fun switchesChatsWithoutMixingMessagesAndDeletesOnlySelectedChat() = runTest(dispatcher) {
        val storage = MemoryStorage()
        val vm = ChatViewModel(FakeRepository(), true, storage)
        advanceUntilIdle()
        vm.onPromptChange("First chat")
        vm.onSend()
        advanceUntilIdle()
        val first = vm.uiState.value.activeId
        vm.newChat()
        vm.onPromptChange("Second chat")
        vm.onSend()
        advanceUntilIdle()
        val second = vm.uiState.value.activeId
        vm.selectChat(first)
        assertEquals("First chat", vm.uiState.value.messages.first().text)
        vm.deleteChat(second)
        advanceUntilIdle()
        assertEquals(listOf(first), storage.saved.map { it.id })
    }

    @Test fun passesWholeConversationToRepository() = runTest(dispatcher) {
        var received = emptyList<ChatMessage>()
        val repo = object : GeminiRepository {
            override suspend fun generateText(prompt: String) = Result.success("unused")
            override suspend fun generateConversation(messages: List<ChatMessage>): Result<String> {
                received = messages
                return Result.success("Answer")
            }
        }
        val vm = ChatViewModel(repo, true)
        vm.onPromptChange("First")
        vm.onSend()
        advanceUntilIdle()
        vm.onPromptChange("Follow up")
        vm.onSend()
        advanceUntilIdle()
        assertEquals(listOf("First", "Answer", "Follow up"), received.map { it.text })
    }
}
