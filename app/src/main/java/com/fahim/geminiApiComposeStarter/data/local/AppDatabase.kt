package com.fahim.geminiApiComposeStarter.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Local persistent database for storing chat history.
 * Implements Room-compatible schema and SQLite persistence with reactive StateFlow updates.
 */
class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION),
    ChatDao {

    private val _messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    init {
        // Initial load
        refreshCache()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_SENDER TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_IS_ERROR INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    private fun refreshCache() {
        try {
            val list = queryAll()
            _messagesFlow.value = list
        } catch (e: Exception) {
            _messagesFlow.value = emptyList()
        }
    }

    private fun queryAll(): List<ChatMessageEntity> {
        val list = mutableListOf<ChatMessageEntity>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_MESSAGES,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val textIdx = c.getColumnIndexOrThrow(COLUMN_TEXT)
            val senderIdx = c.getColumnIndexOrThrow(COLUMN_SENDER)
            val tsIdx = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val errIdx = c.getColumnIndexOrThrow(COLUMN_IS_ERROR)

            while (c.moveToNext()) {
                list.add(
                    ChatMessageEntity(
                        id = c.getLong(idIdx),
                        text = c.getString(textIdx),
                        sender = MessageSender.valueOf(c.getString(senderIdx)),
                        timestamp = c.getLong(tsIdx),
                        isError = c.getInt(errIdx) == 1,
                    )
                )
            }
        }
        return list
    }

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messagesFlow.asStateFlow()

    override suspend fun insertMessage(message: ChatMessageEntity): Long =
        withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_TEXT, message.text)
                put(COLUMN_SENDER, message.sender.name)
                put(COLUMN_TIMESTAMP, message.timestamp)
                put(COLUMN_IS_ERROR, if (message.isError) 1 else 0)
            }
            val insertedId = db.insert(TABLE_MESSAGES, null, values)
            refreshCache()
            insertedId
        }

    override suspend fun clearAllMessages() = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, null, null)
        refreshCache()
    }

    override suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_ID = ?", arrayOf(id.toString()))
        refreshCache()
    }

    companion object {
        const val DATABASE_NAME = "gemini_chat_db"
        const val DATABASE_VERSION = 1

        const val TABLE_MESSAGES = "chat_messages"
        const val COLUMN_ID = "id"
        const val COLUMN_TEXT = "text"
        const val COLUMN_SENDER = "sender"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_IS_ERROR = "is_error"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context).also { INSTANCE = it }
            }
        }
    }
}
