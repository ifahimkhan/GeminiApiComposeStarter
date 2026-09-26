package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class ConversationEntity(@PrimaryKey val id: String, val title: String, val category: String, val preview: String, val updatedAt: Long)

@Entity(tableName = "messages", foreignKeys = [ForeignKey(entity = ConversationEntity::class, parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["conversationId", "sequence"], unique = true)])
data class MessageEntity(@PrimaryKey val id: String, val conversationId: String, val turnId: String, val sequence: Long, val role: String, val text: String, val delivery: String, val feedback: Int = 0)

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations WHERE :query = '' OR instr(lower(title), lower(:query)) > 0 OR id IN (SELECT conversationId FROM messages WHERE instr(lower(text), lower(:query)) > 0) ORDER BY updatedAt DESC, id")
    fun conversations(query: String): Flow<List<ConversationEntity>>
    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY sequence ASC")
    fun messages(id: String): Flow<List<MessageEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun create(conversation: ConversationEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(message: MessageEntity)
    @Query("UPDATE messages SET delivery = :delivery WHERE id = :id") suspend fun delivery(id: String, delivery: String)
    @Query("UPDATE messages SET feedback = :value WHERE id = :id") suspend fun feedback(id: String, value: Int)
    @Query("UPDATE conversations SET preview = :preview, updatedAt = :time WHERE id = :id") suspend fun touch(id: String, preview: String, time: Long)
    @Query("UPDATE messages SET delivery = 'INTERRUPTED' WHERE delivery = 'PENDING'") suspend fun recover()
}

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1, exportSchema = true)
abstract class ChatDatabase : RoomDatabase() { abstract fun chats(): ChatDao }
