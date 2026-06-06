package com.chatsphere.domain.model

import java.time.Instant

// ─────────────────────────── User ───────────────────────────

/**
 * Core domain model representing a ChatSphere user.
 * Immutable value object with no framework dependencies.
 */
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val email: String,
    val bio: String?,
    val presence: UserPresence = UserPresence.Offline,
    val lastSeen: Instant? = null,
    val isBlocked: Boolean = false,
    val createdAt: Instant
)

/** Presence state for real-time online indicators. */
enum class UserPresence { Online, Away, Offline }

// ─────────────────────────── Message ───────────────────────────

/**
 * Core domain model for a chat message.
 * Supports text, image, emoji, and voice note types.
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val content: MessageContent,
    val status: MessageStatus,
    val replyTo: ReplyPreview? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val isPinned: Boolean = false,
    val isEdited: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant? = null
)

/** Sealed hierarchy for type-safe message content. */
sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Image(val url: String, val thumbnailUrl: String?, val width: Int, val height: Int) : MessageContent()
    data class Emoji(val unicode: String) : MessageContent()
    data class VoiceNote(val url: String, val durationSeconds: Int, val waveform: List<Float>) : MessageContent()
    data class Media(val url: String, val mimeType: String, val fileName: String, val sizeBytes: Long) : MessageContent()
}

/** Delivery / read receipt status for offline-first sync. */
enum class MessageStatus { Pending, Sent, Delivered, Read, Failed }

/** Lightweight preview for quoted replies. */
data class ReplyPreview(
    val messageId: String,
    val senderName: String,
    val previewText: String
)

/** Emoji reaction on a message. */
data class MessageReaction(
    val emoji: String,
    val userId: String,
    val userName: String
)

// ─────────────────────────── Chat ───────────────────────────

/**
 * Represents a conversation — either one-to-one or group.
 */
data class Chat(
    val id: String,
    val type: ChatType,
    val name: String,
    val avatarUrl: String?,
    val participants: List<User>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
    val isMuted: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class ChatType { OneToOne, Group }

// ─────────────────────────── Group ───────────────────────────

/**
 * Extended information about a group conversation,
 * including admin roles and metadata.
 */
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val avatarUrl: String?,
    val adminIds: List<String>,
    val members: List<User>,
    val createdById: String,
    val createdAt: Instant
)

// ─────────────────────────── Auth ───────────────────────────

/** JWT authentication session. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val user: User
)

/** Credentials for login. */
data class LoginCredentials(
    val email: String,
    val password: String
)

/** Data required for account registration. */
data class RegisterData(
    val username: String,
    val displayName: String,
    val email: String,
    val password: String
)

// ─────────────────────────── Notifications ───────────────────────────

/** Push notification payload with deep-link routing info. */
data class ChatNotification(
    val type: NotificationType,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val avatarUrl: String?
)

enum class NotificationType { Chat, GroupMessage, MentionInGroup }