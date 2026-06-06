package com.chatsphere.data.remote

import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class RegisterRequest(val name: String, val email: String, val password: String)
@Serializable data class AuthResponse(val accessToken: String, val refreshToken: String, val user: UserDto)
@Serializable data class UserDto(val id: String, val displayName: String, val avatarUrl: String? = null)
@Serializable data class SendMessageRequest(val conversationId: String, val body: String, val replyToMessageId: String?)
@Serializable data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val type: String = "Text",
    val status: String = "Sent",
    val createdAtEpochMillis: Long,
    val replyToMessageId: String? = null
)
@Serializable data class ConversationDto(
    val id: String,
    val title: String,
    val type: String,
    val avatarUrl: String? = null,
    val unreadCount: Int = 0
)
@Serializable data class CreateGroupRequest(val name: String, val memberIds: List<String>)
