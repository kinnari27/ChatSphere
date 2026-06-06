package com.chatsphere.data.repository

import com.chatsphere.core.network.safeApiCall
import com.chatsphere.core.signalr.SignalRManager
import com.chatsphere.data.local.dao.ChatDao
import com.chatsphere.data.local.dao.MessageDao
import com.chatsphere.data.local.dao.UserDao
import com.chatsphere.data.local.entity.MessageEntity
import com.chatsphere.data.mapper.*
import com.chatsphere.data.remote.api.ChatApi
import com.chatsphere.data.remote.dto.SendMessageRequest
import com.chatsphere.domain.model.*
import com.chatsphere.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first implementation of [ChatRepository].
 *
 * Architecture:
 * - Room is the single source of truth for the UI
 * - API calls populate/update Room, never directly observed by the UI
 * - Outgoing messages are inserted as [MessageStatus.Pending] immediately
 * - SignalR events update Room in real-time, driving reactive UI updates
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val signalRManager: SignalRManager
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Observe incoming real-time messages and persist to Room
        scope.launch {
            signalRManager.incomingMessages.collect { messageDto ->
                val entity = messageDto.toEntity()
                messageDao.insertMessage(entity)
                chatDao.updateLastMessage(messageDto.chatId, messageDto.id, messageDto.createdAt)
            }
        }

        // Update message delivery statuses from SignalR
        scope.launch {
            signalRManager.messageStatusUpdates.collect { update ->
                update.messageId?.let { id ->
                    messageDao.updateMessageStatus(id, update.status)
                }
                update.chatId?.let { chatId ->
                    messageDao.markChatAsDelivered(chatId)
                }
            }
        }
    }

    override fun observeChats(): Flow<List<Chat>> =
        chatDao.observeChats()
            .map { chatEntities ->
                chatEntities.map { chatEntity ->
                    val participantIds = chatDao.getParticipantIds(chatEntity.id)
                    val participants = userDao.getUsersByIds(participantIds).map { it.toDomain() }
                    val lastMessage = chatEntity.lastMessageId?.let { id ->
                        messageDao.getMessageById(id)?.toDomain()
                    }
                    Chat(
                        id = chatEntity.id,
                        type = if (chatEntity.type == "group") ChatType.Group else ChatType.OneToOne,
                        name = chatEntity.name,
                        avatarUrl = chatEntity.avatarUrl,
                        participants = participants,
                        lastMessage = lastMessage,
                        unreadCount = chatEntity.unreadCount,
                        isPinned = chatEntity.isPinned,
                        isArchived = chatEntity.isArchived,
                        isMuted = chatEntity.isMuted,
                        createdAt = Instant.ofEpochMilli(chatEntity.createdAt),
                        updatedAt = Instant.ofEpochMilli(chatEntity.updatedAt)
                    )
                }
            }
            .onStart {
                // Trigger background refresh on first collection
                scope.launch { refreshChats() }
            }

    override fun observeChat(chatId: String): Flow<Chat?> =
        chatDao.observeChat(chatId).map { chatEntity ->
            chatEntity ?: return@map null
            val participantIds = chatDao.getParticipantIds(chatEntity.id)
            val participants = userDao.getUsersByIds(participantIds).map { it.toDomain() }
            Chat(
                id = chatEntity.id,
                type = if (chatEntity.type == "group") ChatType.Group else ChatType.OneToOne,
                name = chatEntity.name,
                avatarUrl = chatEntity.avatarUrl,
                participants = participants,
                lastMessage = null,
                unreadCount = chatEntity.unreadCount,
                isPinned = chatEntity.isPinned,
                isArchived = chatEntity.isArchived,
                isMuted = chatEntity.isMuted,
                createdAt = Instant.ofEpochMilli(chatEntity.createdAt),
                updatedAt = Instant.ofEpochMilli(chatEntity.updatedAt)
            )
        }

    override fun observeMessages(chatId: String, pageSize: Int): Flow<List<Message>> =
        messageDao.observeMessages(chatId, limit = pageSize)
            .map { entities -> entities.map { it.toDomain() } }
            .onStart {
                scope.launch { refreshMessages(chatId, pageSize) }
            }

    override suspend fun sendMessage(chatId: String, content: MessageContent): Result<Message> {
        // 1. Create local pending message immediately (optimistic update)
        val localId = "local_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val (type, contentStr) = serializeContent(content)

        val pendingEntity = MessageEntity(
            id = localId,
            chatId = chatId,
            senderId = "current_user",     // Replaced after sync
            senderName = "You",
            senderAvatarUrl = null,
            type = type,
            content = contentStr,
            status = "pending",
            replyToId = null,
            replyPreviewJson = null,
            reactionsJson = "[]",
            createdAt = now,
            updatedAt = null
        )
        messageDao.insertMessage(pendingEntity)
        chatDao.updateLastMessage(chatId, localId, now)

        // 2. Attempt server sync
        return safeApiCall {
            chatApi.sendMessage(SendMessageRequest(chatId, type, contentStr))
        }.onSuccess { dto ->
            // Replace temp message with real server response
            messageDao.deleteMessage(localId)
            messageDao.insertMessage(dto.toEntity())
            chatDao.updateLastMessage(chatId, dto.id, dto.createdAt)
        }.onFailure {
            // Mark as failed for retry
            messageDao.updateMessageStatus(localId, "failed")
            Timber.e(it, "Failed to send message to $chatId")
        }.map { dto -> dto.toDomain() }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> =
        safeApiCall { chatApi.deleteMessage(messageId) }
            .onSuccess { messageDao.deleteMessage(messageId) }

    override suspend fun editMessage(messageId: String, newText: String): Result<Message> =
        safeApiCall { chatApi.editMessage(messageId, mapOf("content" to newText)) }
            .onSuccess { dto -> messageDao.insertMessage(dto.toEntity()) }
            .map { it.toDomain() }

    override suspend fun markChatAsRead(chatId: String): Result<Unit> =
        safeApiCall { chatApi.markChatAsRead(chatId) }
            .onSuccess {
                chatDao.clearUnreadCount(chatId)
                signalRManager.markRead(chatId)
            }

    override suspend fun pinMessage(messageId: String): Result<Unit> =
        safeApiCall { chatApi.pinMessage(messageId) }
            .onSuccess { messageDao.setPinned(messageId, true) }

    override suspend fun archiveChat(chatId: String, archive: Boolean): Result<Unit> =
        safeApiCall { chatApi.archiveChat(chatId, mapOf("archived" to archive)) }
            .onSuccess { chatDao.setArchived(chatId, archive) }
            .map { }

    override suspend fun searchMessages(query: String): Result<List<Message>> =
        safeApiCall { chatApi.searchMessages(query) }
            .map { dtos -> dtos.map { it.toDomain() } }

    override suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit> =
        safeApiCall { chatApi.reactToMessage(messageId, mapOf("emoji" to emoji)) }

    override suspend fun syncPendingMessages(): Result<Unit> {
        val pending = messageDao.getPendingMessages() + messageDao.getFailedMessages()
        pending.forEach { entity ->
            safeApiCall {
                chatApi.sendMessage(SendMessageRequest(entity.chatId, entity.type, entity.content))
            }.onSuccess { dto ->
                messageDao.deleteMessage(entity.id)
                messageDao.insertMessage(dto.toEntity())
            }.onFailure {
                messageDao.updateMessageStatus(entity.id, "failed")
            }
        }
        return Result.success(Unit)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private suspend fun refreshChats() {
        safeApiCall { chatApi.getChats() }
            .onSuccess { dtos ->
                dtos.forEach { dto ->
                    chatDao.insertChat(dto.toEntity())
                    userDao.insertUsers(dto.participants.map { it.toEntity() })
                    chatDao.insertParticipants(
                        dto.participants.map {
                            com.chatsphere.data.local.entity.ChatParticipantEntity(dto.id, it.id)
                        }
                    )
                    dto.lastMessage?.let { msg ->
                        messageDao.insertMessage(msg.toEntity())
                    }
                }
            }
            .onFailure { Timber.e(it, "Failed to refresh chats") }
    }

    private suspend fun refreshMessages(chatId: String, pageSize: Int) {
        safeApiCall { chatApi.getMessages(chatId, page = 0, pageSize = pageSize) }
            .onSuccess { response ->
                messageDao.insertMessages(response.data.map { it.toEntity() })
            }
            .onFailure { Timber.e(it, "Failed to refresh messages for $chatId") }
    }

    private fun serializeContent(content: MessageContent): Pair<String, String> =
        when (content) {
            is MessageContent.Text -> Pair("text", content.text)
            is MessageContent.Emoji -> Pair("emoji", content.unicode)
            is MessageContent.Image -> Pair("image", "${content.url}|${content.thumbnailUrl}|${content.width}|${content.height}")
            is MessageContent.VoiceNote -> Pair("voice", "${content.url}|${content.durationSeconds}")
            is MessageContent.Media -> Pair("media", content.url)
        }
}