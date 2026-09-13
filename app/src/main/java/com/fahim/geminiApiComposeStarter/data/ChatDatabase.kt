package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.withTransaction

@Database(
    entities = [ChatEntity::class, MessageEntity::class, ChatMetadataEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: ChatDatabase? = null

        fun getInstance(context: Context): ChatDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "chat_history.db",
            ).build().also { instance = it }
        }
    }
}

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val position: Int,
)

@Entity(
    tableName = "messages",
    indices = [Index("chatId")],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: Long,
    val chatId: String,
    val text: String,
    val role: ConversationRole,
)

@Entity(tableName = "chat_metadata")
data class ChatMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class ChatWithMessages(
    @Embedded val chat: ChatEntity,
    @Relation(parentColumn = "id", entityColumn = "chatId")
    val messages: List<MessageEntity>,
)

@Dao
interface ChatDao {
    @Transaction
    @Query("SELECT * FROM chats ORDER BY position ASC")
    suspend fun getChatsWithMessages(): List<ChatWithMessages>

    @Query("SELECT value FROM chat_metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): String?

    @Query("DELETE FROM messages")
    suspend fun deleteMessages()

    @Query("DELETE FROM chats")
    suspend fun deleteChats()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: ChatMetadataEntity)
}

class RoomChatStorage(
    private val database: ChatDatabase,
) : ChatStorage {
    private val dao = database.chatDao()

    override suspend fun loadChats(): List<StoredChat> = dao.getChatsWithMessages().map { chatWithMessages ->
        StoredChat(
            id = chatWithMessages.chat.id,
            title = chatWithMessages.chat.title,
            messages = chatWithMessages.messages
                .sortedBy { message -> message.id }
                .map { message ->
                    StoredMessage(
                        id = message.id,
                        text = message.text,
                        role = message.role,
                    )
                },
        )
    }

    override suspend fun saveChats(chats: List<StoredChat>, activeChatId: String) {
        database.withTransaction {
            dao.deleteMessages()
            dao.deleteChats()
            dao.insertChats(
                chats.mapIndexed { index, chat ->
                    ChatEntity(id = chat.id, title = chat.title, position = index)
                },
            )
            dao.insertMessages(
                chats.flatMap { chat ->
                    chat.messages.map { message ->
                        MessageEntity(
                            id = message.id,
                            chatId = chat.id,
                            text = message.text,
                            role = message.role,
                        )
                    }
                },
            )
            dao.insertMetadata(ChatMetadataEntity(ACTIVE_CHAT_ID_KEY, activeChatId))
        }
    }

    override suspend fun loadActiveChatId(): String? = dao.getMetadata(ACTIVE_CHAT_ID_KEY)

    private companion object {
        const val ACTIVE_CHAT_ID_KEY = "active_chat_id"
    }
}
