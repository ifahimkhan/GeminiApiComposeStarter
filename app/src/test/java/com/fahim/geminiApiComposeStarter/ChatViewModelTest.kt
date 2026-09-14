package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository(var shouldFail: Boolean = false) : GeminiRepository {
    override suspend fun generateText(prompt: String, modelName: String): Result<String> {
        return if (shouldFail) Result.failure(RuntimeException("API Error"))
        else Result.success("Echo: $prompt")
    }
}

class FakeChatMessageDao : ChatMessageDao {
    private val messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>> = messages
    override fun getAllSessionIds(): Flow<List<Long>> = MutableStateFlow(emptyList())
    override suspend fun getFirstMessageForSession(sessionId: Long): ChatMessageEntity? = null

    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        val current = messages.value.toMutableList()
        val newEntity = message.copy(id = (current.size + 1).toLong())
        current.add(newEntity)
        messages.value = current
        return newEntity.id
    }

    override suspend fun clearSession(sessionId: Long) { messages.value = emptyList() }
    override suspend fun clearAll() { messages.value = emptyList() }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _isDarkMode = MutableStateFlow(false)
    private val _selectedModel = MutableStateFlow("gemini-3.6-flash")
    override val isDarkModeFlow: Flow<Boolean> = _isDarkMode
    override val selectedModelFlow: Flow<String> = _selectedModel
    override suspend fun setDarkMode(isDarkMode: Boolean) { _isDarkMode.value = isDarkMode }
    override suspend fun setSelectedModel(model: String) { _selectedModel.value = model }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var fakePreferences: FakeUserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakePreferences = FakeUserPreferencesRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun promptChange_updatesState() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, fakePreferences, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun emptyPrompt_setsError() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, fakePreferences, hasApiKey = true)
        viewModel.onPromptChange("")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }
}
