package com.fahim.geminiApiComposeStarter.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// ── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    /** Insertion order for sidebar sorting (lower = older). */
    val position: Int = 0,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey val id: Long,
    val conversationId: String,
    val role: String,          // ChatRole.name
    val text: String,
    val kind: String,          // MessageKind.name
    val imagePath: String?,
    val imageState: String?,   // ImageState.name or null
    val timestamp: Long,
    /** Order within the conversation. */
    val position: Int,
)

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("messageId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val attachmentId: Long = 0,
    val messageId: Long,
    val uri: String,
    val name: String,
    val mimeType: String,
)

// ── Relation helpers ──────────────────────────────────────────────────────────

data class MessageWithAttachments(
    @Embedded val message: MessageEntity,
    @Relation(parentColumn = "id", entityColumn = "messageId")
    val attachments: List<AttachmentEntity>,
)

data class ConversationWithMessages(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        entity = MessageEntity::class,
        parentColumn = "id",
        entityColumn = "conversationId",
    )
    val messages: List<MessageWithAttachments>,
)
