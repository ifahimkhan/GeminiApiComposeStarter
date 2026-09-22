package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sender: String,
    val timestamp: Long,
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)
}

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class ChatHistoryDatabase : RoomDatabase() {
    abstract fun messageDao(): ChatMessageDao

    companion object {
        fun create(context: Context): ChatHistoryDatabase = Room.databaseBuilder(
            context.applicationContext,
            ChatHistoryDatabase::class.java,
            "chat-history.db",
        ).build()
    }
}

interface ChatHistoryRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    suspend fun add(text: String, sender: Sender)
}

class RoomChatHistoryRepository(private val dao: ChatMessageDao) : ChatHistoryRepository {
    override fun observeMessages(): Flow<List<ChatMessage>> = dao.observeAll().map {
        entities -> entities.map { ChatMessage(it.id, it.text, Sender.valueOf(it.sender), it.timestamp) }
    }

    override suspend fun add(text: String, sender: Sender) {
        dao.insert(ChatMessageEntity(text = text, sender = sender.name, timestamp = System.currentTimeMillis()))
    }
}
