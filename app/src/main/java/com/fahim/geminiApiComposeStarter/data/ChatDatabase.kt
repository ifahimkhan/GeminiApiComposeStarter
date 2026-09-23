package com.fahim.geminiApiComposeStarter.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM messages ORDER BY id ASC")
    fun observeMessages(): Flow<List<ChatMessage>>

    @Insert
    suspend fun insert(message: ChatMessage)

    @Query("DELETE FROM messages")
    suspend fun clear()
}

@Database(entities = [ChatMessage::class], version = 1, exportSchema = true)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}

interface ConversationRepository {
    val messages: Flow<List<ChatMessage>>
    suspend fun add(message: ChatMessage)
    suspend fun clear()
}

class RoomConversationRepository(private val dao: ChatDao) : ConversationRepository {
    override val messages = dao.observeMessages()
    override suspend fun add(message: ChatMessage) = dao.insert(message)
    override suspend fun clear() = dao.clear()
}
