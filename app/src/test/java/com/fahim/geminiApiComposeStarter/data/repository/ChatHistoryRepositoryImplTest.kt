package com.fahim.geminiApiComposeStarter.data.repository

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryRepositoryImplTest {

    private class FakeChatMessageDao : ChatMessageDao {
        val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        val savedMessages = mutableListOf<ChatMessageEntity>()
        var throwOnInsert = false
        var throwOnClear = false

        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

        override fun insertMessage(message: ChatMessageEntity): Long {
            if (throwOnInsert) throw RuntimeException("Disk I/O error")
            savedMessages.add(message)
            messagesFlow.value = savedMessages.toList()
            return savedMessages.size.toLong()
        }

        override fun insertMessages(messages: List<ChatMessageEntity>): List<Long> {
            savedMessages.addAll(messages)
            messagesFlow.value = savedMessages.toList()
            return messages.map { savedMessages.size.toLong() }
        }

        override fun clearAllMessages(): Int {
            if (throwOnClear) throw RuntimeException("Disk failure")
            val count = savedMessages.size
            savedMessages.clear()
            messagesFlow.value = emptyList()
            return count
        }

        override fun deleteMessage(messageId: String): Int {
            val removed = savedMessages.removeAll { it.messageId == messageId }
            messagesFlow.value = savedMessages.toList()
            return if (removed) 1 else 0
        }
    }

    @Test
    fun saveMessage_convertsAndPersistsToDao() = runTest {
        val fakeDao = FakeChatMessageDao()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ChatHistoryRepositoryImpl(chatMessageDao = fakeDao, ioDispatcher = dispatcher)

        val message = ChatMessage(id = "msg-1", text = "Hello Room", participant = Participant.USER, timestamp = 12345L)
        val result = repository.saveMessage(message)

        assertTrue(result.isSuccess)
        assertEquals(1, fakeDao.savedMessages.size)
        val saved = fakeDao.savedMessages.first()
        assertEquals("msg-1", saved.messageId)
        assertEquals("Hello Room", saved.text)
        assertEquals("USER", saved.participant)
        assertEquals(12345L, saved.timestamp)
    }

    @Test
    fun getMessages_mapsEntitiesToDomainChatMessages() = runTest {
        val fakeDao = FakeChatMessageDao()
        fakeDao.savedMessages.addAll(
            listOf(
                ChatMessageEntity(id = 1, messageId = "id-1", text = "User prompt", participant = "USER", timestamp = 100L),
                ChatMessageEntity(id = 2, messageId = "id-2", text = "Gemini answer", participant = "MODEL", timestamp = 200L),
            )
        )
        fakeDao.messagesFlow.value = fakeDao.savedMessages.toList()

        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ChatHistoryRepositoryImpl(chatMessageDao = fakeDao, ioDispatcher = dispatcher)

        val messages = repository.getMessages().first()
        assertEquals(2, messages.size)
        assertEquals("id-1", messages[0].id)
        assertEquals("User prompt", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("id-2", messages[1].id)
        assertEquals("Gemini answer", messages[1].text)
        assertEquals(Participant.MODEL, messages[1].participant)
    }

    @Test
    fun clearHistory_clearsDaoAndUpdatesFlow() = runTest {
        val fakeDao = FakeChatMessageDao()
        fakeDao.savedMessages.add(
            ChatMessageEntity(id = 1, messageId = "id-1", text = "Text", participant = "USER", timestamp = 100L)
        )
        fakeDao.messagesFlow.value = fakeDao.savedMessages.toList()

        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ChatHistoryRepositoryImpl(chatMessageDao = fakeDao, ioDispatcher = dispatcher)

        val clearResult = repository.clearHistory()
        assertTrue(clearResult.isSuccess)
        assertTrue(fakeDao.savedMessages.isEmpty())
        assertTrue(repository.getMessages().first().isEmpty())
    }

    @Test
    fun saveMessage_whenDaoFails_returnsFailureWithoutCrashing() = runTest {
        val fakeDao = FakeChatMessageDao().apply { throwOnInsert = true }
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ChatHistoryRepositoryImpl(chatMessageDao = fakeDao, ioDispatcher = dispatcher)

        val message = ChatMessage(id = "msg-err", text = "Crash test", participant = Participant.USER)
        val result = repository.saveMessage(message)

        assertTrue("Should return Result.failure on database error", result.isFailure)
        assertEquals("Disk I/O error", result.exceptionOrNull()?.message)
    }

    @Test
    fun getMessages_whenDaoThrows_emitsEmptyListGracefully() = runTest {
        val errorDao = object : ChatMessageDao {
            override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow {
                throw RuntimeException("Database corrupted")
            }
            override fun insertMessage(message: ChatMessageEntity): Long = 1L
            override fun insertMessages(messages: List<ChatMessageEntity>): List<Long> = listOf(1L)
            override fun clearAllMessages(): Int = 0
            override fun deleteMessage(messageId: String): Int = 0
        }

        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = ChatHistoryRepositoryImpl(chatMessageDao = errorDao, ioDispatcher = dispatcher)

        val messages = repository.getMessages().first()
        assertTrue("Should gracefully fallback to emptyList on database flow error", messages.isEmpty())
    }
}
