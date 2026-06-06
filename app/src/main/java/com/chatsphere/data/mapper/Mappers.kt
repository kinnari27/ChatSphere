package com.chatsphere.data.mapper

import com.chatsphere.data.local.entity.*
import com.chatsphere.data.remote.dto.*
import com.chatsphere.domain.model.*
import kotlinx.serialization.json.Json
import java.time.Instant

// ──────────────────────── User Mappers ────────────────────────

fun UserDto.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio,
    presence = if (isOnline) UserPresence.Online else UserPresence.Offline,
    lastSeen = lastSeen?.let { Instant.ofEpochMilli(it) },
    createdAt = Instant.ofEpochMilli(createdAt)
)

fun UserDto.toEntity() = UserEntity(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio,
    presence = if (isOnline) "online" else "offline",
    lastSeen = lastSeen,
    createdAt = createdAt
)

fun UserEntity.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    email = email,
    bio = bio,
    presence = when (presence) {
        "online" -> UserPresence.Online
        "away" -> UserPresence.Away
        else -> UserPresence.Offline
    },
    lastSeen = lastSeen?.let { Instant.ofEpochMilli(it) },
    isBlocked = isBlocked,
    createdAt = Instant.ofEpochMilli(createdAt)
)

// ──────────────────────── Message Mappers ────────────────────────

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

fun MessageDto.toDomain() = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    content = parseMessageContent(type, content),
    status = status.toMessageStatus(),
    replyTo = replyPreview?.let { ReplyPreview(it.messageId, it.senderName, it.previewText) },
    reactions = reactions.map { MessageReaction(it.emoji, it.userId, it.userName) },
    isPinned = isPinned,
    isEdited = isEdited,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let { Instant.ofEpochMilli(it) }
)

fun MessageDto.toEntity() = MessageEntity(
    id = id,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    type = type,
    content = content,
    status = status,
    replyToId = replyToId,
    replyPreviewJson = replyPreview?.let { json.encodeToString(ReplyPreviewDto.serializer(), it) },
    reactionsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ReactionDto.serializer()), reactions),
    isPinned = isPinned,
    isEdited = isEdited,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MessageEntity.toDomain() = Message(
    id = id,
    chatId = chatId,
    senderId = senderId,
    senderName = senderName,
    senderAvatarUrl = senderAvatarUrl,
    content = parseMessageContent(type, content),
    status = status.toMessageStatus(),
    replyTo = replyPreviewJson?.let {
        try {
            val dto = json.decodeFromString(ReplyPreviewDto.serializer(), it)
            ReplyPreview(dto.messageId, dto.senderName, dto.previewText)
        } catch (e: Exception) { null }
    },
    reactions = try {
        json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ReactionDto.serializer()),
            reactionsJson
        ).map { MessageReaction(it.emoji, it.userId, it.userName) }
    } catch (e: Exception) { emptyList() },
    isPinned = isPinned,
    isEdited = isEdited,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = updatedAt?.let { Instant.ofEpochMilli(it) }
)

private fun parseMessageContent(type: String, content: String): MessageContent =
    when (type) {
        "text" -> MessageContent.Text(content)
        "emoji" -> MessageContent.Emoji(content)
        "image" -> try {
            val parts = content.split("|")
            MessageContent.Image(parts[0], parts.getOrNull(1), parts.getOrNull(2)?.toIntOrNull() ?: 0, parts.getOrNull(3)?.toIntOrNull() ?: 0)
        } catch (e: Exception) { MessageContent.Text(content) }
        "voice" -> try {
            val parts = content.split("|")
            MessageContent.VoiceNote(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: 0, emptyList())
        } catch (e: Exception) { MessageContent.Text(content) }
        else -> MessageContent.Text(content)
    }

private fun String.toMessageStatus() = when (this) {
    "pending" -> MessageStatus.Pending
    "sent" -> MessageStatus.Sent
    "delivered" -> MessageStatus.Delivered
    "read" -> MessageStatus.Read
    else -> MessageStatus.Failed
}

// ──────────────────────── Chat Mappers ────────────────────────

fun ChatDto.toEntity() = ChatEntity(
    id = id,
    type = type,
    name = name,
    avatarUrl = avatarUrl,
    unreadCount = unreadCount,
    isPinned = isPinned,
    isArchived = isArchived,
    isMuted = isMuted,
    lastMessageId = lastMessage?.id,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ──────────────────────── Group Mappers ────────────────────────

fun GroupDto.toEntity() = GroupEntity(
    id = id,
    name = name,
    description = description,
    avatarUrl = avatarUrl,
    adminIdsJson = json.encodeToString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()), adminIds),
    createdById = createdById,
    createdAt = createdAt
)

fun GroupEntity.toDomain(members: List<User>) = Group(
    id = id,
    name = name,
    description = description,
    avatarUrl = avatarUrl,
    adminIds = try {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()), adminIdsJson)
    } catch (e: Exception) { emptyList() },
    members = members,
    createdById = createdById,
    createdAt = Instant.ofEpochMilli(createdAt)
)