package com.devraj.geminiassistant.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.devraj.geminiassistant.data.local.dao.ChatMessageDao
import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    private val _messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val dao = ChatMessageDaoImpl()

    init {
        refreshMessagesFlow()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text TEXT NOT NULL,
                isUser INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                isError INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS chat_messages")
        onCreate(db)
    }

    fun chatMessageDao(): ChatMessageDao = dao

    private fun refreshMessagesFlow() {
        try {
            val list = queryAllSync()
            _messagesFlow.value = list
        } catch (_: Exception) {
        }
    }

    private fun queryAllSync(): List<ChatMessageEntity> {
        val list = mutableListOf<ChatMessageEntity>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            "chat_messages",
            null,
            null,
            null,
            null,
            null,
            "timestamp ASC, id ASC"
        )
        cursor.use {
            val idIdx = it.getColumnIndex("id")
            val textIdx = it.getColumnIndex("text")
            val isUserIdx = it.getColumnIndex("isUser")
            val timestampIdx = it.getColumnIndex("timestamp")
            val isErrorIdx = it.getColumnIndex("isError")

            while (it.moveToNext()) {
                val entity = ChatMessageEntity(
                    id = if (idIdx >= 0) it.getLong(idIdx) else 0L,
                    text = if (textIdx >= 0) it.getString(textIdx) else "",
                    isUser = if (isUserIdx >= 0) it.getInt(isUserIdx) == 1 else false,
                    timestamp = if (timestampIdx >= 0) it.getLong(timestampIdx) else 0L,
                    isError = if (isErrorIdx >= 0) it.getInt(isErrorIdx) == 1 else false
                )
                list.add(entity)
            }
        }
        return list
    }

    private inner class ChatMessageDaoImpl : ChatMessageDao {
        override fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>> = _messagesFlow.asStateFlow()

        override suspend fun getAllMessages(): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
            queryAllSync()
        }

        override suspend fun insertMessage(message: ChatMessageEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("text", message.text)
                put("isUser", if (message.isUser) 1 else 0)
                put("timestamp", message.timestamp)
                put("isError", if (message.isError) 1 else 0)
            }
            val insertedId = db.insertWithOnConflict("chat_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            refreshMessagesFlow()
            insertedId
        }

        override suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("chat_messages", null, null)
            refreshMessagesFlow()
        }
    }

    companion object {
        private const val DATABASE_NAME = "devraj_gemini_db.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
