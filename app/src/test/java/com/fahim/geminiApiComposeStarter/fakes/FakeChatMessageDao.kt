package com.fahim.geminiApiComposeStarter.fakes

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory stand-in for the Room DAO so ViewModel tests don't need an Android/SQLite runtime. */
class FakeChatMessageDao : ChatMessageDao {
    private val state = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(message: ChatMessageEntity): Long {
        val withId = message.copy(id = nextId++)
        state.value = state.value + withId
        return withId.id
    }

    override fun observeAll(): StateFlow<List<ChatMessageEntity>> = state

    override suspend fun clearAll() {
        state.value = emptyList()
    }
}
