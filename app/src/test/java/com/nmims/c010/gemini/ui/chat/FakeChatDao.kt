package com.nmims.c010.gemini.ui.chat

import com.nmims.c010.gemini.data.local.ChatDao
import com.nmims.c010.gemini.data.local.ChatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// in memory dao so the view model tests never touch a real database
class FakeChatDao : ChatDao {

    private val state = MutableStateFlow<List<ChatEntity>>(emptyList())

    override fun observeAll(): Flow<List<ChatEntity>> = state

    override suspend fun insert(message: ChatEntity) {
        state.value = state.value + message
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}
