package com.fahim.geminiApiComposeStarter.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessageEntity> getAllMessages();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<ChatMessageEntity> messages);

    @Query("DELETE FROM chat_messages")
    void clearAllMessages();
}
