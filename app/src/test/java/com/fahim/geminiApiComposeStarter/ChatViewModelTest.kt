package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository(hasKey = true)
        viewModel = ChatViewModel(repository = fakeRepository, userPreferencesRepository = null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultValues() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() = runTest(testDispatcher) {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.hasKey = false
        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_successfulResponse_clearsPromptAndUpdatesMessages() = runTest(testDispatcher) {
        fakeRepository.responseToReturn = Result.success("This is a response")

        viewModel.onPromptChange("What is Kotlin?")
        viewModel.onSend()

        // Wait for coroutines to complete
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.messages.size)
        assertEquals("What is Kotlin?", state.messages[0].content)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("This is a response", state.messages[1].content)
        assertEquals(MessageRole.MODEL, state.messages[1].role)
    }

    @Test
    fun onSend_failureResponse_setsErrorMessage() = runTest(testDispatcher) {
        fakeRepository.responseToReturn = Result.failure(RuntimeException("Network timeout"))

        viewModel.onPromptChange("Query with error")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network timeout", state.errorMessage)
    }

    @Test
    fun onSpeechResult_appendsToExistingPrompt() = runTest(testDispatcher) {
        viewModel.onPromptChange("Translate this:")
        viewModel.onSpeechResult("Good morning")

        assertEquals("Translate this: Good morning", viewModel.uiState.value.prompt)
    }

    @Test
    fun onClearChat_clearsMessages() = runTest(testDispatcher) {
        fakeRepository.responseToReturn = Result.success("Response")
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSelectModel_updatesSelectedModel() = runTest(testDispatcher) {
        viewModel.onSelectModel("gemini-3.6-pro")
        assertEquals("gemini-3.6-pro", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun onThemeModeChange_updatesThemeMode() = runTest(testDispatcher) {
        viewModel.onThemeModeChange("DARK")
        assertEquals("DARK", viewModel.uiState.value.themeMode)

        viewModel.onThemeModeChange("LIGHT")
        assertEquals("LIGHT", viewModel.uiState.value.themeMode)
    }

    @Test
    fun buildGeminiHistory_filtersErrorsAndEnsuresValidAlternation() {
        val entities = listOf(
            ChatMessageEntity(id = 1, role = MessageRole.USER.name, content = "My name is Alice"),
            ChatMessageEntity(id = 2, role = MessageRole.MODEL.name, content = "Nice to meet you Alice!"),
            ChatMessageEntity(id = 3, role = MessageRole.MODEL.name, content = "Failed request", isError = true),
            ChatMessageEntity(id = 4, role = MessageRole.USER.name, content = "I like coding in Kotlin"),
            ChatMessageEntity(id = 5, role = MessageRole.MODEL.name, content = "Kotlin is awesome!"),
        )
        val history = com.fahim.geminiApiComposeStarter.data.buildGeminiHistory(entities)
        assertEquals(4, history.size)
        assertEquals("user", history[0].role)
        assertEquals("model", history[1].role)
        assertEquals("user", history[2].role)
        assertEquals("model", history[3].role)
    }
}

/**
 * In-memory test double for [GeminiRepository].
 */
private class FakeGeminiRepository(var hasKey: Boolean = true) : GeminiRepository {

    private val _messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    override val messages: Flow<List<ChatMessageEntity>> = _messagesFlow

    var responseToReturn: Result<String> = Result.success("Default fake response")
    private var nextId = 1L

    override fun hasApiKey(): Boolean = hasKey

    override suspend fun sendMessage(prompt: String, modelName: String): Result<String> {
        val userEntity = ChatMessageEntity(
            id = nextId++,
            role = MessageRole.USER.name,
            content = prompt,
        )
        _messagesFlow.update { it + userEntity }

        return responseToReturn.onSuccess { reply ->
            val modelEntity = ChatMessageEntity(
                id = nextId++,
                role = MessageRole.MODEL.name,
                content = reply,
            )
            _messagesFlow.update { it + modelEntity }
        }.onFailure { err ->
            val errorEntity = ChatMessageEntity(
                id = nextId++,
                role = MessageRole.MODEL.name,
                content = err.message ?: "Error",
                isError = true,
            )
            _messagesFlow.update { it + errorEntity }
        }
    }

    override suspend fun clearHistory() {
        _messagesFlow.value = emptyList()
    }

    override suspend fun deleteMessage(id: Long) {
        _messagesFlow.update { list -> list.filterNot { it.id == id } }
    }
}
