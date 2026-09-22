package com.vaishnavi.n085.geminichat.ui.chat

import com.vaishnavi.n085.geminichat.data.GeminiRepository
import com.vaishnavi.n085.geminichat.data.local.ChatDao
import com.vaishnavi.n085.geminichat.data.local.Message
import com.vaishnavi.n085.geminichat.data.local.MessageRole
import com.vaishnavi.n085.geminichat.data.prefs.ApiKeyCipher
import com.vaishnavi.n085.geminichat.data.prefs.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGeminiRepository(
        private val result: Result<String> = Result.success("Hello from Gemini"),
    ) : GeminiRepository {
        var lastPrompt: String? = null
        override suspend fun generateText(prompt: String): Result<String> {
            lastPrompt = prompt
            return result
        }
    }

    private class FakeChatDao : ChatDao {
        val messages = mutableListOf<Message>()
        override suspend fun getAll(): List<Message> = messages.toList()
        override suspend fun insert(message: Message) { messages.add(message) }
        override suspend fun clearAll() { messages.clear() }
    }

    private class FakeAppPreferences : AppPreferences {
        override val userName: Flow<String> = flowOf("Vaishnavi")
        override val apiKeyCipher: Flow<ApiKeyCipher?> = flowOf<ApiKeyCipher?>(null)
        override suspend fun setUserName(name: String) = Unit
        override suspend fun saveApiKeyCipher(iv: String, ciphertext: String) = Unit
    }

    @Test
    fun emptyPrompt_setsPromptError() = runTest {
        val vm = ChatViewModel(FakeGeminiRepository(), FakeChatDao(), FakeAppPreferences(), hasApiKey = true)
        vm.onSend()
        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertEquals(0, vm.uiState.value.messages.size)
    }

    @Test
    fun missingApiKey_showsMessage() = runTest {
        val vm = ChatViewModel(FakeGeminiRepository(), FakeChatDao(), FakeAppPreferences(), hasApiKey = false)
        vm.onPromptChange("hello")
        vm.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertEquals(0, vm.uiState.value.messages.size)
    }

    @Test
    fun send_success_appendsBothMessages() = runTest {
        val repo = FakeGeminiRepository(Result.success("Hi!"))
        val dao = FakeChatDao()
        val vm = ChatViewModel(repo, dao, FakeAppPreferences(), hasApiKey = true)
        vm.onPromptChange("Hello")
        vm.onSend()

        val state = vm.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
        assertEquals("Hello", state.messages[0].content)
        assertEquals(MessageRole.ASSISTANT, state.messages[1].role)
        assertEquals("Hi!", state.messages[1].content)
        assertEquals("Hello", repo.lastPrompt)
        assertEquals(false, state.isLoading)
        assertEquals("", state.prompt)
        assertEquals(2, dao.messages.size)
    }

    @Test
    fun send_failure_setsErrorMessage() = runTest {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("network down")))
        val vm = ChatViewModel(repo, FakeChatDao(), FakeAppPreferences(), hasApiKey = true)
        vm.onPromptChange("hello")
        vm.onSend()

        val state = vm.uiState.value
        assertEquals("network down", state.errorMessage)
        assertEquals(false, state.isLoading)
        assertEquals(1, state.messages.size)
        assertEquals(MessageRole.USER, state.messages[0].role)
    }

    @Test
    fun clearConversation_removesMessages() = runTest {
        val repo = FakeGeminiRepository(Result.success("Hi!"))
        val dao = FakeChatDao()
        val vm = ChatViewModel(repo, dao, FakeAppPreferences(), hasApiKey = true)
        vm.onPromptChange("Hello")
        vm.onSend()
        assertEquals(2, vm.uiState.value.messages.size)

        vm.clearConversation()
        assertEquals(0, vm.uiState.value.messages.size)
        assertEquals(0, dao.messages.size)
    }
}
