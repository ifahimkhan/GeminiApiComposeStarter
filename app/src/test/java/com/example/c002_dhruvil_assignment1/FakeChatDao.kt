package com.example.c002_dhruvil_assignment1

import com.example.c002_dhruvil_assignment1.data.room.ChatDao
import com.example.c002_dhruvil_assignment1.data.room.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatDao : ChatDao {
    private val messagesList = mutableListOf<MessageEntity>()
    private val _messagesFlow = MutableStateFlow<List<MessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<MessageEntity>> = _messagesFlow

    override suspend fun insertMessage(message: MessageEntity) {
        messagesList.add(message)
        _messagesFlow.value = messagesList.toList()
    }

    override suspend fun clearAll() {
        messagesList.clear()
        _messagesFlow.value = emptyList()
    }
}
