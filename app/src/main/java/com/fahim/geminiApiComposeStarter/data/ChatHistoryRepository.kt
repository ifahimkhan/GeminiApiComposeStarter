package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val sequence: Long = 0,
    val messageId: String,
    val text: String,
    val isUser: Boolean,
)

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages ORDER BY sequence ASC")
    suspend fun getMessages(): List<ChatMessageEntity>

    // Room inserts the entire list in one transaction.
    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
}

@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "chat_history.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}

class ChatHistoryRepository(context: Context) {

    private val dao = ChatDatabase
        .getInstance(context)
        .chatMessageDao()

    suspend fun loadMessages(): List<ChatMessage> {
        return dao.getMessages().map { entity ->
            ChatMessage(
                id = entity.messageId,
                text = entity.text,
                isUser = entity.isUser
            )
        }
    }

    suspend fun saveExchange(
        userMessage: ChatMessage,
        assistantMessage: ChatMessage,
    ) {
        dao.insertMessages(
            listOf(userMessage, assistantMessage).map { message ->
                ChatMessageEntity(
                    messageId = message.id,
                    text = message.text,
                    isUser = message.isUser
                )
            }
        )
    }
}
