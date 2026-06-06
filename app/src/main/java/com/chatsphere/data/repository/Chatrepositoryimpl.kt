package com.chatsphere.data.repository

import com.chatsphere.core.common.AppResult
import com.chatsphere.core.database.ChatDao
import com.chatsphere.core.database.MessageEntity
import com.chatsphere.core.database.PendingMessageEntity
import com.chatsphere.core.signalr.SignalRManager
import com.chatsphere.data.mapper.toDomain
import com.chatsphere.data.mapper.toEntity
import com.chatsphere.data.remote.ChatSphereApi
import com.chatsphere.data.remote.SendMessageRequest
import com.chatsphere.domain.model.ConnectionState
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.MessageType
import com.chatsphere.domain.model.TypingState
import com.chatsphere.domain.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache-first chat repository.
 *
 * UI observes Room flows, outgoing messages are written locally before network delivery, and
 * failed sends are replayed when SignalR reports a connected state.
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatSphereApi,
    private val chatDao: ChatDao,
    private val signalRManager: SignalRManager
) : ChatRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val typing = MutableStateFlow<TypingState?>(null)

    init {
        signalRManager.incomingMessages
            .onEach { chatDao.upsertMessage(it.toEntity()) }
            .launchIn(scope)
        signalRManager.typingEvents
            .onEach { typing.value = it }
            .launchIn(scope)
        signalRManager.connectionState
            .filter { it == ConnectionState.Connected }
            .onEach { syncPendingMessages() }
            .launchIn(scope)
    }

    override fun observeConversations() = chatDao.observeConversations()
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeMessages(conversationId: String) = chatDao.observeMessages(conversationId)
        .map { entities -> entities.map { it.toDomain() } }

    override fun observeTyping(conversationId: String) = typing
        .map { it?.takeIf { state -> state.conversationId == conversationId && state.isTyping } }
        .distinctUntilChanged()

    override fun observeConnectionState(): Flow<ConnectionState> = signalRManager.connectionState

    override suspend fun sendMessage(conversationId: String, body: String, replyToMessageId: String?): AppResult<Message> {
        val local = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "me",
            senderName = "You",
            body = body,
            type = MessageType.Text,
            status = MessageStatus.Pending,
            createdAt = Instant.now(),
            replyToMessageId = replyToMessageId
        )
        chatDao.upsertMessage(local.toEntity())
        return runCatching {
            val remote = api.sendMessage(SendMessageRequest(conversationId, body, replyToMessageId)).toEntity()
            chatDao.upsertMessage(remote)
            signalRManager.sendMessage(conversationId, body, replyToMessageId)
            remote.toDomain()
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = {
                chatDao.enqueuePending(local.toPendingEntity())
                AppResult.Success(local)
            }
        )
    }

    override suspend fun markAsRead(conversationId: String, messageId: String) {
        chatDao.updateMessageStatus(messageId, MessageStatus.Read)
    }

    override suspend fun setTyping(conversationId: String, isTyping: Boolean) {
        if (isTyping) signalRManager.startTyping(conversationId) else signalRManager.stopTyping(conversationId)
    }

    override suspend fun searchMessages(query: String): List<Message> =
        chatDao.searchMessages(query).map { it.toDomain() }

    override suspend fun syncPendingMessages() {
        chatDao.pendingMessages().forEach { pending ->
            runCatching {
                api.sendMessage(SendMessageRequest(pending.conversationId, pending.body, pending.replyToMessageId))
            }.onSuccess { remote ->
                chatDao.upsertMessage(remote.toEntity())
                chatDao.removePending(pending.localId)
            }
        }
    }
}

private fun Message.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    type = type,
    status = status,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    replyToMessageId = replyToMessageId,
    reactionSummary = reactions.entries.joinToString(",") { "${it.key}:${it.value}" },
    isPinned = isPinned
)

private fun Message.toPendingEntity() = PendingMessageEntity(
    localId = id,
    conversationId = conversationId,
    body = body,
    replyToMessageId = replyToMessageId,
    createdAtEpochMillis = createdAt.toEpochMilli()
)
