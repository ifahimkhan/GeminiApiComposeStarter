package com.fahim.geminiApiComposeStarter.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey
    @NonNull
    private final String id;
    @NonNull
    private final String text;
    @NonNull
    private final String participant;
    private final long timestamp;

    public ChatMessageEntity(@NonNull String id, @NonNull String text, @NonNull String participant, long timestamp) {
        this.id = id;
        this.text = text;
        this.participant = participant;
        this.timestamp = timestamp;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getText() {
        return text;
    }

    @NonNull
    public String getParticipant() {
        return participant;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
