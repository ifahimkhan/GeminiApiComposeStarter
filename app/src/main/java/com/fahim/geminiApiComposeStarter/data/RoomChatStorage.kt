package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatRole
import com.fahim.geminiApiComposeStarter.ui.chat.ImageState
import com.fahim.geminiApiComposeStarter.ui.chat.MessageKind
import com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Room-backed implementation of [ChatStorage].
 *
 * The public API mirrors [FileChatStorage] exactly — the ViewModel and tests
 * do not need to know which backing store is in use.
 */
class RoomChatStorage(private val db: AppDatabase) : ChatStorage {

    override suspend fun load(): List<Conversation> = withContext(Dispatchers.IO) {
        db.chatDao().loadAll()
            .sortedBy { it.conversation.position }
            .map { it.toDomain() }
    }

    override suspend fun save(conversations: List<Conversation>) = withContext(Dispatchers.IO) {
        val dao = db.chatDao()

        // Remove conversations that no longer exist
        dao.deleteConversationsNotIn(conversations.map { it.id })

        conversations.forEachIndexed { convIndex, conv ->
            dao.upsertConversation(
                ConversationEntity(id = conv.id, title = conv.title, position = convIndex)
            )

            val messageEntities = conv.messages.mapIndexed { msgIndex, msg ->
                msg.toEntity(conversationId = conv.id, position = msgIndex)
            }
            dao.upsertMessages(messageEntities)

            // Cascade-delete stale messages
            val keepIds = conv.messages.map { it.id }
            if (keepIds.isNotEmpty()) {
                dao.deleteStaleMessages(conv.id, keepIds)
            }

            // Upsert attachments
            conv.messages.forEach { msg ->
                val attachmentEntities = msg.attachments.map { it.toEntity(messageId = msg.id) }
                dao.upsertAttachments(attachmentEntities)

                val keepUris = msg.attachments.map { it.uri }
                if (keepUris.isNotEmpty()) {
                    dao.deleteStaleAttachments(msg.id, keepUris)
                }
            }
        }
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun ConversationWithMessages.toDomain(): Conversation {
        val msgs = messages
            .sortedBy { it.message.position }
            .map { it.toDomain() }
        return Conversation(
            id = conversation.id,
            title = conversation.title,
            messages = msgs,
            draft = "",
        )
    }

    private fun MessageWithAttachments.toDomain(): ChatMessage {
        return ChatMessage(
            id = message.id,
            role = ChatRole.valueOf(message.role),
            text = message.text,
            kind = runCatching { MessageKind.valueOf(message.kind) }.getOrDefault(MessageKind.TEXT),
            imagePath = message.imagePath?.takeIf { it.isNotBlank() },
            imageState = message.imageState?.takeIf { it.isNotBlank() }?.let {
                runCatching { ImageState.valueOf(it) }.getOrNull()
            },
            attachments = attachments.map { it.toDomain() },
            timestamp = message.timestamp,
        )
    }

    private fun AttachmentEntity.toDomain() = PendingAttachment(
        uri = uri, name = name, mimeType = mimeType,
    )

    private fun ChatMessage.toEntity(conversationId: String, position: Int) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        text = text,
        kind = kind.name,
        imagePath = imagePath,
        imageState = imageState?.name,
        timestamp = timestamp,
        position = position,
    )

    private fun PendingAttachment.toEntity(messageId: Long) = AttachmentEntity(
        messageId = messageId,
        uri = uri,
        name = name,
        mimeType = mimeType,
    )
}
