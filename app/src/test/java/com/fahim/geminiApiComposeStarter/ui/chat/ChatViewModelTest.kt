package com.fahim.geminiApiComposeStarter.ui.chat

import android.content.Context
import android.content.ContextWrapper
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.dao.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.entity.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeChatDao: FakeChatDao
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeChatDao = FakeChatDao()
        tempDir = Files.createTempDirectory("test_user_preferences").toFile()
        val fakeContext = object : ContextWrapper(null) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = tempDir
        }
        userPreferencesRepository = UserPreferencesRepository(fakeContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    private fun createViewModel(
        repository: GeminiRepository = fakeRepository,
        chatDao: ChatDao = fakeChatDao,
        hasApiKey: Boolean = true,
    ): ChatViewModel {
        return ChatViewModel(
            repository = repository,
            chatDao = chatDao,
            userPreferencesRepository = userPreferencesRepository,
            hasApiKey = hasApiKey,
        )
    }

    @Test
    fun initialState_defaultsAreCorrect() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun existingPersistedMessages_loadedIntoState() = runTest(testDispatcher) {
        val initialEntities = listOf(
            ChatMessageEntity(id = "1", text = "Hello", participant = "USER", timestamp = 1000L),
            ChatMessageEntity(id = "2", text = "Hi there!", participant = "MODEL", timestamp = 2000L),
        )
        fakeChatDao = FakeChatDao(initialEntities)
        val viewModel = createViewModel(chatDao = fakeChatDao)
        val state = viewModel.uiState.first { it.messages.size == 2 }

        val messages = state.messages
        assertEquals(2, messages.size)
        assertEquals("Hello", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("Hi there!", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsPromptError() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Trigger an empty error first
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)

        // Type a new prompt
        viewModel.onPromptChange("Tell me a joke")
        assertEquals("Tell me a joke", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun blankPrompt_setsPromptErrorAndDoesNotCallRepository() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("    ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeRepository.prompts.isEmpty())
        assertTrue(fakeChatDao.insertedMessages.isEmpty())
    }

    @Test
    fun missingApiKey_setsErrorMessageAndDoesNotCallRepository() = runTest(testDispatcher) {
        val viewModel = createViewModel(hasApiKey = false)
        advanceUntilIdle()

        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeRepository.prompts.isEmpty())
        assertTrue(fakeChatDao.insertedMessages.isEmpty())
    }

    @Test
    fun successfulSend_clearsPromptInsertsMessagesAndManagesLoading() = runTest(testDispatcher) {
        val promptReceived = CompletableDeferred<Unit>()
        val deferred = CompletableDeferred<Result<String>>()
        fakeRepository.onPromptReceived = { promptReceived.complete(Unit) }
        fakeRepository.deferredResult = deferred

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("What is Kotlin?")
        viewModel.onSend()

        // Prompt clears and loading starts immediately
        assertEquals("", viewModel.uiState.value.prompt)
        assertTrue(viewModel.uiState.value.isLoading)

        // Wait until prompt is received by repository (guarantees user message insertion completed)
        promptReceived.await()
        advanceUntilIdle()

        assertEquals(1, fakeChatDao.insertedMessages.size)
        assertEquals("What is Kotlin?", fakeChatDao.insertedMessages[0].text)
        assertEquals("USER", fakeChatDao.insertedMessages[0].participant)
        assertTrue(viewModel.uiState.value.isLoading)

        // Complete the repository call
        val modelInserted = CompletableDeferred<Unit>()
        fakeChatDao.onMessageInserted = { if (it.participant == "MODEL") modelInserted.complete(Unit) }
        deferred.complete(Result.success("Kotlin is a modern language"))

        // Wait until model message is inserted
        modelInserted.await()
        advanceUntilIdle()

        // State reflects both messages and loading finished
        val state = viewModel.uiState.first { it.messages.size == 2 && !it.isLoading }
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        // Both user and model messages are inserted
        assertEquals(2, fakeChatDao.insertedMessages.size)
        assertEquals("Kotlin is a modern language", fakeChatDao.insertedMessages[1].text)
        assertEquals("MODEL", fakeChatDao.insertedMessages[1].participant)

        val messages = state.messages
        assertEquals(2, messages.size)
        assertEquals("What is Kotlin?", messages[0].text)
        assertEquals("Kotlin is a modern language", messages[1].text)
    }

    @Test
    fun repositoryFailure_setsErrorMessageAndStopsLoading() = runTest(testDispatcher) {
        val promptReceived = CompletableDeferred<Unit>()
        fakeRepository.onPromptReceived = { promptReceived.complete(Unit) }
        fakeRepository.nextResult = Result.failure(Exception("API limit exceeded"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        // Wait until repository receives prompt (and user message has been inserted)
        promptReceived.await()
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.isLoading && it.errorMessage != null }
        assertFalse(state.isLoading)
        assertEquals("API limit exceeded", state.errorMessage)

        // User message was persisted, but no model message
        assertEquals(1, fakeChatDao.insertedMessages.size)
        assertEquals("USER", fakeChatDao.insertedMessages[0].participant)
    }

    @Test
    fun duplicateSend_ignoredWhileLoading() = runTest(testDispatcher) {
        val promptReceived = CompletableDeferred<Unit>()
        val deferred = CompletableDeferred<Result<String>>()
        fakeRepository.onPromptReceived = { promptReceived.complete(Unit) }
        fakeRepository.deferredResult = deferred

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onPromptChange("First prompt")
        viewModel.onSend()

        // Wait until first prompt reaches repository (and user message is inserted)
        promptReceived.await()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(1, fakeRepository.prompts.size)

        // Attempt a second send while still loading
        viewModel.onPromptChange("Second prompt")
        viewModel.onSend()
        advanceUntilIdle()

        // Verify repository was not called again and no second user message was inserted
        assertEquals(1, fakeRepository.prompts.size)
        assertEquals(1, fakeChatDao.insertedMessages.size)

        // Complete first request
        val modelInserted = CompletableDeferred<Unit>()
        fakeChatDao.onMessageInserted = { if (it.participant == "MODEL") modelInserted.complete(Unit) }
        deferred.complete(Result.success("First response"))
        modelInserted.await()
        advanceUntilIdle()

        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isLoading)
        assertEquals(2, fakeChatDao.insertedMessages.size)
    }
}

class FakeGeminiRepository : GeminiRepository {
    val prompts = mutableListOf<String>()
    var nextResult: Result<String>? = null
    var deferredResult: CompletableDeferred<Result<String>>? = null
    var onPromptReceived: (() -> Unit)? = null

    override suspend fun generateText(prompt: String): Result<String> {
        synchronized(this) {
            prompts.add(prompt)
        }
        onPromptReceived?.invoke()
        val deferred = deferredResult
        if (deferred != null) {
            return deferred.await()
        }
        return nextResult ?: Result.success("Default response")
    }
}

class FakeChatDao(initialMessages: List<ChatMessageEntity> = emptyList()) : ChatDao {
    private val _messages = MutableStateFlow(initialMessages)
    val insertedMessages = mutableListOf<ChatMessageEntity>()
    var onMessageInserted: ((ChatMessageEntity) -> Unit)? = null

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messages

    override suspend fun insertMessage(message: ChatMessageEntity) {
        synchronized(this) {
            insertedMessages.add(message)
            _messages.value = _messages.value + message
        }
        onMessageInserted?.invoke(message)
    }

    override suspend fun insertMessages(messages: List<ChatMessageEntity>) {
        synchronized(this) {
            insertedMessages.addAll(messages)
            _messages.value = _messages.value + messages
        }
        messages.forEach { onMessageInserted?.invoke(it) }
    }

    override suspend fun clearMessages(): Int {
        synchronized(this) {
            val count = _messages.value.size
            _messages.value = emptyList()
            insertedMessages.clear()
            return count
        }
    }
}
