package com.chatsphere.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ──────────────────────── Auth DTOs ────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    @SerialName("display_name") val displayName: String,
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    val user: UserDto
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

// ──────────────────────── User DTOs ────────────────────────

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    val email: String,
    val bio: String?,
    @SerialName("last_seen") val lastSeen: Long?,
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("created_at") val createdAt: Long
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("display_name") val displayName: String,
    val bio: String?
)

@Serializable
data class FcmTokenRequest(
    @SerialName("fcm_token") val fcmToken: String
)

// ──────────────────────── Chat DTOs ────────────────────────

@Serializable
data class ChatDto(
    val id: String,
    val type: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    val participants: List<UserDto>,
    @SerialName("last_message") val lastMessage: MessageDto?,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("is_pinned") val isPinned: Boolean,
    @SerialName("is_archived") val isArchived: Boolean,
    @SerialName("is_muted") val isMuted: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

// ──────────────────────── Message DTOs ────────────────────────

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_avatar_url") val senderAvatarUrl: String?,
    val type: String,                        // "text" | "image" | "emoji" | "voice" | "media"
    val content: String,                     // JSON string of the actual payload
    val status: String,                      // "pending" | "sent" | "delivered" | "read" | "failed"
    @SerialName("reply_to_id") val replyToId: String?,
    @SerialName("reply_preview") val replyPreview: ReplyPreviewDto?,
    val reactions: List<ReactionDto> = emptyList(),
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long?
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id") val chatId: String,
    val type: String,
    val content: String,
    @SerialName("reply_to_id") val replyToId: String? = null
)

@Serializable
data class ReplyPreviewDto(
    @SerialName("message_id") val messageId: String,
    @SerialName("sender_name") val senderName: String,
    @SerialName("preview_text") val previewText: String
)

@Serializable
data class ReactionDto(
    val emoji: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String
)

// ──────────────────────── Group DTOs ────────────────────────

@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String?,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("admin_ids") val adminIds: List<String>,
    val members: List<UserDto>,
    @SerialName("created_by_id") val createdById: String,
    @SerialName("created_at") val createdAt: Long
)

@Serializable
data class CreateGroupRequest(
    val name: String,
    @SerialName("member_ids") val memberIds: List<String>,
    @SerialName("avatar_url") val avatarUrl: String?
)

@Serializable
data class AddMembersRequest(
    @SerialName("member_ids") val memberIds: List<String>
)

// ──────────────────────── Pagination ────────────────────────

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    @SerialName("has_more") val hasMore: Boolean
)