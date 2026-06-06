package com.chatsphere.data.mapper

import com.chatsphere.core.database.ConversationEntity
import com.chatsphere.core.database.MessageEntity
import com.chatsphere.core.database.UserEntity
import com.chatsphere.data.remote.AuthResponse
import com.chatsphere.data.remote.ConversationDto
import com.chatsphere.data.remote.MessageDto
import com.chatsphere.data.remote.UserDto
import com.chatsphere.domain.model.AuthSession
import com.chatsphere.domain.model.Conversation
import com.chatsphere.domain.model.ConversationType
import com.chatsphere.domain.model.Message
import com.chatsphere.domain.model.MessageStatus
import com.chatsphere.domain.model.MessageType
import com.chatsphere.domain.model.User
import java.time.Instant

fun UserDto.toDomain() = User(id = id, displayName = displayName, avatarUrl = avatarUrl)
fun AuthResponse.toDomain() = AuthSession(accessToken = accessToken, refreshToken = refreshToken, user = user.toDomain())

fun UserEntity.toDomain() = User(
    id = id,
    displayName = displayName,
    avatarUrl = avatarUrl,
    isOnline = isOnline,
    lastSeen = lastSeenEpochMillis?.let(Instant::ofEpochMilli),
    isBlocked = isBlocked
)

fun ConversationDto.toEntity() = ConversationEntity(
    id = id,
    title = title,
    type = ConversationType.valueOf(type),
    avatarUrl = avatarUrl,
    unreadCount = unreadCount,
    isArchived = false
)

fun ConversationEntity.toDomain(lastMessage: Message? = null) = Conversation(
    id = id,
    title = title,
    type = type,
    avatarUrl = avatarUrl,
    unreadCount = unreadCount,
    isArchived = isArchived,
    lastMessage = lastMessage
)

fun MessageDto.toEntity() = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    type = MessageType.valueOf(type),
    status = MessageStatus.valueOf(status),
    createdAtEpochMillis = createdAtEpochMillis,
    replyToMessageId = replyToMessageId,
    reactionSummary = "",
    isPinned = false
)

fun MessageEntity.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    body = body,
    type = type,
    status = status,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    replyToMessageId = replyToMessageId,
    reactions = reactionSummary.takeIf { it.isNotBlank() }?.split(",")?.associate {
        val parts = it.split(":", limit = 2)
        parts.first() to parts.getOrElse(1) { "" }
    }.orEmpty(),
    isPinned = isPinned
)
