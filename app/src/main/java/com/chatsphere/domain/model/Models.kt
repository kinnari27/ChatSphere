package com.chatsphere.domain.model

import java.time.Instant

enum class MessageStatus { Pending, Sent, Delivered, Read, Failed }
enum class MessageType { Text, Image, Emoji, Voice }
enum class ConversationType { Direct, Group }
enum class ConnectionState { Disconnected, Connecting, Connected, Reconnecting }

data class User(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Instant? = null,
    val isBlocked: Boolean = false
)

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val body: String,
    val type: MessageType = MessageType.Text,
    val status: MessageStatus = MessageStatus.Pending,
    val createdAt: Instant = Instant.now(),
    val replyToMessageId: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val isPinned: Boolean = false
)

data class Conversation(
    val id: String,
    val title: String,
    val type: ConversationType,
    val avatarUrl: String? = null,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isArchived: Boolean = false,
    val participants: List<User> = emptyList()
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

data class TypingState(
    val conversationId: String,
    val userId: String,
    val displayName: String,
    val isTyping: Boolean
)
